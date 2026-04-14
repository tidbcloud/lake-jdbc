package com.tidbcloud.jdbc.cloud;

import org.apache.commons.lang3.StringUtils;

public class LakeStage {
    private static final String defaultStageName = "~";
    private final String stageName;
    private final String path;
    private final ExternalLocationS3 externalLocationS3;

    // TODO(zhihanz) support more external location such as Azure/GCS
    private LakeStage(String stageName, String path, ExternalLocationS3 externalLocationS3) {
        if (StringUtils.isBlank(stageName)) {
            this.stageName = defaultStageName;
        } else {
            this.stageName = stageName;
        }
        this.path = path;
        this.externalLocationS3 = externalLocationS3;
    }

    public static LakeStage.Builder builder() {
        return new LakeStage.Builder();
    }

    public String getStageName() {
        return stageName;
    }

    public String getPath() {
        return path;
    }

    public ExternalLocationS3 getExternalLocationS3() {
        return externalLocationS3;
    }

    @Override
    public String toString() {
        if (this.externalLocationS3 != null) {
            return this.externalLocationS3.toString();
        }
        if (this.stageName != null) {
            return String.format("@%s/%s", this.stageName, this.path);
        }
        return this.path;
    }


    public static class Builder {
        private String stageName;
        private String path;
        private ExternalLocationS3 externalLocationS3;

        public Builder stageName(String stageName) {
            this.stageName = stageName;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder externalLocationS3(ExternalLocationS3 externalLocationS3) {
            this.externalLocationS3 = externalLocationS3;
            return this;
        }

        public LakeStage build() {
            return new LakeStage(stageName, path, externalLocationS3);
        }
    }
}
