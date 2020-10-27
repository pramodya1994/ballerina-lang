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
package io.ballerina.projects.test;

import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JdkVersion;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contains cases to test the JarWriter.
 *
 * @since 2.0.0
 */
public class TestJarWriter {
    private static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/");
    Path tempDirectory;

    @Test (description = "tests writing of the executable")
    public void testJarWriter() throws IOException {
        Path projectPath = RESOURCE_DIRECTORY.resolve("balowriter").resolve("projectOne");

        // 1) Initialize the project instance
        BuildProject project = null;
        try {
            project = BuildProject.loadProject(projectPath);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        // 2) Load the package
        Package currentPackage = project.currentPackage();

        tempDirectory = Files.createTempDirectory("ballerina-test-" + System.nanoTime());
        Path defaultJarPath = tempDirectory.resolve("winery.jar");
        Path servicesJarPath = tempDirectory.resolve("services.jar");
        Path storageJarPath = tempDirectory.resolve("storage.jar");
        Assert.assertFalse(defaultJarPath.toFile().exists());
        Assert.assertFalse(servicesJarPath.toFile().exists());
        Assert.assertFalse(storageJarPath.toFile().exists());

        PackageCompilation pkgCompilation = currentPackage.getCompilation();
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(pkgCompilation, JdkVersion.JAVA_11);
        jBallerinaBackend.emit(JBallerinaBackend.OutputType.JAR, tempDirectory);

        Assert.assertTrue(defaultJarPath.toFile().exists());
        Assert.assertTrue(defaultJarPath.toFile().length() > 0);
        Assert.assertTrue(servicesJarPath.toFile().exists());
        Assert.assertTrue(servicesJarPath.toFile().length() > 0);
        Assert.assertTrue(storageJarPath.toFile().exists());
        Assert.assertTrue(storageJarPath.toFile().length() > 0);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        if (tempDirectory.toFile().exists()) {
            TestUtils.deleteDirectory(tempDirectory.toFile());
        }
    }
}