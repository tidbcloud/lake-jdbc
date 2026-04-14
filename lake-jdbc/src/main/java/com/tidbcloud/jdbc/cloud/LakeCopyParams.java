package com.tidbcloud.jdbc.cloud;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class LakeCopyParams {
    private static final String defaultType = "CSV";
    private final List<String> files;
    private final String pattern;

    private LakeStage lakeStage;

    private final String type;
    private final String databaseTableName;
    private final Map<String, String> fileOptions;
    private final Map<String, String> copyOptions;

    private LakeCopyParams(LakeStage lakeStage, List<String> files, String pattern, String type, String databaseTableName, Map<String, String> fileOptions, Map<String, String> copyOptions) {
        this.lakeStage = lakeStage;
        this.databaseTableName = databaseTableName;
        this.files = files;
        this.pattern = pattern;
        if (type != null) {
            this.type = type;
        } else {
            this.type = defaultType;
        }
        this.fileOptions = fileOptions;
        this.copyOptions = copyOptions;
    }

    private static void parseParam(Map.Entry<String, String> s, StringBuilder sb) {
        boolean needQuote = true;
        try {
            LakeParams p = LakeParams.valueOf(s.getKey().toUpperCase(Locale.US));
            needQuote = p.needQuote();
        } catch (IllegalArgumentException ignored) {
        }
        if (needQuote) {
            sb.append(s.getKey()).append(" = ").append("'").append(s.getValue()).append("'").append(" ");
        } else {
            sb.append(s.getKey()).append(" = ").append(s.getValue()).append(" ");
        }
    }

    public static LakeCopyParams.Builder builder() {
        return new LakeCopyParams.Builder();
    }

    public LakeStage getLakeStage() {
        return lakeStage;
    }

    public List<String> getFiles() {
        return files;
    }

    public String getPattern() {
        return pattern;
    }

    public String getType() {
        return type;
    }

    public String getDatabaseTableName() {
        return databaseTableName;
    }


    public Map<String, String> getFileOptions() {
        return fileOptions;
    }

    public Map<String, String> getCopyOptions() {
        return copyOptions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.files != null && !this.files.isEmpty()) {
            StringJoiner s = new StringJoiner(",");
            for (String file : this.files) {
                s.add("'" + file + "'");
            }
            sb.append("FILES = ");
            sb.append("(");
            sb.append(s);
            sb.append(")");
            sb.append(" ");
        }

        if (this.pattern != null) {
            sb.append("PATTERN = ");
            sb.append("'").append(this.pattern).append("'").append(" ");
        }
        sb.append("FILE_FORMAT = ( ");
        sb.append("type = ");
        sb.append("'").append(this.type).append("'").append(" ");
        if (this.fileOptions != null) {
            for (Map.Entry<String, String> e : this.fileOptions.entrySet()) {
                parseParam(e, sb);
            }
        }
        sb.append(")").append(" ");
        if (this.copyOptions != null) {
            for (Map.Entry<String, String> e : this.copyOptions.entrySet()) {
                parseParam(e, sb);
            }
        }
        return sb.toString();
    }

    public enum LakeParams {
        RECORD_DELIMITER("RECORD_DELIMITER", String.class),
        FIELD_DELIMITER("FIELD_DELIMITER", String.class),
        SKIP_HEADER("SKIP_HEADER", Integer.class),
        QUOTE("QUOTE", String.class),
        ESCAPE("ESCAPE", String.class),
        NAN_DISPLAY("NAN_DISPLAY", String.class),
        ROW_TAG("ROW_TAG", String.class),
        COMPRESSION("COMPRESSION", String.class),
        SIZE_LIMIT("SIZE_LIMIT", Integer.class),
        // default false
        PURGE("PURGE", Boolean.class),
        FORCE("FORCE", Boolean.class),
        // on error only support continue/abort without quote
        ON_ERROR("ON_ERROR", null);


        private final String name;
        private final Class<?> type;

        LakeParams(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public boolean needQuote() {
            if (type == null) {
                return false;
            }
            return type == String.class;
        }
    }

    public static class Builder {
        private LakeStage lakeStage;
        private List<String> files;
        private String pattern;

        private String type;
        private String databaseTableName;
        private Map<String, String> fileOptions;
        private Map<String, String> copyOptions;

        public Builder setLakeStage(LakeStage lakeStage) {
            if (lakeStage == null) {
                LakeStage stage = LakeStage.builder().stageName("~").path("/").build();
                this.lakeStage = stage;
                return this;
            }
            this.lakeStage = lakeStage;
            return this;
        }

        public Builder setFiles(List<String> files) {
            this.files = files;
            return this;
        }

        public Builder setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setDatabaseTableName(String databaseTableName) {
            this.databaseTableName = databaseTableName;
            return this;
        }

        public Builder setFileOptions(Map<String, String> fileOptions) {
            this.fileOptions = fileOptions;
            return this;
        }

        public Builder setCopyOptions(Map<String, String> copyOptions) {
            this.copyOptions = copyOptions;
            return this;
        }

        public LakeCopyParams build() {
            return new LakeCopyParams(lakeStage, files, pattern, type, databaseTableName, fileOptions, copyOptions);
        }
    }
}
