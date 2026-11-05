package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.internal.QueryResultFormat;
import com.tidbcloud.jdbc.internal.session.LakeSessionCookieJar;
import com.tidbcloud.jdbc.internal.session.SessionHandleConfig;
import com.tidbcloud.jdbc.internal.session.SessionState;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;

import static com.tidbcloud.jdbc.internal.http.OkHttpUtils.*;
import static com.tidbcloud.jdbc.ConnectionProperties.*;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Parses and extracts parameters from a lake JDBC URL
 */
final class LakeDriverUri {
    private static final String JDBC_URL_PREFIX = "jdbc:";
    private static final String JDBC_URL_START = JDBC_URL_PREFIX + "lake://";
    private static final Splitter QUERY_SPLITTER = Splitter.on('&').omitEmptyStrings();
    private static final Splitter ARG_SPLITTER = Splitter.on('=').limit(2);
    private static final char[] AUTHORITY_DELIMITERS = {'/', '?', '#'};
    /**
     * URL query parameters that carry a credential. They are parsed into the connection properties
     * and must never be retained in the canonical base URI.
     */
    private static final List<String> CREDENTIAL_QUERY_PARAMETERS =
            ImmutableList.of("password", "accesstoken", "access_token", "token");
    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final int DEFAULT_HTTP_PORT = 8000;
    private static final Logger logger = Logger.getLogger(LakeDriverUri.class.getPackage().getName());
    private final Properties properties;
    private final URI uri;
    private final boolean useSecureConnection;
    private final boolean useVerify;
    private final boolean debug;
    private final boolean strNullAsNull;
    private final String warehouse;
    private final String sslmode;
    private final String tenant;
    private final boolean copyPurge;
    private final String nullDisplay;
    private final String binaryFormat;
    private final QueryResultFormat queryResultFormat;
    private final String database;
    private final boolean presignedUrlDisabled;
    private final String presign;
    private final boolean presignAllowPrivate;
    private final Integer connectionTimeout;
    private final Integer queryTimeout;
    private final Integer socketTimeout;
    private final Integer waitTimeSecs;
    private final Integer maxRowsInBuffer;
    private final Integer maxRowsPerPage;

    private final Map<String, String> sessionSettings;

//    private final boolean useSecureConnection;

    private LakeDriverUri(String url, Properties driverProperties)
            throws SQLException {
        Map.Entry<URI, Map<String, String>> uriAndProperties = parse(url);
        URI rawUri = uriAndProperties.getKey();
        this.properties = mergeProperties(rawUri, uriAndProperties.getValue(), driverProperties);
        Map<String,String> selectors = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        selectors.putAll(parseParameters(rawUri.getQuery()));
        if (driverProperties != null) selectors.putAll(Maps.fromProperties(driverProperties));
        if (selectors.containsKey("sslmode") && !selectors.containsKey("ssl")) {
            properties.setProperty("ssl", "false");
        } else if (selectors.containsKey("ssl") && !selectors.containsKey("sslmode")) {
            properties.setProperty("sslmode", SSL.getValue(properties).orElse(false) ? "require" : "disable");
        }
        this.sslmode = SSL_MODE.getValue(properties).orElse("require");
        boolean modeRequiresTls = requiresTls(this.sslmode);
        this.useSecureConnection = SSL.getValue(properties).orElse(false) || modeRequiresTls;
        this.useVerify = USE_VERIFY.getValue(properties).orElse(false);
        this.debug = DEBUG.getValue(properties).orElse(false);
        this.strNullAsNull = STRNULL_AS_NULL.getValue(properties).orElse(true);
        this.warehouse = WAREHOUSE.getValue(properties).orElse("");
        this.tenant = TENANT.getValue(properties).orElse("");
        // Historical sslmode=enable (without ssl=true) used HTTPS on port 8000. Preserve that
        // implicit port; the newly recognized modes use the conventional HTTPS default.
        boolean useHttpsPort = SSL.getValue(properties).orElse(false) || !"enable".equalsIgnoreCase(this.sslmode);
        this.uri = canonicalizeUri(rawUri, this.useSecureConnection, useHttpsPort);
        this.database = DATABASE.getValue(properties).orElse("default");
        this.presignedUrlDisabled = PRESIGNED_URL_DISABLED.getRequiredValue(properties);
        this.presign = PRESIGN.getValue(properties).orElse("");
        this.presignAllowPrivate = PRESIGN_ALLOW_PRIVATE.getRequiredValue(properties);
        this.copyPurge = COPY_PURGE.getValue(properties).orElse(false);
        this.nullDisplay = NULL_DISPLAY.getValue(properties).orElse("\\N");
        this.binaryFormat = BINARY_FORMAT.getValue(properties).orElse("");
        this.queryResultFormat = QueryResultFormat.fromValue(QUERY_RESULT_FORMAT.getValue(properties).orElse("json"));
        this.waitTimeSecs = WAIT_TIME_SECS.getRequiredValue(properties);
        this.connectionTimeout = CONNECTION_TIMEOUT.getRequiredValue(properties);
        this.queryTimeout = QUERY_TIMEOUT.getRequiredValue(properties);
        this.maxRowsInBuffer = ConnectionProperties.MAX_ROWS_IN_BUFFER.getRequiredValue(properties);
        this.maxRowsPerPage = ConnectionProperties.MAX_ROWS_PER_PAGE.getRequiredValue(properties);
        Integer socketTimeout = SOCKET_TIMEOUT.getRequiredValue(properties);
        if (socketTimeout <= this.waitTimeSecs + 10) {
            this.socketTimeout = this.waitTimeSecs + 10;
        } else {
            this.socketTimeout = socketTimeout;
        }

        String settingsStr = SESSION_SETTINGS.getValue(properties).orElse("");
        this.sessionSettings = parseSessionSettings(settingsStr);

    }

