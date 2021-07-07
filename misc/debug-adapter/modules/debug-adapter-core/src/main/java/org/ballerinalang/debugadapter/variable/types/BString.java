/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.debugadapter.variable.types;

import com.sun.jdi.Value;
import org.ballerinalang.debugadapter.SuspendedContext;
import org.ballerinalang.debugadapter.variable.BSimpleVariable;
import org.ballerinalang.debugadapter.variable.BVariableType;

import static org.ballerinalang.debugadapter.variable.VariableUtils.UNKNOWN_VALUE;
import static org.ballerinalang.debugadapter.variable.VariableUtils.getStringFrom;

/**
 * Ballerina string variable type.
 */
public class BString extends BSimpleVariable {

    private static final String SYMBOL_DOUBLE_QUOTE = "\"";

    public BString(SuspendedContext context, String name, Value value) {
        super(context, name, BVariableType.STRING, value);
    }

    @Override
    public String computeValue() {
        try {
            // Add double quote to the beginning and end of the computed string value.
            return SYMBOL_DOUBLE_QUOTE.concat(getStringFrom(jvmValue)).concat(SYMBOL_DOUBLE_QUOTE);
        } catch (Exception ignored) {
            return UNKNOWN_VALUE;
        }
    }
}
