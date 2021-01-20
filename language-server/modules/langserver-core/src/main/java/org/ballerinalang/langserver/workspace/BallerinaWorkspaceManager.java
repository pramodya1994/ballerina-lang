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
package org.ballerinalang.langserver.workspace;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.BuildOptionsBuilder;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.util.ProjectPaths;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.LSContextOperation;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentManager;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Contains a set of utility methods to manage projects.
 *
 * @since 2.0.0
 */
public class BallerinaWorkspaceManager implements WorkspaceManager {
    /**
     * Mapping of source root to project instance.
     */
    private final Map<Path, ProjectPair> sourceRootToProject = new HashMap<>();
    /**
     * Cache mapping of document path to source root.
     */
    private final Map<Path, Path> pathToSourceRootCache;
    private static final LanguageServerContext.Key<BallerinaWorkspaceManager> WORKSPACE_MANAGER_KEY =
            new LanguageServerContext.Key<>();
    private final LSClientLogger clientLogger;

    private BallerinaWorkspaceManager(LanguageServerContext serverContext) {
        serverContext.put(WORKSPACE_MANAGER_KEY, this);
        this.clientLogger = LSClientLogger.getInstance(serverContext);
        Cache<Path, Path> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
        this.pathToSourceRootCache = cache.asMap();
    }

    public static BallerinaWorkspaceManager getInstance(LanguageServerContext serverContext) {
        BallerinaWorkspaceManager workspaceManager = serverContext.get(WORKSPACE_MANAGER_KEY);
        if (workspaceManager == null) {
            workspaceManager = new BallerinaWorkspaceManager(serverContext);
        }

        return workspaceManager;
    }

    @Override
    public Optional<String> relativePath(Path path) {
        Optional<Document> document = this.document(path);
        return document.map(Document::name);
    }

    /**
     * Returns a project root from the path provided.
     *
     * @param path ballerina project or standalone file path
     * @return project root
     */
    public Path projectRoot(Path path) {
        return pathToSourceRootCache.computeIfAbsent(path, this::computeProjectRoot);
    }

    /**
     * Returns project from the path provided.
     *
     * @param filePath ballerina project or standalone file path
     * @return project of applicable type
     */
    @Override
    public Optional<Project> project(Path filePath) {
        return projectPair(filePath).map(ProjectPair::project);
    }