    private Map<String, String> parseSessionSettings(String settingsStr) {
        if (isNullOrEmpty(settingsStr)) {
            return new HashMap<>();
        }

        // key1=value1,key2=value2
        Map<String, String> settings = new HashMap<>();
        String[] pairs = settingsStr.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                settings.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return settings;
    }


    public static LakeDriverUri create(String url, Properties properties)
            throws SQLException {
        return new LakeDriverUri(url, firstNonNull(properties, new Properties()));
    }

    private static void initDatabase(URI uri, Map<String, String> uriProperties) throws SQLException {
        String path = uri.getPath();
        if (isNullOrEmpty(path) || "/".equals(path)) {
            return;
        }
        if (!path.startsWith("/")) {
            throw new SQLException(format("Invalid database name '%s'", path));
        }

        String db = path.substring(1);
        uriProperties.put(DATABASE.getKey(), db);
    }

    private static boolean requiresTls(String mode) throws SQLException {
        switch (mode.toLowerCase(Locale.US)) {
            case "enable":
            case "require":
            case "verify-ca":
            case "verify-full":
                return true;
            case "disable":
                return false;
            default:
                throw new SQLException("Unrecognized sslmode; use enable, require, verify-ca, verify-full or disable");
        }
    }

    private static URI canonicalizeUri(URI uri, boolean isSSLSecured, boolean useHttpsPort) throws SQLException {
        requireNonNull(uri, "uri is null");
        String authority = uri.getAuthority();
        if (authority != null) {
            // The userinfo has already been parsed into the connection properties; a residual
            // "user:pass@" would make HostAndPort reject the authority and silently fall back to
            // the default port.
            int atPos = authority.lastIndexOf('@');
            if (atPos >= 0) {
                authority = authority.substring(atPos + 1);
            }
        }
        String scheme;
        if (isSSLSecured) {
            scheme = "https";
        } else {
            scheme = "http";
        }
        // The canonical base URI is used for every REST request, for DatabaseMetaData.getURL() and
        // for exception text. Credentials must not survive into it: they have already been parsed
        // into the connection properties, and keeping them here replays ?password=... / ?accesstoken=...
        // on every request and exposes them to server access logs and host application logs.
        String query = queryWithoutCredentials(uri.getQuery());
        int finalPort = -1;
        try {
            HostAndPort hostAndPort = HostAndPort.fromString(authority);
            if (hostAndPort.hasPort()) {
                finalPort = hostAndPort.getPort();
                return new URI(scheme, null, uri.getHost(), finalPort, uri.getPath(), query, uri.getFragment());
            }
        } catch (Exception e) {
            // ignore
        }
        if (finalPort == -1) {
            finalPort = isSSLSecured && useHttpsPort ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
        }
        try {

            return new URI(scheme, null, uri.getHost(), finalPort, uri.getPath(), query, uri.getFragment());
        } catch (URISyntaxException e) {
            throw new SQLException("Invalid URI: " + uri, e);
        }
    }

