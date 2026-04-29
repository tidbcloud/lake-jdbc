package com.tidbcloud.jdbc.internal.query;

import com.tidbcloud.jdbc.internal.data.LakeRawType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.MoreObjects.toStringHelper;

public class QueryRowField {
    private final String name;
    private final LakeRawType dataType;

    @JsonCreator
    public QueryRowField(
            @JsonProperty("name") String name,
            @JsonProperty("type") LakeRawType dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("type")
    public LakeRawType getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name).add("type", dataType).toString();
    }
}
