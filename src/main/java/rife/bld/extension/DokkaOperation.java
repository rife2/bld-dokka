/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.BaseProject;
import rife.bld.extension.dokka.LoggingLevel;
import rife.bld.extension.dokka.OutputFormat;
import rife.bld.extension.dokka.SourceSet;
import rife.bld.extension.tools.CollectionTools;
import rife.bld.extension.tools.IOTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.extension.tools.TextTools;
import rife.bld.operations.AbstractProcessOperation;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Builds documentation (javadoc, HTML, etc.) using Dokka.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class DokkaOperation extends AbstractProcessOperation<DokkaOperation> {

    private static final String GFM_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|gfm-plugin|freemarker).*\\.jar$";
    private static final String HTML_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|kotlinx-html-jvm|freemarker).*\\.jar$";
    private static final String JAVADOC_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|javadoc-plugin|kotlin-as-java-plugin|korte-jvm).*\\.jar$";
    private static final String JEKYLL_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|jekyll-plugin|gfm-plugin|freemarker).*\\.jar$";
    private static final Logger LOGGER = Logger.getLogger(DokkaOperation.class.getName());
    private static final String SEMICOLON = ";";
    private final Map<String, String> globalLinks_ = new ConcurrentHashMap<>();
    private final List<String> globalPackageOptions_ = new ArrayList<>();
    private final List<String> globalSrcLinks_ = new ArrayList<>();
    private final List<File> includes_ = new ArrayList<>();
    private final List<File> pluginsClasspath_ = new ArrayList<>();
    private final Map<String, String> pluginsConfiguration_ = new ConcurrentHashMap<>();
    private boolean delayTemplateSubstitution_;
    private boolean failOnWarning_;
    private File json_;
    private LoggingLevel loggingLevel_;
    private String moduleName_;
    private String moduleVersion_;
    private boolean noSuppressObviousFunctions_;
    private boolean offlineMode_;
    private File outputDir_;
    private OutputFormat outputFormat_;
    private BaseProject project_;
    private SourceSet sourceSet_;
    private boolean suppressInheritedMembers_;

    @Override
    public void execute() throws IOException, InterruptedException, ExitStatusException {
        if (project_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("A project must be specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (outputFormat_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("An output format must be specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else {
            super.execute();
        }
    }

    /**
     * Part of the {@link #execute execute} operation, constructs the command list to use for building the process.
     *
     * @since 1.5
     */
    @Override
    protected List<String> executeConstructProcessCommandList() {
        final List<String> args = new ArrayList<>(50);

        if (project_ != null) {
            // java
            args.add(javaTool());

            var jarList = getJarList(project_.libBldDirectory(), "^.*dokka-cli.*\\.jar$");
            if (!jarList.isEmpty()) {
                // class path
                args.add("-cp");
                args.add(jarList.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
            }

            // main class
            args.add("org.jetbrains.dokka.MainKt");

            // -pluginClasspath
            var classPath = new ArrayList<>(pluginsClasspath_);
            if (outputFormat_ != null) {
                switch (outputFormat_) {
                    case HTML -> classPath.addAll(getJarList(project_.libBldDirectory(), HTML_PLUGIN_REGEXP));
                    case MARKDOWN -> classPath.addAll(getJarList(project_.libBldDirectory(), GFM_PLUGIN_REGEXP));
                    case JEKYLL -> classPath.addAll(getJarList(project_.libBldDirectory(), JEKYLL_PLUGIN_REGEXP));
                    default -> classPath.addAll(getJarList(project_.libBldDirectory(), JAVADOC_PLUGIN_REGEXP));
                }
            }
            if (!classPath.isEmpty()) {
                args.add("-pluginsClasspath");
                args.add(classPath.stream().map(File::getAbsolutePath).collect(Collectors.joining(SEMICOLON)));
            } else if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("No valid plugins jars found or specified.");
            }

            // -sourceSet
            var sourceSetArgs = sourceSet_.args();
            if (sourceSetArgs.isEmpty()) {
                throw new IllegalArgumentException("At least one sourceSet is required.");
            } else {
                args.add("-sourceSet");
                args.add(String.join(" ", sourceSet_.args()));
            }

            // -outputDir
            if (outputDir_ != null) {
                if (!IOTools.mkdirs(outputDir_)) {
                    throw new RuntimeException("Could not create: " + outputDir_.getAbsolutePath());
                }

                args.add("-outputDir");
                args.add(outputDir_.getAbsolutePath());
            }

            // -delayTemplateSubstitution
            if (delayTemplateSubstitution_) {
                args.add("-delayTemplateSubstitution");
            }

            // -failOnWarning
            if (failOnWarning_) {
                args.add("-failOnWarning");
            }

            // -globalLinks
            if (!globalLinks_.isEmpty()) {
                args.add("-globalLinks");
                var links = new ArrayList<String>();
                globalLinks_.forEach((k, v) ->
                        links.add(String.format("%s^%s", k, v)));
                args.add(String.join("^^", links));
            }

            // -globalPackageOptions
            if (!globalPackageOptions_.isEmpty()) {
                args.add("-globalPackageOptions");
                args.add(String.join(SEMICOLON, globalPackageOptions_));
            }

            // -globalSrcLinks
            if (!globalSrcLinks_.isEmpty()) {
                args.add("-globalSrcLinks");
                args.add(String.join(SEMICOLON, globalSrcLinks_));
            }

            // -includes
            if (!includes_.isEmpty()) {
                args.add("-includes");
                args.add(includes_.stream().map(File::getAbsolutePath).collect(Collectors.joining(SEMICOLON)));
            }

            // -loggingLevel
            if (loggingLevel_ != null) {
                args.add("-loggingLevel");
                args.add(loggingLevel_.toValue());
            }

            // -moduleName
            if (TextTools.isNotBlank(moduleName_)) {
                args.add("-moduleName");
                args.add(moduleName_);
            }

            // -moduleVersion
            if (TextTools.isNotBlank(moduleVersion_)) {
                args.add("-moduleVersion");
                args.add(moduleVersion_);
            }

            // -noSuppressObviousFunctions
            if (noSuppressObviousFunctions_) {
                args.add("-noSuppressObviousFunctions");
            }

            // -offlineMode
            if (offlineMode_) {
                args.add("-offlineMode");
            }

            // -pluginConfiguration
            if (!pluginsConfiguration_.isEmpty()) {
                args.add("-pluginsConfiguration");
                var confs = new ArrayList<String>();
                pluginsConfiguration_.forEach((k, v) ->
                        confs.add(String.format("%s=%s", encodeJson(k), encodeJson(v))));
                args.add(String.join("^^", confs));
            }

            // -suppressInheritedMembers
            if (suppressInheritedMembers_) {
                args.add("-suppressInheritedMembers");
            }

            // json
            if (json_ != null) {
                args.add(json_.getAbsolutePath());
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.join(" ", args));
            }
        }

        return args;
    }

    /**
     * Configures the operation from a {@link BaseProject}.
     * <p>
     * Sets the {@link #sourceSet sourceSet}, {@link SourceSet#jdkVersion jdkVersion}, {@link #moduleName moduleName}
     * and {@link SourceSet#classpath(File...) classpath} from the project.
     *
     * @param project the project to configure the operation from
     */
    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DokkaOperation fromProject(BaseProject project) {
        project_ = project;
        sourceSet_ = new SourceSet(silent())
                .src(new File(project.srcMainDirectory(), "kotlin"))
                .classpath(project.compileClasspathJars())
                .classpath(project.providedClasspathJars());
        if (project.javaRelease() != null) {
            sourceSet_ = sourceSet_.jdkVersion(project.javaRelease());
        }
        moduleName_ = project.name();
        return this;
    }

    // Encodes to JSON adding braces as needed
    private static String encodeJson(final String json) {
        var sb = new StringBuilder(json);
        if (!json.startsWith("{") || !json.endsWith("}")) {
            sb.insert(0, "{").append('}');
        }
        return StringUtils.encodeJson(sb.toString());
    }

    /**
     * Returns the JARs contained in a given directory.
     * <p>
     * Sources and Javadoc JARs are ignored.
     *
     * @param directory the directory
     * @param regex     the regular expression to match
     * @return the Java Archives
     */
    public static List<File> getJarList(File directory, String regex) {
        var jars = new ArrayList<File>();

        if (directory.isDirectory()) {
            var files = directory.listFiles();
            if (files != null) {
                for (var f : files) {
                    if (!f.getName().endsWith("-sources.jar") && (!f.getName().endsWith("-javadoc.jar")) &&
                            f.getName().matches(regex)) {
                        jars.add(f);
                    }
                }
            }
        }

        return jars;
    }

    /**
     * Sets the delay substitution of some elements.
     * <p>
     * Used in incremental builds of multimodule projects.
     *
     * @param delayTemplateSubstitution the delay
     * @return this operation instance
     */
    public DokkaOperation delayTemplateSubstitution(boolean delayTemplateSubstitution) {
        delayTemplateSubstitution_ = delayTemplateSubstitution;
        return this;
    }

    /**
     * Sets whether to fail documentation generation if Dokka has emitted a warning or an error.
     * <p>
     * Whether to fail documentation generation if Dokka has emitted a warning or an error. The process waits until all
     * errors and warnings have been emitted first.
     * <p>
     * This setting works well with {@link SourceSet#reportUndocumented}
     *
     * @param failOnWarning {@code true} or {@code false}
     * @return this operation instance
     */
    public DokkaOperation failOnWarning(boolean failOnWarning) {
        failOnWarning_ = failOnWarning;
        return this;
    }

    /**
     * Retrieves the global external documentation links.
     *
     * @return the documentation links
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> globalLinks() {
        return globalLinks_;
    }

    /**
     * Set the global external documentation links.
     *
     * @param url            the external documentation URL
     * @param packageListUrl the external documentation package list URL
     * @return this operation instance
     */
    public DokkaOperation globalLinks(String url, String packageListUrl) {
        if (TextTools.isNotBlank(url, packageListUrl)) {
            globalLinks_.put(url, packageListUrl);
        }
        return this;
    }

    /**
     * Set the global external documentation links.
     *
     * @param globalLinks the map of global links
     * @return this operation instance
     * @see #globalSrcLink(String...) #globalSrcLink(String...)#globalSrcLink(String...)
     */
    public DokkaOperation globalLinks(Map<String, String> globalLinks) {
        if (ObjectTools.isNotEmpty(globalLinks)) {
            globalLinks_.putAll(globalLinks);
        }
        return this;
    }

    /**
     * Sets the global package configurations.
     * <p>
     * Using format:
     * <ul>
     * <li>matchingRegexp</li>
     * <li>-deprecated</li>
     * <li>-privateApi</li>
     * <li>+warnUndocumented</li>
     * <li>+suppress</li>
     * <li>+visibility:PUBLIC</li>
     * <li>...</li>
     * </ul>
     *
     * @param options one or more package configurations
     * @return this operation instance
     */
    public DokkaOperation globalPackageOptions(String... options) {
        globalPackageOptions_.addAll(CollectionTools.combine(options));
        return this;
    }

    /**
     * Sets the global package configurations.
     * <p>
     * Using format:
     * <ul>
     * <li>matchingRegexp</li>
     * <li>-deprecated</li>
     * <li>-privateApi</li>
     * <li>+warnUndocumented</li>
     * <li>+suppress</li>
     * <li>+visibility:PUBLIC</li>
     * <li>...</li>
     * </ul>
     *
     * @param options the package configurations
     * @return this operation instance
     */
    @SafeVarargs
    public final DokkaOperation globalPackageOptions(Collection<String>... options) {
        globalPackageOptions_.addAll(CollectionTools.combine(options));
        return this;
    }

    /**
     * Retrieves the global package configurations.
     *
     * @return the package configurations
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> globalPackageOptions() {
        return globalPackageOptions_;
    }

    /**
     * Sets the global mapping between a source directory and a Web service for browsing the code.
     *
     * @param links one or more links mapping
     * @return this operation instance
     */
    public DokkaOperation globalSrcLink(String... links) {
        globalSrcLinks_.addAll(CollectionTools.combine(links));
        return this;
    }

    /**
     * Sets the global mapping between a source directory and a Web service for browsing the code.
     *
     * @param links the links mapping
     * @return this operation instance
     */
    @SafeVarargs
    public final DokkaOperation globalSrcLink(Collection<String>... links) {
        globalSrcLinks_.addAll(CollectionTools.combine(links));
        return this;
    }

    /**
     * Retrieves the global source links
     *
     * @return the source links
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> globalSrcLink() {
        return globalSrcLinks_;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on a per-package basis.
     *
     * @param files one or more files
     * @return this operation instance
     * @see #includes(Collection...)
     */
    public DokkaOperation includes(File... files) {
        includes_.addAll(CollectionTools.combine(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on a per-package basis.
     *
     * @param files the Markdown files
     * @return this operation instance
     * @see #includes(File...)
     */
    @SafeVarargs
    public final DokkaOperation includes(Collection<File>... files) {
        includes_.addAll(CollectionTools.combine(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on a per-package basis.
     *
     * @param files one or more files
     * @return this operation instance
     * @see #includesStrings(Collection...)
     */
    public DokkaOperation includes(String... files) {
        includes_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on a per-package basis.
     *
     * @param files one or more files
     * @return this operation instance
     * @see #includesPaths(Collection...)
     */
    public DokkaOperation includes(Path... files) {
        includes_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Retrieves the Markdown files that contain the module and package documentation.
     *
     * @return the Markdown files
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> includes() {
        return includes_;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on a per-package basis.
     *
     * @param files the Markdown files
     * @return this operation instance
     * @see #includes(Path...)
     */
    @SafeVarargs
    public final DokkaOperation includesPaths(Collection<Path>... files) {
        includes_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The contents of specified files are parsed and embedded into documentation as module and package descriptions.
     * <p>
     * This can be configured on a per-package basis.
     *
     * @param files the Markdown files
     * @return this operation instance
     * @see #includes(String...)
     */
    @SafeVarargs
    public final DokkaOperation includesStrings(Collection<String>... files) {
        includes_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * JSON configuration file path.
     *
     * @param configuration the configuration file path
     */
    public DokkaOperation json(Path configuration) {
        return json(configuration.toFile());
    }

    /**
     * JSON configuration file path.
     *
     * @param configuration the configuration file path
     */
    public DokkaOperation json(File configuration) {
        json_ = configuration;
        return this;
    }

    /**
     * Retrieves the JSON configuration file path.
     *
     * @return the configuration file path
     */
    public File json() {
        return json_;
    }

    /**
     * JSON configuration file path.
     *
     * @param configuration the configuration file path
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public DokkaOperation json(String configuration) {
        return json(new File(configuration));
    }

    /**
     * Sets the logging level.
     *
     * @param loggingLevel the logging level
     * @return this operation instance
     */
    public DokkaOperation loggingLevel(LoggingLevel loggingLevel) {
        loggingLevel_ = loggingLevel;
        return this;
    }

    /**
     * Sets the name of the project/module. Default is {@code root}.
     * <p>
     * The display name used to refer to the module. It is used for the table of contents, navigation, logging, etc.
     *
     * @param moduleName the project/module name
     * @return this operation instance
     */
    public DokkaOperation moduleName(String moduleName) {
        moduleName_ = moduleName;
        return this;
    }

    /**
     * Set the documented version.
     *
     * @param version the version
     * @return this operation instance
     */
    public DokkaOperation moduleVersion(String version) {
        moduleVersion_ = version;
        return this;
    }

    /**
     * Sets whether to suppress obvious functions such as inherited from
     * <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/">kotlin.Any</a> and {@link java.lang.Object}.
     * <p>
     * A function is considered to be obvious if it is:
     * <ul>
     * <li>Inherited from <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/">kotlin.Any</a>,
     * <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/">Kotlin.Enum</a>, {@link java.lang.Object}
     * or {@link java.lang.Enum}, such as {@code equals}, {@code hashCode}, {@code toString}.
     * <li>Synthetic (generated by the compiler) and does not have any documentation, such as
     * {@code dataClass.componentN} or {@code dataClass.copy}.
     * </ul>
     *
     * @param noSuppressObviousFunctions {@code true} or {@code false}
     * @return this operation instance
     */
    public DokkaOperation noSuppressObviousFunctions(boolean noSuppressObviousFunctions) {
        noSuppressObviousFunctions_ = noSuppressObviousFunctions;
        return this;
    }

    /**
     * Sets whether to resolve remote files/links over network.
     * <p>
     * This includes package-lists used for generating external documentation links. For example, to make classes from
     * the standard library clickable.
     * <p>
     * Setting this to true can significantly speed up build times in certain cases, but can also worsen documentation
     * quality and user experience. For example, by not resolving class/member links from your dependencies, including
     * the standard library.
     * <p>
     * Note: You can cache fetched files locally and provide them to Dokka as local paths.
     *
     * @param offlineMode the offline mode
     * @return this operation instance
     * @see SourceSet#externalDocumentationLinks(String, String)
     */
    public DokkaOperation offlineMode(boolean offlineMode) {
        offlineMode_ = offlineMode;
        return this;
    }

    /**
     * Retrieves the output directory path.
     *
     * @return the output directory
     */
    public File outputDir() {
        return outputDir_;
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     * <p>
     * The directory to where documentation is generated, regardless of output format.
     *
     * @param outputDir the output directory
     * @return this operation instance
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public DokkaOperation outputDir(String outputDir) {
        return outputDir(new File(outputDir));
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     * <p>
     * The directory to where documentation is generated, regardless of output format.
     *
     * @param outputDir the output directory
     * @return this operation instance
     */
    public DokkaOperation outputDir(File outputDir) {
        outputDir_ = outputDir;
        return this;
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     * <p>
     * The directory to where documentation is generated, regardless of output format.
     *
     * @param outputDir the output directory
     * @return this operation instance
     */
    public DokkaOperation outputDir(Path outputDir) {
        return outputDir(outputDir.toFile());
    }

    /**
     * Retrieves the output format.
     *
     * @return the output format
     */
    public OutputFormat outputFormat() {
        return outputFormat_;
    }

    /**
     * Sets the Dokka {@link OutputFormat output format}.
     *
     * @param format The {@link OutputFormat output format}
     * @return this operation instance
     */
    public DokkaOperation outputFormat(OutputFormat format) {
        if (format != null) {
            outputFormat_ = format;
        } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
            LOGGER.warning("No valid output format specified.");
        }
        return this;
    }

    /**
     * Sets the configuration for Dokka plugins.
     *
     * @param name              The fully qualified plugin name
     * @param jsonConfiguration The plugin JSON configuration
     * @return this operation instance
     */
    public DokkaOperation pluginConfigurations(String name, String jsonConfiguration) {
        if (TextTools.isNotBlank(name, jsonConfiguration)) {
            pluginsConfiguration_.put(name, jsonConfiguration);
        } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
            LOGGER.warning("A plugin name and configuration are required.");
        }
        return this;
    }

    /**
     * Sets the configuration for Dokka plugins.
     *
     * @param pluginConfigurations the map of configurations
     * @return this operation instance
     * @see #pluginConfigurations(String, String)
     */
    public DokkaOperation pluginConfigurations(Map<String, String> pluginConfigurations) {
        if (ObjectTools.isNotEmpty(pluginConfigurations)) {
            pluginsConfiguration_.putAll(pluginConfigurations);
        }
        return this;
    }

    /**
     * Retrieves the plugin configurations.
     *
     * @return the plugin configurations.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> pluginConfigurations() {
        return pluginsConfiguration_;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     * @see #includes(Collection...)
     */
    public DokkaOperation pluginsClasspath(File... jars) {
        pluginsClasspath_.addAll(CollectionTools.combine(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars the jars
     * @return this operation instance
     * @see #pluginsClasspath(Collection...)
     */
    @SafeVarargs
    public final DokkaOperation pluginsClasspath(Collection<File>... jars) {
        pluginsClasspath_.addAll(CollectionTools.combine(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     * @see #pluginsClasspathStrings(Collection...)
     */
    public DokkaOperation pluginsClasspath(String... jars) {
        pluginsClasspath_.addAll(CollectionTools.combineStringsToFiles(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     * @see #pluginsClasspathPaths(Collection...)
     */
    public DokkaOperation pluginsClasspath(Path... jars) {
        pluginsClasspath_.addAll(CollectionTools.combinePathsToFiles(jars));
        return this;
    }

    /**
     * Retrieves the plugins classpath.
     *
     * @return the classpath
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> pluginsClasspath() {
        return pluginsClasspath_;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars the jars
     * @return this operation instance
     * @see #pluginsClasspath(Path...)
     */
    @SafeVarargs
    public final DokkaOperation pluginsClasspathPaths(Collection<Path>... jars) {
        pluginsClasspath_.addAll(CollectionTools.combinePathsToFiles(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars the jars
     * @return this operation instance
     * @see #pluginsClasspath(String...)
     */
    @SafeVarargs
    public final DokkaOperation pluginsClasspathStrings(Collection<String>... jars) {
        pluginsClasspath_.addAll(CollectionTools.combineStringsToFiles(jars));
        return this;

    }

    /**
     * Sets the configurations for a source set.
     * <p>
     * Individual and additional configuration of Kotlin source sets.
     *
     * @param sourceSet the source set configurations
     * @return this operation instance
     */
    public DokkaOperation sourceSet(SourceSet sourceSet) {
        sourceSet_ = sourceSet;
        return this;
    }

    /**
     * Sets whether to suppress inherited members that aren't explicitly overridden in a given class.
     *
     * @param suppressInheritedMembers {@code true} or {@code false}
     * @return this operation instance
     */
    public DokkaOperation suppressInheritedMembers(boolean suppressInheritedMembers) {
        suppressInheritedMembers_ = suppressInheritedMembers;
        return this;
    }
}