    private static String queryWithoutCredentials(String query) {
        if (isNullOrEmpty(query)) {
            return query;
        }
        List<String> kept = new ArrayList<>();
        for (String queryArg : QUERY_SPLITTER.split(query)) {
            List<String> parts = ARG_SPLITTER.splitToList(queryArg);
            if (parts.isEmpty()
                    || !CREDENTIAL_QUERY_PARAMETERS.contains(parts.get(0).toLowerCase(Locale.US))) {
                kept.add(queryArg);
            }
        }
        return kept.isEmpty() ? null : String.join("&", kept);
    }

    private static String tryParseUriUserPassword(String url, Map<String, String> properties) {
        // Only the authority section can carry userinfo. An '@' that appears after it - typically
        // inside a query-string value such as ?warehouse=x@evil.example.com - must not be taken as
        // the userinfo delimiter: doing so redefines the target host and sends the credentials and
        // all query data to an attacker-chosen server.
        int authorityEnd = url.length();
        for (char delimiter : AUTHORITY_DELIMITERS) {
            int index = url.indexOf(delimiter);
            if (index >= 0 && index < authorityEnd) {
                authorityEnd = index;
            }
        }
        int atPos = url.lastIndexOf('@', authorityEnd - 1);
        if (atPos > 0) {
            setUserInfoProperties(url.substring(0, atPos), properties);
            url = url.substring(atPos + 1);
        }
        return url;
    }

    /**
     * Parses a {@code user[:password]} userinfo section into the connection properties.
     *
     * <p>Percent-escapes are decoded: the value is used verbatim as the HTTP basic-auth credential,
     * so {@code p%2Fss} has to become {@code p/ss} before it is sent. Encoding it instead (the
     * pre-0.4.10 behaviour) made every password containing {@code : @ / % + space} unusable.
     */
    private static void setUserInfoProperties(String userInfo, Map<String, String> properties) {
        int colonPos = userInfo.indexOf(':');
        if (colonPos > 0) {
            properties.put(USER.getKey(), decodeUserInfoComponent(userInfo.substring(0, colonPos)));
            properties.put(ConnectionProperties.PASSWORD.getKey(),
                    decodeUserInfoComponent(userInfo.substring(colonPos + 1)));
        } else {
            properties.put(USER.getKey(), decodeUserInfoComponent(userInfo));
        }
    }

