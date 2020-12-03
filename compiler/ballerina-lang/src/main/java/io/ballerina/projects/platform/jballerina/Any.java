/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.projects.platform.jballerina;

import io.ballerina.projects.CompilerBackend;

/**
 * Represents any version that supported by the jBallerina backend.
 *
 * @since 2.0.0
 */
public enum Any implements CompilerBackend.TargetPlatform {
    ANY("any"),
    ;

    private final String code;

    Any(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
