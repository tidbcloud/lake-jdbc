# TiDB Cloud Lake JDBC

![Apache License 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
[![lake-jdbc](https://img.shields.io/maven-central/v/com.tidbcloud/lake-jdbc?style=flat-square)](https://central.sonatype.com/artifact/com.tidbcloud/lake-jdbc)

## Highlights

- Lake-specific interfaces to stream files into tables or stages with `loadStreamToTable`, `uploadStream`, and `downloadStream`.
- Temporal APIs use session timezone to avoid depending on JVM default zone and support modern `java.time`.

## Prerequisites

The TiDB Cloud Lake JDBC driver requires Java 8 or later.
If the minimum required version of Java is not installed on the client machines where the JDBC driver is installed, you
must install either Oracle Java or OpenJDK.

## Installation

### Maven

Add following code block as a dependency

```xml

<dependency>
    <groupId>com.tidbcloud</groupId>
    <artifactId>lake-jdbc</artifactId>
    <version>0.4.10</version>
</dependency>
```

### Build from source

Note: build from source requires Java 11+, Maven 3.6.3+

```shell
cd lake-jdbc
mvn clean install -DskipTests
```

## Upgrade notes

### 0.4.10

- **`DatabaseMetaData.getURL()` no longer contains credentials.** The base URL returned by `DatabaseMetaData.getURL()`
  and `Connection.getMetaData().getURL()` no longer carries `?password=`, `?accesstoken=` or `?token=`; those
  parameters are still parsed as connection properties, they are just not replayed on every request. Userinfo was
  already parsed into the connection properties instead of the base URL, and 0.4.10 also strips it from URLs that
  carry it inside the parsed authority (defence in depth). If you store that string and later pass it to
  `DriverManager.getConnection(...)` to reconnect, use the original connection configuration instead, or rebuild the
  URL from `getUserName()` and your own credential source.
- **`PreparedStatement#setString` now escapes backslashes.** The server treats `\` as the escape character inside
  single-quoted string literals, so 0.4.10 escapes both the backslash and the quote (`\` → `\\`, `'` → `\'`). Before
  0.4.10 a value such as `C:\temp` was stored as `C:<TAB>emp`, and values containing `\` could produce malformed
  literals, syntax errors, or SQL injection. New versions store the value you pass. If your application pre-escaped
  backslashes to work around the old behaviour, that value will now be stored with an extra `\`; audit existing rows
  with `SELECT count(*) FROM <table> WHERE <column> LIKE '%\\\\%'` (adjust the pattern and quoting to your table,
  column and client) and migrate the rows that were double-escaped.
- **`DatabaseMetaData` name filters now escape backslashes too.** `getTables`, `getColumns`, `getSchemas` and friends
  splice the caller-supplied schema/table/column patterns into SQL and used to double only the single quote, so a
  pattern containing `\` could escape that quote and inject SQL - the same defect fixed in `setString`. They now use
  the same escaping. Note that `\` is also the `LIKE` escape character (`getSearchStringEscape()` returns `\`), so a
  pattern that should match a literal backslash has to escape it for `LIKE` as well.
- **Userinfo passwords are percent-decoded instead of percent-encoded.** Before 0.4.10 the driver URL-encoded the
  userinfo password and sent the encoded text as the basic-auth credential, so a password containing `:`, `@`, `/`,
  `%`, `+` or a space could never authenticate. 0.4.10 decodes `%XX` escapes (UTF-8) and sends the value verbatim:
  write `jdbc:lake://user:P%2Fss@host:8000/default` for the password `P/ss`, and `+` stays a literal plus. A raw `/`,
  `?` or `#` still ends the authority section, so those characters have to be percent-encoded as well.
- **`jdbc:lake://http(s)://user:password@host:port/db` keeps its port and credentials.** The scheme-prefixed form used
  to mis-split the userinfo (`user=https`, password `//user:password`), so authentication failed; the credentials are
  now read from the authority and the explicit port is preserved.
- **Credential query parameters are no longer forwarded to the server.** `password`, `accesstoken`, `access_token`
  and `token` in the connection URL are still parsed as connection properties, but they are no longer appended to
  every HTTP request. If an authentication proxy in front of your deployment relies on one of those parameters being
  present on each request, configure it to accept the driver's `Authorization` header instead.

## Testing

Start the local integration test environment from `tests/Makefile`:

```shell
cd tests
make up
make test
```

The default `make test` command runs `lake-jdbc` tests only.

### Run integration tests with Arrow

To run tests with Arrow result pages, set `LAKE_JDBC_TEST_QUERY_RESULT_FORMAT=arrow`:

```shell
cd tests
make test LAKE_JDBC_TEST_QUERY_RESULT_FORMAT=arrow TEST_MVN_ARGS='-Dgroups=IT -DexcludedGroups=FLAKY'
```

When Arrow mode is enabled through `make test`, the required JVM options are added automatically:

```shell
--add-opens=java.base/java.nio=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true
```

If you run Maven directly instead of `make test`, you must set both the Arrow test environment variable and the JVM options yourself:

```shell
JAVA_TOOL_OPTIONS='--add-opens=java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true' \
LAKE_JDBC_TEST_QUERY_RESULT_FORMAT=arrow \
mvn -pl lake-jdbc test -Dgroups=IT -DexcludedGroups=FLAKY
```

CI note:
- `Standalone Test` runs the regular suite and an extra Arrow IT pass.
- `Cluster Tests` runs the regular suite and an extra Arrow IT pass for each cluster matrix entry.

### Download jar from maven central

You can download the latest version of the lake-jdbc driver [here](https://repo1.maven.org/maven2/com/tidbcloud/lake-jdbc/).

## How to use

Set the connection environment variables before running the examples:

```shell
export LAKE_JDBC_URL='jdbc:lake://host:443/default?warehouse=your-warehouse&ssl=true&sslmode=enable'
export LAKE_JDBC_USER='user'
export LAKE_JDBC_PASSWORD='password'
```

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    private static Connection openConnection() throws SQLException {
        String url = System.getenv("LAKE_JDBC_URL");
        String user = System.getenv("LAKE_JDBC_USER");
        String password = System.getenv("LAKE_JDBC_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) throws SQLException {
        try ( Connection conn = openConnection();
              Statement statement = conn.createStatement()
            ) {
            statement.execute("SELECT number from numbers(200000) order by number");
            try(ResultSet rs = statement.getResultSet()){
                // ** We must call `rs.next()` otherwise the query may be canceled **
                while (rs.next()) {
                    System.out.println(rs.getInt(1));
                }
            }
        }
    }
}
```

### Important Notes

1. Close Connection/Statement/ResultSet to release resources faster.
2. Because the `select`, `copy into`, `merge into` are query type SQL, they will return a `ResultSet` object, you must
   call `rs.next()` before accessing the data. Otherwise, the query may be canceled. If you do not want get the result,
   you can call `while(r.next(){})` to iterate over the result set.
3. For other SQL such as `create/drop table` non-query type SQL, you can call `statement.execute()` directly.


## Connection Parameters 

For detailed references, please take a look at the following Links:

1. [Connection Parameters](./docs/Connection.md) : detailed documentation about how to use connection parameters in a
   jdbc connection

## JDBC Java type mapping
The Lake type is mapped to Java type as follows:

| Lake Type | Java Type      |
|---------------|----------------|
| TINYINT       | Byte           |
| SMALLINT      | Short          |
| INT           | Integer        |
| BIGINT        | Long           |
| UInt8         | Short          |
| UInt16        | Integer        |
| UInt32        | Long           |
| UInt64        | BigInteger     |
| Float32       | Float          |
| Float64       | Double         |
| Decimal       | BigDecimal     |
| String        | String         |
| Date          | LocalDate      |
| Timestamp     | ZonedDateTime  |
| Timestamp_TZ  | OffsetDateTime |
| Interval      | Duration       |
| Geometry      | byte[]         |
| Bitmap        | byte[]         |
| Array         | String         |
| Tuple         | String         |
| Map           | String         |
| VARIANT       | String         |

### Temporal types

we recommend using `java.time` to avoid ambiguity and set/get values via these APIs:

```
void setObject(int parameterIndex, Object x)
<T> T getObject(int columnIndex, Class<T> type)
```

- TIMESTAMP_TZ and TIMESTAMP map to `OffsetDateTime`, `ZonedDateTime`, `Instant` and `LocalDateTime` (TIMESTAMP_TZ can return `OffsetDateTime` but not `ZonedDateTime`).
- Date maps to `LocalDate`
- When parameters do not contain a timezone, Lake uses the session timezone (not the JVM zone) when storing/returning dates on lake-jdbc ≥ 0.4.3 AND Lake server ≥ 1.2.844.
- Interval map to `java.time.Duration`.

old Timestamp/Date are also supported, note that:

- `getTimestamp(int, Calendar cal)` is equivalent to `getTimestamp(int)` (the cal is omitted) and
`getObject(int, Instant.classes).toTimestamp()`
- `setTimestamp(int, Calendar cal)` is diff with `setTimestamp(int)`, the epoch is adjusted according to timezone in cal
- `setDate`/`getDate` still use the JVM timezone, `getDate(1)` is equivalent to `Date.valueOf(getObject(1, LocalDate.class))`, `setDate(1, date)` is equivalent to `setObject(1, date.toLocalDate())`.




# Unwrapping to Lake-specific interfaces 

## interface LakeConnection

The following code shows how to unwrap a JDBC Connection object to expose the methods of the LakeConnection interface.

```java
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import com.tidbcloud.jdbc.LakeConnection;

public class UnwrapExample {
    private static Connection openConnection() throws SQLException {
        String url = System.getenv("LAKE_JDBC_URL");
        String user = System.getenv("LAKE_JDBC_USER");
        String password = System.getenv("LAKE_JDBC_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) throws SQLException {
        try (Connection conn = openConnection()) {
            LakeConnection lakeConnection = conn.unwrap(LakeConnection.class);
        }
    }
}
```

### method `loadStreamToTable` 

```java
int loadStreamToTable(String sql, InputStream inputStream, long fileSize, LoadMethod loadMethod) throws SQLException;
```

Load data from a stream directly into a table, using either a staging or streaming approach.

Available with lake-jdbc >= 0.4.1 AND Lake server >= 1.2.791.

**Parameters:**
- `sql`: SQL statement with specific syntax for data loading, use special stage `_databend_load`
- `inputStream`: The input stream of the file data to load
- `fileSize`: The size of the file being loaded
- `loadMethod`: LoadMethod.STREAMING or LoadMethod.STAGE
  - `STAGE`: first upload file to a special path in user stage, then load the file in stage in to table, Limited by the max object size of storage of the stage.
    - the upload method is determined by connection parameter `presigned_url_disabled`.
  - `STREAMING` load data to while transforming data in one http request. Limited by server memory when load large Parquet/Orc file, whose meta is at the file end.

**Returns:** Number of rows successfully loaded

example:


```java
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.tidbcloud.jdbc.LakeConnection;

public class LoadStreamExample {
    private static Connection openConnection() throws SQLException {
        String url = System.getenv("LAKE_JDBC_URL") + "&presigned_url_disabled=true";
        String user = System.getenv("LAKE_JDBC_USER");
        String password = System.getenv("LAKE_JDBC_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) throws SQLException {
        byte[] csv = "1,hello\n2,world\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String tableName = "readme_load_example";
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ByteArrayInputStream fileStream = new ByteArrayInputStream(csv)) {
            LakeConnection lakeConnection = conn.unwrap(LakeConnection.class);
            stmt.execute("create or replace table " + tableName + " (id int, value string)");

            // use special stage `_databend_load`
            String sql = "insert into " + tableName + " from @_databend_load file_format=(type=csv)";

            lakeConnection.loadStreamToTable(
                    sql,
                    fileStream,
                    csv.length,
                    LakeConnection.LoadMethod.STAGE);
            stmt.executeQuery("select count(*) from " + tableName);
        }
    }
}
```

### Use Arrow Result Format

By default, the driver fetches query results in JSON format. To enable Arrow over HTTP, add
`query_result_format=arrow` to the JDBC URL:

```java
String url = System.getenv("LAKE_JDBC_URL");
if (!url.contains("query_result_format=")) {
    url = url + (url.contains("?") ? "&" : "?") + "query_result_format=arrow";
}
Connection conn = DriverManager.getConnection(
    url,
    System.getenv("LAKE_JDBC_USER"),
    System.getenv("LAKE_JDBC_PASSWORD"));
```

Arrow mode is intended for query result fetching. Internal control queries still use JSON when needed.

Requirements:

1. Lake server must support Arrow result pages.
2. The JVM must allow Arrow to access `java.nio` internals.

Before starting your application, set:

```shell
export JAVA_TOOL_OPTIONS='--add-opens=java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true'
```

If you do not want to set `JAVA_TOOL_OPTIONS` globally, pass the same options directly to `java`:

```shell
java --add-opens=java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true -jar your-app.jar
```

If `query_result_format` is not specified, the driver uses JSON.
# Explicit TLS modes

`sslmode=enable`, `require`, `verify-ca` and `verify-full` select HTTPS with
certificate and hostname verification. Values are case-insensitive. All these
modes retain hostname verification; `verify-ca` does not weaken it. An explicit
port is retained. `require`, `verify-ca` and `verify-full` use port 443 when no
port is given; the historical `sslmode=enable` implicit port 8000 is preserved.
`ssl=true` also enables HTTPS. The existing default and `sslmode=disable` remain
plain HTTP for compatibility with local deployments. Unknown legacy spellings
retain their previous handling with a warning; use a supported mode explicitly.

### Security defaults for the next release

Unqualified JDBC endpoints now use verified HTTPS on port 443. Plaintext requires
an explicit HTTP scheme, `ssl=false` or `sslmode=disable`; invalid TLS modes fail
rather than downgrading. All TLS modes keep certificate and hostname verification.
`copy_purge` defaults to false. Default connection/query/socket timeouts are
60/3600/600 seconds; explicit timeout values remain available.

Presigned storage requests require public HTTPS targets by default. Private,
loopback, link-local, reserved and mixed public/private DNS answers are rejected
at connection-time resolution, and redirects/proxies are disabled. Storage URLs
and response bodies are omitted from diagnostics. For customer-controlled private
storage (including local MinIO), explicitly set `presign_allow_private=true`;
this also permits HTTP and must only be used for trusted storage endpoints.

### Schema and stage-name handling

`Connection.setSchema` takes a plain database name, which the driver quotes as
one identifier. Do not include SQL identifier delimiters in the argument.
Streaming-load qualification also quotes the current schema returned by the
server. Driver control operations accept one SQL statement; a trailing semicolon
and comments remain supported.

Stage names and file paths are data, including spaces, quotes and semicolons.
Presign requests quote the entire stage location as a string literal. Both
presigned transfers and direct stage uploads validate paths before reading the
stream or sending a request: parent (`..`) segments, encoded parent traversal,
backslashes and control characters are rejected. Redundant slashes and `.` path
segments are normalized; a null stage selects the user's `~` stage.
Server permissions still determine which stages and databases a credential may
access; these client APIs do not sandbox an overprivileged connection.
