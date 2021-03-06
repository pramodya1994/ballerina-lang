/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.ballerinalang.bre.bvm.BVM;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;

import java.util.List;
import java.util.Map;

/**
 * This is a temporary BValue created to implement handle type related test cases.
 *
 * @since 1.0.0
 */
public class BHandleValue implements BRefType<Object> {

    private Object value;

    public BHandleValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public Boolean value() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String stringValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BType getType() {
        return BTypes.typeHandle;
    }

    @Override
    public void stamp(BType type, List<BVM.TypeValuePair> unresolvedValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BValue copy(Map<BValue, BValue> refs) {
        throw new UnsupportedOperationException();
    }
}
