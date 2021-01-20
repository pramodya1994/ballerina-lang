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
package io.ballerina.projects;

import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.ast.TomlTableNode;
import io.ballerina.toml.validator.TomlValidator;
import io.ballerina.toml.validator.schema.Schema;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a generic TOML document in a Ballerina package.
 *
 * @since 2.0.0
 */
public abstract class TomlDocument {
    private final Path filePath;
    private final Path schemaPath;
    private TomlTableNode tomlAstNode;
    private Toml toml;

    protected TomlDocument(Path filePath, Path schemaPath) {
        this.filePath = filePath;
        this.schemaPath = schemaPath;
    }

    public Toml toml() {
        if (toml != null) {
            return toml;
        }

        parseToml();
        return toml;
    }

    public TomlTableNode tomlAstNode() {
        if (tomlAstNode != null) {
            return tomlAstNode;
        }

        parseToml();
        return tomlAstNode;
    }

    private void parseToml() {
        TomlValidator validator;
        try {
            validator = new TomlValidator(Schema.from(this.schemaPath));
        } catch (IOException e) {
            throw new ProjectException("Failed to read the TOML validator schema file from the path:"
                                               + this.schemaPath);
        }

        try {
            toml = Toml.read(this.filePath);
        } catch (IOException e) {
            throw new ProjectException("Failed to read the 'Ballerina.toml' file from the path:" + this.filePath);
        }

        if (toml == null) {
            throw new ProjectException("Failed to read the 'Ballerina.toml' file from the path:" + this.filePath);
        }

        validator.validate(toml);
        tomlAstNode = toml.rootNode();
    }
}
