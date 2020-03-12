/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.packerina.task;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.ballerinalang.cli.module.util.ErrorUtil;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.packerina.buildcontext.BuildContext;
import org.ballerinalang.packerina.buildcontext.BuildContextField;
import org.ballerinalang.packerina.model.ExecutableJar;
import org.ballerinalang.toml.model.Library;
import org.ballerinalang.toml.model.Manifest;
import org.ballerinalang.toml.parser.ManifestProcessor;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import javax.ws.rs.core.HttpHeaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.ballerinalang.tool.LauncherUtils.createLauncherException;

/**
 * Task for fetching mvn dependencies from mvn api, store in `target/libs` & modify `Ballerina.toml` object.
 */
public class FetchMvnDependenciesTask implements Task {

    private static PrintStream outStream = System.out;

    @Override
    public void execute(BuildContext buildContext) {
        Path targetLibsDirPath = Paths.get(buildContext.get(BuildContextField.TARGET_DIR).toString(), "libs");
        createTargetLibs(targetLibsDirPath);

        CompilerContext context = buildContext.get(BuildContextField.COMPILER_CONTEXT);
        Manifest manifest = ManifestProcessor.getInstance(context).getManifest();
        List<Library> platformLibraries = manifest.getPlatform().libraries;

        DependencyResult result = null;
        for (Library library:platformLibraries) {
            if (library.getPath() == null || library.getPath().equals("")) {
//                String groupId = library.groupId;
//                String artifactId = library.artifactId;
//                String version = library.version;
//                String[] modules = library.modules;

                if (library.groupId == null || library.groupId.equals("") || library.artifactId == null ||
                        library.artifactId.equals("")) {
                    throw createLauncherException("groupId or artifactId cannot be empty");
                } else if (library.version == null || library.version.equals("")) {
                    // get latest version for version
                    throw createLauncherException("version cannot be empty");
                }

                resolveDependencies(result, library.groupId, library.artifactId, library.version,
                        String.valueOf(targetLibsDirPath));

//                String jarName = artifactId + "-" + version + ".jar";
//
//                if (!new File(String.valueOf(targetLibsDirPath), jarName).exists()) {
//                    downloadDependencyJar(targetLibsDirPath, groupId, artifactId, version, jarName);
//                }
//                updateLibraryPath(buildContext, jarName, modules);
            }
        }

        updateLibraryPath(buildContext, result);
    }

    private static void resolveDependencies(DependencyResult result, String groupId, String artifactId, String version,
            String targetLibsPath) {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        RepositorySystem system = newRepositorySystem(locator);
//        RepositorySystem system = new DefaultRepositorySystem();
        RepositorySystemSession session = newSession(system, targetLibsPath);

        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
                .build();
        Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);

        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE),
                Arrays.asList(central));
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        DependencyRequest request = new DependencyRequest(collectRequest, filter);
        try {
            result = system.resolveDependencies(session, request);
        } catch (Exception e) {
            throw createLauncherException("resolving dependencies failed");
        }

        //        for (ArtifactResult artifactResult : result.getArtifactResults()) {
//            System.out.println(artifactResult.getArtifact().getFile());
//        }
    }

    private static RepositorySystem newRepositorySystem(DefaultServiceLocator locator) {
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newSession(RepositorySystem system, String localRepoPath) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoPath);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    private void createTargetLibs(Path targetLibsDirPath) {
        File targetLibsDir = new File(String.valueOf(targetLibsDirPath));
        if (!targetLibsDir.exists()) {
            if (!targetLibsDir.mkdir()) {
                throw createLauncherException("unable to create target/libs directory");
            }
        }
    }

    private void downloadDependencyJar(Path targetLibsDirPath, String groupId, String artifactId, String version, String jarName){
        String filePath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + jarName;

        URL url = convertToUrl("https://search.maven.org/remotecontent?filepath=" + filePath);
        HttpURLConnection conn = null;

        try {
            conn = createHttpUrlConnection(url);
            conn.setInstanceFollowRedirects(false);
            try {
                conn.setRequestMethod("GET");
            } catch (ProtocolException e) {
                throw createLauncherException(e.getMessage());
            }

            boolean redirect = false;
            // 301 - dependency jar is found
            // Other - Error occurred, json returned with the error message
            if (getStatusCode(conn) == HttpURLConnection.HTTP_MOVED_PERM) {
                redirect = true;
            } else {
                //handleErrorResponse(conn, url, moduleNameWithOrg);
            }

            if (redirect) {
                // get redirect url from "location" header field
                String newUrl = conn.getHeaderField(HttpHeaders.LOCATION);
                conn = createHttpUrlConnection(convertToUrl(newUrl));

                int statusCode = getStatusCode(conn);
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    Path jarFilePath = Paths.get(targetLibsDirPath.toString(), jarName);
                    try (InputStream inputStream = conn.getInputStream();
                            FileOutputStream outputStream = new FileOutputStream(jarFilePath.toString())) {
                        writeAndHandleProgress(inputStream, outputStream, conn.getContentLength() / 1024, jarName);
                    } catch (IOException e) {
                        throw createLauncherException("error occurred while downloading the dependency jar: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw createLauncherException(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void writeAndHandleProgress(InputStream inputStream, FileOutputStream outputStream,
            long totalSizeInKB, String jarName) {
        int count;
        byte[] buffer = new byte[1024];

        try (ProgressBar progressBar = new ProgressBar("Downloading " + jarName, totalSizeInKB, 1000, outStream,
                ProgressBarStyle.ASCII, " KB", 1)) {
            while ((count = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, count);
                progressBar.step();
            }
        } catch (IOException e) {
            outStream.println(jarName + " downloading failed");
        } finally {
            outStream.println(jarName + " downloaded successfully");
        }
    }

    private void updateLibraryPath(BuildContext buildContext, DependencyResult result) { // String jarName, String[] modules
//        Path libraryJarPath = Paths.get("./target/libs/" + jarName);
        Map<PackageID, ExecutableJar> a = buildContext.moduleDependencyPathMap;
//        for (String module : modules) {
//            if (a.containsKey(module)) {
//                a.get(module).moduleLibs.add(libraryJarPath);
//            }
//        }
    }

    /**
     * Convert string to URL.
     *
     * @param url string URL
     * @return URL
     */
    private static URL convertToUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw createLauncherException(e.getMessage());
        }
    }

    private static HttpURLConnection createHttpUrlConnection(URL url) {
        try {
                return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw createLauncherException(e.getMessage());
        }
    }

    /**
     * Get status code of http response.
     *
     * @param conn http connection
     * @return status code
     */
    private static int getStatusCode(HttpURLConnection conn) {
        try {
            return conn.getResponseCode();
        } catch (IOException e) {
            throw ErrorUtil
                    .createCommandException("connection to the remote repository host failed: " + e.getMessage());
        }
    }
}