    /**
     * Decodes percent-escapes the way the userinfo component of a URI defines them: {@code %XX} is
     * one byte of the UTF-8 encoded value and everything else - including {@code +} - is literal.
     */
    static String decodeUserInfoComponent(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); ) {
            if (value.charAt(i) == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    decoded.write((high << 4) + low);
                    i += 3;
                    continue;
                }
            }
            int codePoint = value.codePointAt(i);
            byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
            decoded.write(bytes, 0, bytes.length);
            i += Character.charCount(codePoint);
        }
        return new String(decoded.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Consumes the userinfo that a URL carries inside the URI itself.
     *
     * <p>Only the {@code jdbc:lake://http(s)://user:password@host:port/db} form reaches this
     * method: its scheme prefix makes the first {@code /} end the authority section, so
     * {@link #tryParseUriUserPassword} deliberately leaves the userinfo untouched there. It has to
     * be consumed before the URI is canonicalized, because {@link #canonicalizeUri} resolves the
     * port from the authority and {@code HostAndPort} rejects an authority that still contains '@'.
     */
    private static URI stripUriUserInfo(URI uri, Map<String, String> properties) throws SQLException {
        String userInfo = uri.getUserInfo();
        if (isNullOrEmpty(userInfo)) {
            return uri;
        }
        // URI#getUserInfo() is decoded already, so the values are stored as they are.
        int colonPos = userInfo.indexOf(':');
        if (colonPos > 0) {
            properties.put(USER.getKey(), userInfo.substring(0, colonPos));
            properties.put(ConnectionProperties.PASSWORD.getKey(), userInfo.substring(colonPos + 1));
        } else {
            properties.put(USER.getKey(), userInfo);
        }
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new SQLException("Invalid URI: " + uri, e);
        }
    }

    private static void setProperties(Properties properties, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            properties.setProperty(entry.getKey().toLowerCase(Locale.US), entry.getValue());
        }
    }

    private static Properties mergeProperties(URI uri, Map<String, String> uriProperties, Properties driverProperties) throws SQLException {
        Map<String, String> defaults = ConnectionProperties.getDefaults();
        Map<String, String> urlProperties = parseParameters(uri.getQuery());
        Map<String, String> suppliedProperties = Maps.fromProperties(driverProperties);
        Properties result = new Properties();
        setProperties(result, defaults);
        setProperties(result, uriProperties);
        setProperties(result, urlProperties);
        setProperties(result, suppliedProperties);
        return result;
    }

    // needs to parse possible host, port, username, password, tenant, warehouse, database, etc.
    private static Map.Entry<URI, Map<String, String>> parse(String url)
            throws SQLException {
        if (url == null) {
            throw new SQLException("URL is null");
        }

        int pos = url.indexOf(JDBC_URL_START);
        if (pos != 0) {
            throw new SQLException("Invalid JDBC URL: " + url + " URL does not start with " + JDBC_URL_START);
        }
        Map<String, String> uriProperties = new LinkedHashMap<>();
        String raw = url.substring(pos + JDBC_URL_START.length());
        raw = tryParseUriUserPassword(raw, uriProperties);
        ensureSingleHostAuthority(raw, url);

        try {
            URI uri = parseSingleUri(raw);
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new SQLException("Invalid host " + uri.getHost());
            }
            uri = stripUriUserInfo(uri, uriProperties);

            if ("https".equals(uri.getScheme())) {
                uriProperties.put(SSL.getKey(), "true");
                uriProperties.put(SSL_MODE.getKey(), "require");
            } else {
                uriProperties.put(SSL.getKey(), "false");
                uriProperties.put(SSL_MODE.getKey(), "disable");
            }

            initDatabase(uri, uriProperties);
            return new AbstractMap.SimpleImmutableEntry<>(uri, uriProperties);
        } catch (URISyntaxException e) {
            throw new SQLException("Invalid URI: " + raw, e);
        }
    }

    private static URI parseSingleUri(String rawHost) throws URISyntaxException {
        String fullUri = (rawHost.startsWith("http://") || rawHost.startsWith("https://")) ?
                rawHost :
                "https://" + rawHost;
        return new URI(fullUri);
    }

    private static void ensureSingleHostAuthority(String raw, String originalUrl) throws SQLException {
        int endOfAuthority = raw.length();
        int slashIndex = raw.indexOf('/');
        if (slashIndex != -1 && slashIndex < endOfAuthority) {
            endOfAuthority = slashIndex;
        }
        int questionIndex = raw.indexOf('?');
        if (questionIndex != -1 && questionIndex < endOfAuthority) {
            endOfAuthority = questionIndex;
        }
        int fragmentIndex = raw.indexOf('#');
        if (fragmentIndex != -1 && fragmentIndex < endOfAuthority) {
            endOfAuthority = fragmentIndex;
        }
        String authoritySection = raw.substring(0, endOfAuthority);
        if (authoritySection.contains(",")) {
            throw new SQLException("Multiple hosts in JDBC URL are not supported: " + originalUrl);
        }
    }

    public static boolean acceptsURL(String url) {
        return url.startsWith(JDBC_URL_START);
    }

    private static Map<String, String> parseParameters(String query)
            throws SQLException {
        Map<String, String> result = new HashMap<>();

        if (query != null) {
            Iterable<String> queryArgs = QUERY_SPLITTER.split(query);
            for (String queryArg : queryArgs) {
                List<String> parts = ARG_SPLITTER.splitToList(queryArg);
                if (parts.size() != 2) {
                    throw new SQLException(format("Connection argument is not valid connection property: '%s'", queryArg));
                }
                if (result.put(parts.get(0), parts.get(1)) != null) {
                    throw new SQLException(format("Connection property '%s' is in URL multiple times", parts.get(0)));
                }
            }
        }

        return result;
    }

    public URI getUri() {
        return uri;
    }

    public String getDatabase() {
        return database;
    }

    public Boolean presignedUrlDisabled() {
        return presignedUrlDisabled;
    }

    public String getPresign() {
        return presign;
    }

    public Boolean copyPurge() {
        return copyPurge;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public boolean getStrNullAsNull() {
        return strNullAsNull;
    }

    public boolean getUseVerify() {
        return useVerify;
    }

    public boolean getDebug() {
        return debug;
    }

    public String getSslmode() {
        return sslmode;
    }

    public String getTenant() {
        return tenant;
    }

    public String nullDisplay() {
        return nullDisplay;
    }

    public String binaryFormat() {
        return binaryFormat;
    }

    public String getQueryResultFormat() {
        return queryResultFormat.value();
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public Integer getWaitTimeSecs() {
        return waitTimeSecs;
    }

    public Integer getMaxRowsInBuffer() {
        return maxRowsInBuffer;
    }

    public Integer getMaxRowsPerPage() {
        return maxRowsPerPage;
    }

    public Map<String, String> getSessionSettings() {
        return sessionSettings;
    }

    public SessionHandleConfig toSessionHandleConfig() {
        SessionState initialSession = new SessionState.Builder()
                .setDatabase(this.database)
                .setSettings(this.sessionSettings)
                .build();
        return SessionHandleConfig.builder()
                .setPresignAllowPrivate(this.presignAllowPrivate)
                .setBaseUri(this.uri)
                .setQueryTimeoutSecs(this.queryTimeout)
                .setConnectionTimeoutSecs(this.connectionTimeout)
                .setSocketTimeoutSecs(this.socketTimeout)
                .setQueryResultFormat(this.queryResultFormat)
                .setWaitTimeSecs(this.waitTimeSecs)
                .setMaxRowsInBuffer(this.maxRowsInBuffer)
                .setMaxRowsPerPage(this.maxRowsPerPage)
                .setWarehouse(this.warehouse)
                .setTenant(this.tenant)
                .setDebug(this.debug)
                .setInitialSession(initialSession)
                .build();
    }

    public Properties getProperties() {
        return properties;
    }

    public void setupClient(OkHttpClient.Builder builder) throws SQLException {
        try {
            LakeSessionCookieJar cookieJar = new LakeSessionCookieJar();
            cookieJar.add(new Cookie.Builder().name("cookie_enabled").value("true").domain("not_used").build());
            builder.cookieJar(cookieJar);

            String password = PASSWORD.getValue(properties).orElse("");
            builder.addInterceptor(basicAuthInterceptor(USER.getValue(properties).orElse(""), password));
            if (useSecureConnection) {
                setupSsl(builder);
            }
            if (ACCESS_TOKEN.getValue(properties).isPresent()) {
                builder.addInterceptor(tokenAuth(ACCESS_TOKEN.getValue(properties).get()));
            }
            if (CONNECTION_TIMEOUT.getValue(properties).isPresent()) {
                Integer timeout =  CONNECTION_TIMEOUT.getValue(properties).get();
                if (timeout != 0)
                    builder.connectTimeout(timeout, TimeUnit.SECONDS);
            }
            if (SOCKET_TIMEOUT.getValue(properties).isPresent()) {
                builder.readTimeout(SOCKET_TIMEOUT.getValue(properties).get(), TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            throw new SQLException("Failed to setup client", e);
        }
    }

}
