# JDBC Driver

TiDB Cloud Lake JDBC driver accesses Lake deployments through the
[REST API](https://tidbcloud.com/doc/integrations/api/rest).
To use jdbc documentation, you could add the following dependency from maven central

```xml

<dependency>
    <groupId>com.tidbcloud</groupId>
    <artifactId>lake-jdbc</artifactId>
    <version>|version|</version>
</dependency>
```

## Driver Name

The driver class name is `com.tidbcloud.jdbc.LakeDriver`. Most JDBC drivers are automatically loaded by
the `DriverManager` class. However, if you are using a JDBC driver that is not automatically loaded, you can load it by
calling the `Class.forName` method.

## Connecting

The following JDBC URL formats are supported

```text
jdbc:lake://host:port
jdbc:lake://user:password@host:port
jdbc:lake://host:port/database
jdbc:lake://user:password@host:port/database
```

For example, the following URL connects to a local Lake host on `0.0.0.0:8000`
and database `hello_lake`
with username `tidbcloud` and password `secret`

```text
jdbc:lake://tidbcloud:secret@0.0.0.0:8000/hello_lake
```

The above URL can be used as follows

```java 
String url="jdbc:lake://tidbcloud:secret@0.0.0.0:8000/hello_lake"
        Connection conn=DriverManager.getConnection(url);
```

If you are using TiDB Cloud, you can get a warehouse DSN according
to [this doc](https://tidbcloud.com/cloud/using-tidb-cloud/warehouses#connecting).
Then the above URL within warehouse DSN can be used as follows:

```java 
        String url="jdbc:lake://cloudapp:password@sample-cluster.gw.tidbcloud.com:443/db_name?ssl=true"
        Connection conn=DriverManager.getConnection(url);
```

TiDB Cloud Lake JDBC URLs accept a single host. Automatic node discovery, load balancing, and failover are not supported.

## Arrow result format

By default, query results are returned in JSON format. To fetch query results in Arrow format, set
`query_result_format=arrow` in the JDBC URL:

```text
jdbc:lake://tidbcloud:secret@0.0.0.0:8000/hello_lake?query_result_format=arrow
```

Arrow mode is used for query result fetching. If `query_result_format` is not set, the driver uses JSON.

When Arrow is enabled, start the JVM with:

```shell
export JAVA_TOOL_OPTIONS='--add-opens=java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true'
```

Or pass the same options directly to `java`:

```shell
java --add-opens=java.base/java.nio=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true -jar your-app.jar
```

## Connection parameters

The driver supports various parameters that may be set as URL parameters or as properties passed to DriverManager. Both
of the following examples are equivalent:

```java
// URL parameters
String url="jdbc:lake://tidbcloud:secret@0.0.0.0:8000/hello_lake";
        Properties properties=new Properties();
        properties.setProperty("user","test");
        properties.setProperty("password","secret");
        properties.setProperty("SSL","true");
        Connection connection=DriverManager.getConnection(url,properties);

// properties
        String url="jdbc:lake://tidbcloud:secret@0.0.0.0:8000/hello_lake?user=test&password=secret&SSL=true";
        Connection connection=DriverManager.getConnection(url);
```

### Parameter References

| Parameter              | Description                                                                                                               | Default       | example                                                                                                  |
|------------------------|---------------------------------------------------------------------------------------------------------------------------|---------------|----------------------------------------------------------------------------------------------------------|
| user                   | Lake user name                                                                                                        | none          | jdbc:lake://0.0.0.0:8000/hello_lake?user=test                                                        |
| password               | Lake user password                                                                                                    | none          | jdbc:lake://0.0.0.0:8000/hello_lake?password=secret                                                  |
| SSL                    | Enable SSL                                                                                                            | false         | jdbc:lake://0.0.0.0:8000/hello_lake?SSL=true                                                         |
| sslmode                | SSL mode                                                                                                              | disable       | jdbc:lake://0.0.0.0:8000/hello_lake?sslmode=enable                                                   |
| copy_purge             | If true, purge files from the stage after they are loaded successfully into the table                                 | false         | jdbc:lake://0.0.0.0:8000/hello_lake?copy_purge=true                                                  |
| presigned_url_disabled | Whether to use presigned URLs to upload data. If you use local disk as the storage layer, set this to true.          | false         | jdbc:lake://0.0.0.0:8000/hello_lake?presigned_url_disabled=true                                      |
| presign                | Controls presign mode for data upload. Values: `auto` (enable for managed Lake cloud domains, disable otherwise), `detect` (probe the server to determine support), `on` (always enable), `off` (always disable). When set, takes precedence over presigned_url_disabled | none          | jdbc:lake://0.0.0.0:8000/hello_lake?presign=auto                                                     |
| query_result_format    | Query result format. Supported values: `json` and `arrow`. Default is `json`                                            | json          | jdbc:lake://0.0.0.0:8000/default?query_result_format=arrow                                          |
| wait_time_secs         | REST query API blocking time. If the query is not finished, the API blocks for wait_time_secs seconds                 | 10            | jdbc:lake://0.0.0.0:8000/hello_lake?wait_time_secs=10                                                |
| max_rows_per_page      | the maximum rows per page in response data body                                                                           | 100000        | jdbc:lake://0.0.0.0:8000/default?max_rows_per_page=100000                                            |
| null_display           | Null value display                                                                                                     | \N            | jdbc:lake://0.0.0.0:8000/hello_lake?null_display=null                                                |
| binary_format          | binary format, support hex and base64                                                                                     | hex           | jdbc:lake://0.0.0.0:8000/default?binary_format=hex                                                   |
| use_verify             | whether verify the server before establishing the connection                                                              | true          | jdbc:lake://0.0.0.0:8000/default?use_verify=true                                                     |
| debug                  | whether enable debug mode                                                                                                 | false         | jdbc:lake://0.0.0.0:8000/default?debug=true                                                          |
| session_settings       | Set Lake session settings                                                                                              | ""            | jdbc:lake://0.0.0.0:8000/default?session_settings="key1=value1,key2=value2"                          |