    /**
     * Returns module from the path provided.
     *
     * @param filePath file path of the document
     * @return project of applicable type
     */
    @Override
    public Optional<Module> module(Path filePath) {
        Optional<Project> project = project(filePath);
        if (project.isEmpty()) {
            return Optional.empty();
        }
        Optional<Document> document = document(filePath, project.get());
        if (document.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(document.get().module());
    }

    /**
     * Returns document of the project of this path.
     *
     * @param filePath file path of the document
     * @return {@link Document}
     */
    @Override
    public Optional<Document> document(Path filePath) {
        Optional<Project> project = project(filePath);
        return project.isPresent() ? document(filePath, project.get()) : Optional.empty();
    }

    /**
     * Returns syntax tree from the path provided.
     *
     * @param filePath file path of the document
     * @return {@link io.ballerina.compiler.syntax.tree.SyntaxTree}
     */
    @Override
    public Optional<SyntaxTree> syntaxTree(Path filePath) {
        Optional<Document> document = this.document(filePath);
        if (document.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(document.get().syntaxTree());
    }

    /**
     * Returns semantic model from the path provided.
     *
     * @param filePath file path of the document
     * @return {@link SemanticModel}
     */
    @Override
    public Optional<SemanticModel> semanticModel(Path filePath) {
        Optional<Module> module = this.module(filePath);
        if (module.isEmpty()) {
            return Optional.empty();
        }
        return waitAndGetPackageCompilation(filePath)
                .map(pkgCompilation -> pkgCompilation.getSemanticModel(module.get().moduleId()));
    }

    /**
     * Returns module compilation from the file path provided.
     *
     * @param filePath file path of the document
     * @return {@link ModuleCompilation}
     */
    @Override
    public Optional<PackageCompilation> waitAndGetPackageCompilation(Path filePath) {
        // Get Project and Lock
        Optional<ProjectPair> projectPair = projectPair(filePath);
        if (projectPair.isEmpty()) {
            return Optional.empty();
        }
        Optional<Document> document = document(filePath, projectPair.get().project());
        if (document.isEmpty()) {
            return Optional.empty();
        }
        // Get Package
        Package packageInstance = document.get().module().packageInstance();
        // Lock Project Instance
        projectPair.get().locker().lock();
        try {
            return Optional.of(packageInstance.getCompilation());
        } finally {
            // Unlock Project Instance
            projectPair.get().locker().unlock();
        }
    }

    /**
     * The document open notification is sent from the client to the server to signal newly opened text documents.
     *
     * @param filePath {@link Path} of the document
     * @param params   {@link DidOpenTextDocumentParams}
     */
    @Override
    public void didOpen(Path filePath, DidOpenTextDocumentParams params) {
        // Create Project, if not exists
        Path projectRoot = projectRoot(filePath);
        sourceRootToProject.computeIfAbsent(projectRoot, path -> createProject(filePath));
        // Get document
        ProjectPair projectPair = sourceRootToProject.get(projectRoot);
        if (projectPair == null) {
            // NOTE: This will never happen since we create a project if not exists
            return;
        }

        // Check if new document is not loaded to Project Instance
        Optional<Document> document = document(filePath, projectPair.project());
        if (document.isEmpty()) {
            // Lock Project Instance
            projectPair.locker().lock();
            try {
                // Reload the project
                projectPair.setProject(createProject(filePath).project());
            } finally {
                // Unlock Project Instance
                projectPair.locker().unlock();
            }
        }
    }

    /**
     * The document change notification is sent from the client to the server to signal changes to a text document.
     *
     * @param filePath {@link Path} of the document
     * @param params   {@link DidChangeTextDocumentParams}
     * @throws WorkspaceDocumentException when project or document not found
     */
    @Override
    public void didChange(Path filePath, DidChangeTextDocumentParams params) throws WorkspaceDocumentException {
        // Get Project and Lock
        Optional<ProjectPair> projectPair = projectPair(filePath);
        if (projectPair.isEmpty()) {
            throw new WorkspaceDocumentException("Cannot add changes to a file in an un-opened project!");
        }
        // Lock Project Instance
        projectPair.get().locker().lock();
        try {
            // Get document
            Optional<Document> document = document(filePath, projectPair.get().project());
            if (document.isEmpty()) {
                throw new WorkspaceDocumentException("Document does not exist in path: " + filePath.toString());
            }

            // Update file
            String content = params.getContentChanges().get(0).getText();
            Document updatedDoc = document.get().modify().withContent(content).apply();

            // Update project instance
            projectPair.get().setProject(updatedDoc.module().project());
        } finally {
            // Unlock Project Instance
            projectPair.get().locker().unlock();
        }
    }

    /**
     * The document close notification is sent from the client to the server when the document got closed in the
     * client.
     *
     * @param filePath {@link Path} of the document
     * @param params   {@link DidCloseTextDocumentParams}
     * @throws WorkspaceDocumentException when project not found
     */
    @Override
    public void didClose(Path filePath, DidCloseTextDocumentParams params) throws WorkspaceDocumentException {
        Optional<Project> project = project(filePath);
        if (project.isEmpty()) {
            throw new WorkspaceDocumentException("Cannot close a file in an un-opened project!");
        }
        // If it is a single file project, remove project from mapping
        if (project.get().kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            Path projectRoot = project.get().sourceRoot();
            sourceRootToProject.remove(projectRoot);
            clientLogger.logTrace("Operation '" + LSContextOperation.TXT_DID_CLOSE.getName() +
                    "' {project: '" + projectRoot.toUri().toString() +
                    "' kind: '" + project.get().kind().name().toLowerCase(Locale.getDefault()) +
                    "'} removed}");
        }
    }

    // ============================================================================================================== //

    private Path computeProjectRoot(Path path) {
        return computeProjectKindAndProjectRoot(path).getRight();
    }

    private Pair<ProjectKind, Path> computeProjectKindAndProjectRoot(Path path) {
        if (ProjectPaths.isStandaloneBalFile(path)) {
            return new ImmutablePair<>(ProjectKind.SINGLE_FILE_PROJECT, path);
        } else {
            return new ImmutablePair<>(ProjectKind.BUILD_PROJECT, ProjectPaths.packageRoot(path));
        }
    }

    private Optional<ProjectPair> projectPair(Path filePath) {
        return Optional.ofNullable(sourceRootToProject.get(projectRoot(filePath)));
    }

    private ProjectPair createProject(Path filePath) {
        Pair<ProjectKind, Path> projectKindAndProjectRootPair = computeProjectKindAndProjectRoot(filePath);
        ProjectKind projectKind = projectKindAndProjectRootPair.getLeft();
        Path projectRoot = projectKindAndProjectRootPair.getRight();
        try {
            Project project;
            BuildOptions options = new BuildOptionsBuilder().offline(true).build();
            if (projectKind == ProjectKind.BUILD_PROJECT) {
                project = BuildProject.load(projectRoot, options);
            } else {
                project = SingleFileProject.load(projectRoot, options);
            }
            clientLogger.logTrace("Operation '" + LSContextOperation.TXT_DID_OPEN.getName() +
                                          "' {project: '" + projectRoot.toUri().toString() + "' kind: '" +
                                          project.kind().name().toLowerCase(Locale.getDefault()) + "'} created}");
            return ProjectPair.from(project);
        } catch (ProjectException e) {
            clientLogger.notifyUser("Project load failed: " + e.getMessage(), e);
            clientLogger.logError("Operation '" + LSContextOperation.TXT_DID_OPEN.getName() +
                                          "' {project: '" + projectRoot.toUri().toString() + "' kind: '" +
                                          projectKind.name().toLowerCase(Locale.getDefault()) + "'} failed}", e,
                                  new TextDocumentIdentifier(filePath.toUri().toString()));
            return null;
        }
    }

    private Optional<Document> document(Path filePath, Project project) {
        try {
            DocumentId documentId = project.documentId(filePath);
            Module module = project.currentPackage().module(documentId.moduleId());
            return Optional.of(module.document(documentId));
        } catch (ProjectException e) {
            return Optional.empty();
        }
    }

    /**
     * This class holds project and its lock.
     */
    public static class ProjectPair {

        private final Lock lock;
        private Project project;

        private ProjectPair(Project project, Lock lock) {
            this.project = project;
            this.lock = lock;
        }

        public static ProjectPair from(Project project) {
            return new ProjectPair(project, new ReentrantLock(true));
        }

        public static ProjectPair from(Project project, Lock lock) {
            return new ProjectPair(project, lock);
        }

        /**
         * Returns the associated lock for the file.
         *
         * @return {@link Lock}
         */
        public Lock locker() {
            return this.lock;
        }

        /**
         * Returns the workspace document.
         *
         * @return {@link WorkspaceDocumentManager}
         */
        public Project project() {
            return this.project;
        }

        /**
         * Set workspace document.
         *
         * @param project {@link Project}
         */
        public void setProject(Project project) {
            this.project = project;
        }
    }
}
