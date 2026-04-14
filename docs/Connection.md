# JDBC Driver

Lake JDBC driver access lake distributions or lake cloud
through [REST API]{https://lake.rs/doc/integrations/api/rest}.
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

For example, the following URL connects to lake host on your local machine with host `0.0.0.0` port `8000` and
database `hello_lake`
with username `lake` password `secret`

```text
jdbc:lake://lake:secret@0.0.0.0:8000/hello_lake
```

The above URL can be used as follows

```java 
String url="jdbc:lake://lake:secret@0.0.0.0:8000/hello_lake"
        Connection conn=DriverManager.getConnection(url);
```

If you are using [Lake Cloud](https://app.lake.com/), you can get a warehouse DSN according
to [this doc](https://lake.rs/cloud/using-lake-cloud/warehouses#connecting).
Then the above URL within warehouse DSN can be used as follows:

```java 
        String url="jdbc:lake://cloudapp:password@tnf34b0rm--elt-wh-medium.gw.aliyun-cn-beijing.default.lake.cn:443/db_name?ssl=true"
        Connection conn=DriverManager.getConnection(url);
```

Lake JDBC URLs accept a single host. Automatic node discovery, load balancing, and failover are not supported.

## Connection parameters

The driver supports various parameters that may be set as URL parameters or as properties passed to DriverManager. Both
of the following examples are equivalent:

```java
// URL parameters
String url="jdbc:lake://lake:secret@0.0.0.0:8000/hello_lake";
        Properties properties=new Properties();
        properties.setProperty("user","test");
        properties.setProperty("password","secret");
        properties.setProperty("SSL","true");
        Connection connection=DriverManager.getConnection(url,properties);

// properties
        String url="jdbc:lake://lake:secret@0.0.0.0:8000/hello_lake?user=test&password=secret&SSL=true";
        Connection connection=DriverManager.getConnection(url);
```

### Parameter References

| Parameter              | Description                                                                                                               | Default       | example                                                                                                  |
|------------------------|---------------------------------------------------------------------------------------------------------------------------|---------------|----------------------------------------------------------------------------------------------------------|
| user                   | Lake user name                                                                                                        | none          | jdbc:lake://0.0.0.0:8000/hello_lake?user=test                                                    |
| password               | Lake user password                                                                                                    | none          | jdbc:lake://0.0.0.0:8000/hello_lake?password=secret                                              |
| SSL                    | Enable SSL                                                                                                                | false         | jdbc:lake://0.0.0.0:8000/hello_lake?SSL=true                                                     |
| sslmode                | SSL mode                                                                                                                  | disable       | jdbc:lake://0.0.0.0:8000/hello_lake?sslmode=enable                                               |
| copy_purge             | If True, the command will purge the files in the stage after they are loaded successfully into the table                  | false         | jdbc:lake://0.0.0.0:8000/hello_lake?copy_purge=true                                              |
| presigned_url_disabled | whether use presigned url to upload data, generally if you use local disk as your storage layer, it should be set as true | false         | jdbc:lake://0.0.0.0:8000/hello_lake?presigned_url_disabled=true                                  |
| wait_time_secs         | Restful query api blocking time, if the query is not finished, the api will block for wait_time_secs seconds              | 10            | jdbc:lake://0.0.0.0:8000/hello_lake?wait_time_secs=10                                            |
| max_rows_per_page      | the maximum rows per page in response data body                                                                           | 100000        | jdbc:lake://0.0.0.0:8000/default?max_rows_per_page=100000                                            |
| null_display           | null value display                                                                                                        | \N            | jdbc:lake://0.0.0.0:8000/hello_lake?null_display=null                                            |
| binary_format          | binary format, support hex and base64                                                                                     | hex           | jdbc:lake://0.0.0.0:8000/default?binary_format=hex                                                   |
| use_verify             | whether verify the server before establishing the connection                                                              | true          | jdbc:lake://0.0.0.0:8000/default?use_verify=true                                                     |
| debug                  | whether enable debug mode                                                                                                 | false         | jdbc:lake://0.0.0.0:8000/default?debug=true                                                          |
| session_settings | set lake session settings                                                                                             | ""            | jdbc:lake://0.0.0.0:8000/default?session_settings="key1=value1,key2=value2"                          |
