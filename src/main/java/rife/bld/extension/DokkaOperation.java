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

import edu.umd.cs.findbugs.annotations.NonNull;
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Builds documentation (javadoc, HTML, etc.) using Dokka.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Builder pattern intentionally exposes mutable collections"
)
public class DokkaOperation extends AbstractProcessOperation<DokkaOperation> {

    /**
     * Separator used by Dokka CLI between list items in a single argument value.
     */
    public static final String DOKKA_LIST_SEPARATOR = ";";
    private static final String GFM_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|gfm-plugin|freemarker).*\\.jar$";
    private static final String HTML_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|kotlinx-html-jvm|freemarker).*\\.jar$";
    private static final String INCLUDES = "includes";
    private static final String JAVADOC_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|javadoc-plugin|kotlin-as-java-plugin|korte-jvm).*\\.jar$";
    private static final String JEKYLL_PLUGIN_REGEXP =
            "^.*(dokka-base|analysis-kotlin-descriptors|jekyll-plugin|gfm-plugin|freemarker).*\\.jar$";
    private static final String PLUGINS_CLASSPATH = "pluginsClasspath";
    private static final Logger logger = Logger.getLogger(DokkaOperation.class.getName());

    // LinkedHashMap preserves insertion order for deterministic CLI output; single-threaded builder usage
    // does not require ConcurrentHashMap here.
    private final Map<String, String> globalLinks_ = new LinkedHashMap<>();
    private final List<String> globalPackageOptions_ = new ArrayList<>();
    private final List<String> globalSrcLinks_ = new ArrayList<>();
    private final List<File> includes_ = new ArrayList<>();
    private final List<File> pluginsClasspath_ = new ArrayList<>();
    private final Map<String, String> pluginsConfiguration_ = new LinkedHashMap<>();
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

    /**
     * Performs this operation.
     *
     * @throws NullPointerException     if {@code project} or {@link #outputFormat() outputformat}
     *                                  or {@code sourceSet} are {@code null}
     * @throws IllegalArgumentException if {@link #json() json} is {@code null} or does not exist
     */
    @Override
    public void execute() throws IOException, InterruptedException, ExitStatusException {
        ObjectTools.requireNonNull(project_, "project");
        ObjectTools.requireNonNull(outputFormat_, "outputFormat");
        ObjectTools.requireNonNull(sourceSet_, "sourceSet");

        if (json_ != null && !json_.exists()) {
            throw new IllegalArgumentException("JSON config not found: " + json_.getAbsolutePath());
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
        if (project_ == null || outputFormat_ == null || sourceSet_ == null) {
            if (!silent() && logger.isLoggable(Level.WARNING)) {
                logger.warning("Missing required fields: project=" + project_ + ", outputFormat=" + outputFormat_
                        + ", sourceSet=" + sourceSet_);
            }
            return Collections.emptyList();
        }

        var args = new ArrayList<String>(50);

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
        switch (outputFormat_) {
            case HTML -> classPath.addAll(getJarList(project_.libBldDirectory(), HTML_PLUGIN_REGEXP));
            case MARKDOWN -> classPath.addAll(getJarList(project_.libBldDirectory(), GFM_PLUGIN_REGEXP));
            case JEKYLL -> classPath.addAll(getJarList(project_.libBldDirectory(), JEKYLL_PLUGIN_REGEXP));
            default -> classPath.addAll(getJarList(project_.libBldDirectory(), JAVADOC_PLUGIN_REGEXP));
        }
        if (!classPath.isEmpty()) {
            args.add("-pluginsClasspath");
            args.add(classPath.stream().map(File::getAbsolutePath).collect(Collectors.joining(DOKKA_LIST_SEPARATOR)));
        } else if (logger.isLoggable(Level.SEVERE) && !silent()) {
            logger.severe("No valid plugins jars found or specified.");
        }

        // -sourceSet
        var sourceSetArgs = sourceSet_.args();
        if (sourceSetArgs.isEmpty()) {
            throw new IllegalArgumentException("At least one sourceSet is required.");
        } else {
            args.add("-sourceSet");
            args.add(String.join(" ", sourceSetArgs));
        }

        // -outputDir
        try {
            if (IOTools.createDirs(outputDir_)) { // false if null or empty output directory
                args.add("-outputDir");
                args.add(outputDir_.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create output directory: " + outputDir_, e);
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
            globalLinks_.forEach((k, v) -> links.add(String.format("%s^%s", k, v)));
            args.add(String.join("^^", links));
        }

        // -globalPackageOptions
        if (!globalPackageOptions_.isEmpty()) {
            args.add("-globalPackageOptions");
            args.add(String.join(DOKKA_LIST_SEPARATOR, globalPackageOptions_));
        }

        // -globalSrcLinks
        if (!globalSrcLinks_.isEmpty()) {
            args.add("-globalSrcLinks");
            args.add(String.join(DOKKA_LIST_SEPARATOR, globalSrcLinks_));
        }

        // -includes
        if (!includes_.isEmpty()) {
            args.add("-includes");
            args.add(includes_.stream().map(File::getAbsolutePath).collect(Collectors.joining(DOKKA_LIST_SEPARATOR)));
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

        // -pluginsConfiguration
        if (!pluginsConfiguration_.isEmpty()) {
            args.add("-pluginsConfiguration");
            var confs = new ArrayList<String>();
            pluginsConfiguration_.forEach((k, v) -> confs.add(k + "=" + v));
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

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.join(" ", args));
        }

        return args;
    }

    /**
     * Configures the operation from a {@link BaseProject}.
     * <p>
     * Sets the {@link #sourceSet sourceSet}, {@link SourceSet#jdkVersion jdkVersion}, {@link #moduleName moduleName}
     * and {@link SourceSet#classpath(File...) classpath} from the project, if not already set.
     *
     * @param project the project to configure the operation from
     */
    @Override
    public DokkaOperation fromProject(@NonNull BaseProject project) {
        project_ = ObjectTools.requireNonNull(project, "fromProject");
        if (sourceSet_ == null) {
            sourceSet_ = new SourceSet().src(new File(project.srcMainDirectory(), "kotlin"));
            if (!project.compileClasspathJars().isEmpty()) {
                sourceSet_.classpath(project.compileClasspathJars());
            }
            if (!project.providedClasspathJars().isEmpty()) {
                sourceSet_.classpath(project.providedClasspathJars());
            }
        }
        if (project.javaRelease() != null) {
            sourceSet_ = sourceSet_.jdkVersion(project.javaRelease());
        }
        if (moduleName_ == null) {
            moduleName_ = project.name();
        }
        return this;
    }

    /**
     * Returns the JARs contained in a given directory.
     * <p>
     * Sources and Javadoc JARs are ignored.
     * <p>
     * Package-private to allow direct unit testing without subclassing.
     *
     * @param directory the directory
     * @param regex     the regular expression to match
     * @return the Java Archives
     */
    static List<File> getJarList(@NonNull File directory, @NonNull String regex) {
        var jars = new ArrayList<File>();

        if (directory.isDirectory()) {
            var files = directory.listFiles();
            if (files != null) {
                for (var f : files) {
                    if (!f.getName().endsWith("-sources.jar")
                            && !f.getName().endsWith("-javadoc.jar")
                            && !f.getName().contains("-test")
                            && !f.getName().contains("-kjs")
                            && f.getName().matches(regex)) {
                        jars.add(f);
                    }
                }
                jars.sort(Comparator.comparing(File::getName));
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
     * @throws NullPointerException     if {@code url} or {@code packageListUrl} are {@code null}
     * @throws IllegalArgumentException if {@code url} or {@code packageListUrl} are empty
     */
    public DokkaOperation globalLinks(@NonNull String url, @NonNull String packageListUrl) {
        ObjectTools.requireNotEmpty(url, "globalLinks url");
        ObjectTools.requireNotEmpty(packageListUrl, "globalLinks packageListUrl");
        globalLinks_.put(url, packageListUrl);
        return this;
    }

    /**
     * Set the global external documentation links.
     *
     * @param globalLinks the map of global links
     * @return this operation instance
     * @throws NullPointerException     if {@code globalLinks} is {@code null}
     * @throws IllegalArgumentException If {@code globalLinks} is empty
     * @see #globalSrcLink(String...) #globalSrcLink(String...)#globalSrcLink(String...)
     */
    public DokkaOperation globalLinks(@NonNull Map<String, String> globalLinks) {
        ObjectTools.requireNotEmpty(globalLinks, "globalLinks");
        globalLinks_.putAll(globalLinks);
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
     * @throws NullPointerException     if {@code options} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code options} is empty or contains empty elements
     */
    public DokkaOperation globalPackageOptions(@NonNull String... options) {
        ObjectTools.requireNotEmpty(options, "globalPackageOptions");
        globalPackageOptions_.addAll(List.of(options));
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
     * @throws NullPointerException     if {@code options} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code options} is empty or contains empty elements
     */
    public final DokkaOperation globalPackageOptions(@NonNull Collection<String> options) {
        ObjectTools.requireNotEmpty(options, "globalPackageOptions");
        globalPackageOptions_.addAll(options);
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
     * @throws NullPointerException     if {@code links} is null
     * @throws IllegalArgumentException if {@code links} is empty or contains {@code null} or empty elements
     */
    public DokkaOperation globalSrcLink(@NonNull String... links) {
        ObjectTools.requireNotEmpty(links, "globalSrcLink");
        globalSrcLinks_.addAll(List.of(links));
        return this;
    }

    /**
     * Sets the global mapping between a source directory and a Web service for browsing the code.
     *
     * @param links the links mapping
     * @return this operation instance
     * @throws NullPointerException     if {@code links} is null
     * @throws IllegalArgumentException if {@code links} is empty or contains {@code null} or empty elements
     */
    public final DokkaOperation globalSrcLink(@NonNull Collection<String> links) {
        ObjectTools.requireNotEmpty(links, "globalSrcLink");
        globalSrcLinks_.addAll(links);
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
     * @throws NullPointerException     if {@code includes} is {@code null}
     * @throws IllegalArgumentException If {@code includes} is empty
     * @see #includes(Collection)
     */
    public DokkaOperation includes(@NonNull File... files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(List.of(files));
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
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includes(File...)
     */
    public final DokkaOperation includes(@NonNull Collection<File> files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(files);
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
     * @throws NullPointerException     if {@code files} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code files} is empty or contains empty elements
     * @see #includesStrings(Collection)
     */
    public DokkaOperation includes(@NonNull String... files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
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
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includesPaths(Collection)
     */
    public DokkaOperation includes(@NonNull Path... files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
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
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includes(Path...)
     */
    public final DokkaOperation includesPaths(@NonNull Collection<Path> files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
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
     * @throws NullPointerException     if {@code files} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code files} is empty or contains empty elements
     * @see #includes(String...)
     */
    public final DokkaOperation includesStrings(@NonNull Collection<String> files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * JSON configuration file path.
     *
     * @param configuration the configuration file path
     * @return this operation instance
     * @throws NullPointerException if {@code configuration} is {@code null}
     */
    public DokkaOperation json(@NonNull Path configuration) {
        ObjectTools.requireNonNull(configuration, "json");
        json_ = configuration.toFile();
        return this;
    }

    /**
     * JSON configuration file path.
     *
     * @param configuration the configuration file path
     * @return this operation instance
     * @throws NullPointerException if {@code configuration} is {@code null}
     */
    public DokkaOperation json(@NonNull File configuration) {
        json_ = ObjectTools.requireNonNull(configuration, "json");
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
     * @return this operation instance
     * @throws NullPointerException     if {@code configuration} is {@code null}
     * @throws IllegalArgumentException if {@code configuration} is empty
     */
    public DokkaOperation json(@NonNull String configuration) {
        ObjectTools.requireNotEmpty(configuration, "json");
        json_ = new File(configuration);
        return this;
    }

    /**
     * Sets the logging level.
     *
     * @param loggingLevel the logging level
     * @return this operation instance
     * @throws NullPointerException if {@code loggingLevel} is {@code null}
     */
    public DokkaOperation loggingLevel(@NonNull LoggingLevel loggingLevel) {
        loggingLevel_ = ObjectTools.requireNonNull(loggingLevel, "loggingLevel");
        return this;
    }

    /**
     * Sets the name of the project/module. Default is {@code root}.
     * <p>
     * The display name used to refer to the module. It is used for the table of contents, navigation, logging, etc.
     *
     * @param moduleName the project/module name
     * @return this operation instance
     * @throws NullPointerException     if {@code moduleName} is {@code null}
     * @throws IllegalArgumentException if {@code moduleName} is empty
     */
    public DokkaOperation moduleName(@NonNull String moduleName) {
        moduleName_ = ObjectTools.requireNotEmpty(moduleName, "moduleName");
        return this;
    }

    /**
     * Set the documented version.
     *
     * @param version the version
     * @return this operation instance
     * @throws NullPointerException     if {@code version} is {@code null}
     * @throws IllegalArgumentException if {@code version} is empty
     */
    public DokkaOperation moduleVersion(@NonNull String version) {
        moduleVersion_ = ObjectTools.requireNotEmpty(version, "moduleVersion");
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
     * @throws NullPointerException     if {@code outputDir} is {@code null}
     * @throws IllegalArgumentException if {@code outputDir} is empty
     */
    public DokkaOperation outputDir(@NonNull String outputDir) {
        ObjectTools.requireNotEmpty(outputDir, "outputDir");
        outputDir_ = new File(outputDir);
        return this;
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     * <p>
     * The directory to where documentation is generated, regardless of output format.
     *
     * @param outputDir the output directory
     * @return this operation instance
     * @throws NullPointerException if {@code outputDir} is {@code null}
     */
    public DokkaOperation outputDir(@NonNull File outputDir) {
        outputDir_ = ObjectTools.requireNonNull(outputDir, "outputDir");
        return this;
    }

    /**
     * Sets the output directory path, {@code ./dokka} by default.
     * <p>
     * The directory to where documentation is generated, regardless of output format.
     *
     * @param outputDir the output directory
     * @return this operation instance
     * @throws NullPointerException if {@code outputDir} is {@code null}
     */
    public DokkaOperation outputDir(@NonNull Path outputDir) {
        ObjectTools.requireNonNull(outputDir, "outputDir");
        outputDir_ = outputDir.toFile();
        return this;
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
     * @throws NullPointerException if {@code format} is {@code null}
     */
    public DokkaOperation outputFormat(@NonNull OutputFormat format) {
        ObjectTools.requireNonNull(format, "outputFormat");
        outputFormat_ = format;
        return this;
    }

    /**
     * Sets the configuration for Dokka plugins.
     *
     * @param name              The fully qualified plugin name
     * @param jsonConfiguration The plugin JSON configuration
     * @return this operation instance
     * @throws NullPointerException     if {@code name} or {@code jsonConfiguration} are {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code jsonConfiguration} are empty
     */
    public DokkaOperation pluginConfigurations(@NonNull String name, @NonNull String jsonConfiguration) {
        ObjectTools.requireNotEmpty(name, "pluginConfigurations name");
        ObjectTools.requireNotEmpty(jsonConfiguration, "pluginConfigurations jsonConfiguration");
        pluginsConfiguration_.put(name, jsonConfiguration);
        return this;
    }

    /**
     * Sets the configuration for Dokka plugins.
     *
     * @param pluginConfigurations the map of configurations
     * @return this operation instance
     * @throws NullPointerException     if {@code pluginConfigurations} is {@code null}
     * @throws IllegalArgumentException If {@code pluginConfigurations} is empty
     * @see #pluginConfigurations(String, String)
     */
    public DokkaOperation pluginConfigurations(@NonNull Map<String, String> pluginConfigurations) {
        ObjectTools.requireNonNull(pluginConfigurations, "pluginConfigurations");
        pluginsConfiguration_.putAll(pluginConfigurations);
        return this;
    }

    /**
     * Retrieves the plugin configurations.
     *
     * @return the plugin configurations.
     */
    public Map<String, String> pluginConfigurations() {
        return pluginsConfiguration_;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null}
     * @throws IllegalArgumentException If {@code jars} is empty
     * @see #pluginsClasspath(Collection)
     */
    public DokkaOperation pluginsClasspath(@NonNull File... jars) {
        ObjectTools.requireNotEmpty(jars, PLUGINS_CLASSPATH);
        pluginsClasspath_.addAll(List.of(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars the jars
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null}
     * @throws IllegalArgumentException If {@code jars} is empty
     * @see #pluginsClasspath(Collection)
     */
    public final DokkaOperation pluginsClasspath(@NonNull Collection<File> jars) {
        ObjectTools.requireNotEmpty(jars, PLUGINS_CLASSPATH);
        pluginsClasspath_.addAll(jars);
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty or contains empty elements
     * @see #pluginsClasspathStrings(Collection)
     */
    public DokkaOperation pluginsClasspath(@NonNull String... jars) {
        ObjectTools.requireNotEmpty(jars, PLUGINS_CLASSPATH);
        pluginsClasspath_.addAll(CollectionTools.combineStringsToFiles(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars one or more jars
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty or contains empty elements
     * @see #pluginsClasspathPaths(Collection)
     */
    public DokkaOperation pluginsClasspath(@NonNull Path... jars) {
        ObjectTools.requireNotEmpty(jars, PLUGINS_CLASSPATH);
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
     * @throws NullPointerException     if {@code jars} is {@code null}
     * @throws IllegalArgumentException If {@code jars} is empty
     * @see #pluginsClasspath(Path...)
     */
    public final DokkaOperation pluginsClasspathPaths(@NonNull Collection<Path> jars) {
        ObjectTools.requireNotEmpty(jars, "pluginsClasspathPaths");
        pluginsClasspath_.addAll(CollectionTools.combinePathsToFiles(jars));
        return this;
    }

    /**
     * Sets the jars for Dokka plugins and their dependencies.
     *
     * @param jars the jars
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null}
     * @throws IllegalArgumentException If {@code jars} is empty
     * @see #pluginsClasspath(String...)
     */
    public final DokkaOperation pluginsClasspathStrings(@NonNull Collection<String> jars) {
        ObjectTools.requireNotEmpty(jars, "pluginsClasspathStrings");
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
     * @throws NullPointerException if {@code sourceSet} is {@code null}
     */
    public DokkaOperation sourceSet(@NonNull SourceSet sourceSet) {
        sourceSet_ = ObjectTools.requireNonNull(sourceSet, "sourceSet");
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