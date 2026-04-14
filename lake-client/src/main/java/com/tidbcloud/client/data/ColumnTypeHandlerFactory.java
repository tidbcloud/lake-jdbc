/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tidbcloud.client.data;

import java.util.Locale;

public class ColumnTypeHandlerFactory {
    public static ColumnTypeHandler getTypeHandler(LakeRawType type) {
        if (type == null) {
            return null;
        }
        switch (type.getDataType().getDisplayName().toLowerCase(Locale.US)) {
            case LakeTypes.INT8:
                return new Int8Handler(type.isNullable());
            case LakeTypes.INT16:
                return new Int16Handler(type.isNullable());
            case LakeTypes.INT32:
                return new Int32Handler(type.isNullable());
            case LakeTypes.INT64:
                return new Int64Handler(type.isNullable());
            case LakeTypes.UINT8:
                return new UInt8Handler(type.isNullable());
            case LakeTypes.UINT16:
                return new UInt16Handler(type.isNullable());
            case LakeTypes.UINT32:
                return new UInt32Handler(type.isNullable());
            case LakeTypes.UINT64:
                return new UInt64Handler(type.isNullable());
            case LakeTypes.FLOAT32:
                return new Float32Handler(type.isNullable());
            case LakeTypes.FLOAT64:
                return new Float64Handler(type.isNullable());
            case LakeTypes.BOOLEAN:
                return new BooleanHandler(type.isNullable());
            case LakeTypes.DECIMAL:
                return new DecimalHandler(type.isNullable());
            case LakeTypes.GEOMETRY:
                return new GeometryHandler(type.isNullable());
            case LakeTypes.ARRAY:
            case LakeTypes.DATE:
            case LakeTypes.DATETIME:
            case LakeTypes.DATETIME64:
            case LakeTypes.TIMESTAMP:
            case LakeTypes.STRING:
            case LakeTypes.NULL:
            case LakeTypes.STRUCT:

            case LakeTypes.VARIANT:
            case LakeTypes.VARIANT_ARRAY:
            case LakeTypes.VARIANT_OBJECT:
            case LakeTypes.INTERVAL:
            default:
                return new StringHandler(type.isNullable());
        }
    }
}
