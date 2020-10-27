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

package io.ballerina.projects.balo;

import io.ballerina.projects.PackageConfig;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import io.ballerina.projects.env.BuildEnvContext;
import io.ballerina.projects.environment.EnvironmentContext;
import io.ballerina.projects.environment.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.ballerina.projects.utils.ProjectConstants.PACKAGE_JSON;

/**
 * {@code BaloProject} represents Ballerina project instance created from a balo.
 *
 * @since 2.0.0
 */
public class BaloProject extends Project {

    /**
     * Loads a BaloProject from the provided balo path.
     *
     * @param baloPath Balo path
     * @return balo project
     */
    public static BaloProject loadProject(Path baloPath, Repository repo) {
        Path absBaloPath = Optional.of(baloPath.toAbsolutePath()).get();
        if (!absBaloPath.toFile().exists()) {
            throw new RuntimeException("balo path does not exist:" + baloPath);
        }

        return new BaloProject(BuildEnvContext.getInstance(), absBaloPath, repo);
    }

    private BaloProject(EnvironmentContext environmentContext, Path baloPath, Repository repo) {
        super(environmentContext);
        this.sourceRoot = baloPath;
        addPackage(baloPath.toString(), repo);
    }

    /**
     * Loads a package from the provided balo path.
     *
     * @param baloPath balo path
     */
    private void addPackage(String baloPath, Repository repo) {
        PackageDescriptor packageDescriptor = BaloFiles.createPackageDescriptor(Paths.get(baloPath, PACKAGE_JSON));
        PackageConfig packageConfig = BaloPackageLoader.loadPackage(baloPath, packageDescriptor);
        packageConfig.setRepository(Optional.ofNullable(repo));
        this.addPackage(packageConfig);
    }
}