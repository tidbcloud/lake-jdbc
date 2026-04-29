package com.tidbcloud.jdbc;

import java.util.regex.Pattern;

/**
 * Lake server constants.
 *
 * @author wayne
 */
class LakeConstant {
    public static final String ENABLE_STR = "enable";
    public static final String BASE64_STR = "base64";
    public static final Pattern INSERT_INTO_PATTERN = Pattern.compile("(insert|replace)\\s+into");

    public static final String KEYWORDS_SELECT = "select";
}
