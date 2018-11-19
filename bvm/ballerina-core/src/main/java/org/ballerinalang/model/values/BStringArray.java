/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.model.values;

import org.ballerinalang.bre.bvm.CPU;
import org.ballerinalang.model.types.BArrayType;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.util.BLangConstants;

import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;

import static org.ballerinalang.model.util.FreezeUtils.handleInvalidUpdate;

/**
 * @since 0.87
 */
public class BStringArray extends BNewArray {

    private String[] values;

    public BStringArray(String[] values) {
        this.values = values;
        this.size = values.length;
        super.arrayType = new BArrayType(BTypes.typeString);
    }

    public BStringArray() {
        values = (String[]) newArrayInstance(String.class);
        Arrays.fill(values, BLangConstants.STRING_EMPTY_VALUE);
        super.arrayType = new BArrayType(BTypes.typeString);
    }

    public BStringArray(int size) {
        if (size != -1) {
            this.size = maxArraySize = size;
        }
        values = (String[]) newArrayInstance(String.class);
        Arrays.fill(values, BLangConstants.STRING_EMPTY_VALUE);
        super.arrayType = new BArrayType(BTypes.typeString, size);
    }

    public void add(long index, String value) {
        synchronized (this) {
            if (freezeStatus.getState() != CPU.FreezeStatus.State.UNFROZEN) {
                handleInvalidUpdate(freezeStatus.getState());
            }
        }

        prepareForAdd(index, values.length);
        values[(int) index] = value;
    }

    public String get(long index) {
        rangeCheckForGet(index, size);
        return values[(int) index];
    }

    @SuppressWarnings("unchecked")
    public String[] getStringArray() {
        return values;
    }

    @Override
    public BType getType() {
        return arrayType;
    }

    @Override
    public void grow(int newLength) {
        values = Arrays.copyOf(values, newLength);
    }

    @Override
    public BValue copy(Map<BValue, BValue> refs) {
        if (isFrozen()) {
            return this;
        }

        if (refs.containsKey(this)) {
            return refs.get(this);
        }

        BStringArray stringArray = new BStringArray(Arrays.copyOf(values, values.length));
        stringArray.size = this.size;
        refs.put(this, stringArray);
        return stringArray;
    }

    @Override
    public String stringValue() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (int i = 0; i < size; i++) {
            sj.add("\"" + values[i] + "\"");
        }
        return sj.toString();
    }

    @Override
    public BValue getBValue(long index) {
        return new BString(get(index));
    }
}
