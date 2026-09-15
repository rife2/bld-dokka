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

package rife.bld.extension.dokka;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import rife.bld.extension.DokkaOperation;
import rife.bld.extension.tools.CollectionTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.extension.tools.TextTools;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration for a Dokka source set.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@NullMarked
public class SourceSet {

    private static final String INCLUDES = "SourceSet includes";
    private static final String SAMPLES = "SourceSet samples";
    private static final String SRC = "SourceSet src";
    private static final String SUPPRESSED_FILES = "SourceSet suppressedFiles";
    private final List<File> classpath_ = new ArrayList<>();
    private final Map<String, String> dependentSourceSets_ = new LinkedHashMap<>();
    private final List<DocumentedVisibility> documentedVisibilities_ = new ArrayList<>();
    private final Map<String, String> externalDocumentationLinks_ = new LinkedHashMap<>();
    private final List<File> includes_ = new ArrayList<>();
    private final List<String> perPackageOptions_ = new ArrayList<>();
    private final List<File> samples_ = new ArrayList<>();
    private final Map<String, String> srcLinks_ = new LinkedHashMap<>();
    private final List<File> src_ = new ArrayList<>();
    private final List<File> suppressedFiles_ = new ArrayList<>();
    private @Nullable AnalysisPlatform analysisPlatform_;
    private @Nullable String apiVersion_;
    private @Nullable String displayName_;
    private @Nullable String jdkVersion_;
    private @Nullable String languageVersion_;
    private boolean noJdkLink_;
    private boolean noSkipEmptyPackages_;
    private boolean noStdlibLink_;
    private boolean reportUndocumented_;
    private boolean skipDeprecated_;
    private @Nullable String sourceSetName_;

    /**
     * Normalizes a remote path and line suffix for Dokka's {@code -srcLink} option.
     * <p>
     * Ensures there is exactly one {@code #} between the remote path and line suffix.
     * For example: {@code https://.../blob/main} + {@code #L10} → {@code https://.../blob/main#L10}
     *
     * @param remotePath the remote path, e.g. {@code https://github.com/org/repo/blob/main}
     * @param lineSuffix the line suffix, e.g. {@code #L10} or {@code L10}
     * @return the normalized link
     */
    private static String normalizeSrcLink(String remotePath, String lineSuffix) {
        ObjectTools.requireNotEmpty(remotePath, "SourceSet normalize srcLink remotePath");
        ObjectTools.requireNotEmpty(lineSuffix, "SourceSet normalize srcLink lineSuffix");

        var suffix = lineSuffix;

        if (remotePath.endsWith("#") && suffix.startsWith("#")) {
            suffix = suffix.substring(1);
        } else if (!remotePath.endsWith("#") && !suffix.startsWith("#")) {
            suffix = "#" + suffix;
        }
        return remotePath + suffix;
    }

    /**
     * Sets the platform used for setting up analysis. Default is {@link AnalysisPlatform#JVM JVM}
     * <p>
     * Platform to be used for setting up code analysis and {@code @sample} environment.
     *
     * @param analysisPlatform the analysis platform
     * @return this operation instance
     * @throws NullPointerException if {@code analysisPlatform} is {@code null}
     */
    public SourceSet analysisPlatform(AnalysisPlatform analysisPlatform) {
        analysisPlatform_ = ObjectTools.requireNonNull(analysisPlatform, "SourceSet analysisPlatform");
        return this;
    }

    /**
     * Retrieves the platform used for setting up analysis.
     *
     * @return the analysis platform, or {@code null} if not set
     */
    @Nullable
    public AnalysisPlatform analysisPlatform() {
        return analysisPlatform_;
    }

    /**
     * Sets the Kotlin API version used for setting up analysis and samples.
     *
     * @param apiVersion the api version
     * @return this operation instance
     */
    public SourceSet apiVersion(int apiVersion) {
        return apiVersion(String.valueOf(apiVersion));
    }

    /**
     * Sets the Kotlin API version used for setting up analysis and samples.
     *
     * @param apiVersion the api version
     * @return this operation instance
     * @throws NullPointerException     if {@code apiVersion} is {@code null}
     * @throws IllegalArgumentException if {@code apiVersion} is empty
     */
    public SourceSet apiVersion(String apiVersion) {
        apiVersion_ = ObjectTools.requireNotEmpty(apiVersion, "SourceSet apiVersion");
        return this;
    }

    /**
     * Retrieves the Kotlin API version used for setting up analysis and samples.
     *
     * @return the API version, or {@code null} if not set
     */
    @Nullable
    public String apiVersion() {
        return apiVersion_;
    }

    /**
     * Returns the formatted arguments.
     *
     * @return the arguments
     */
    public List<String> args() {
        var args = new ArrayList<String>();

        // -analysisPlatform
        if (analysisPlatform_ != null) {
            args.add("-analysisPlatform");
            args.add(analysisPlatform_.name().toLowerCase());
        }

        // -apiVersion
        if (TextTools.isNotBlank(apiVersion_)) {
            args.add("-apiVersion");
            args.add(apiVersion_);
        }

        // -classpath
        if (!classpath_.isEmpty()) {
            args.add("-classpath");
            args.add(classpath_.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(DokkaOperation.DOKKA_LIST_SEPARATOR)));
        }

        // -dependentSourceSets
        if (!dependentSourceSets_.isEmpty()) {
            args.add("-dependentSourceSets");
            var deps = new ArrayList<String>();
            dependentSourceSets_.forEach((k, v) -> deps.add(String.format("%s/%s", k, v)));
            args.add(String.join(DokkaOperation.DOKKA_LIST_SEPARATOR, deps));
        }

        // -displayName
        if (TextTools.isNotBlank(displayName_)) {
            args.add("-displayName");
            args.add(displayName_);
        }

        // -documentedVisibilities
        if (!documentedVisibilities_.isEmpty()) {
            args.add("-documentedVisibilities");
            var vis = new ArrayList<String>();
            documentedVisibilities_.forEach(d -> vis.add(d.name().toLowerCase()));
            args.add(String.join(DokkaOperation.DOKKA_LIST_SEPARATOR, vis));
        }

        // -externalDocumentationLinks
        if (!externalDocumentationLinks_.isEmpty()) {
            args.add("-externalDocumentationLinks");
            var links = new ArrayList<String>();
            externalDocumentationLinks_.forEach((k, v) -> links.add(String.format("%s^%s", k, v)));
            args.add(String.join("^^", links));
        }

        // -jdkVersion
        if (TextTools.isNotBlank(jdkVersion_)) {
            args.add("-jdkVersion");
            args.add(jdkVersion_);
        }

        // -includes
        if (!includes_.isEmpty()) {
            args.add("-includes");
            args.add(includes_.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(DokkaOperation.DOKKA_LIST_SEPARATOR)));
        }

        // -languageVersion
        if (TextTools.isNotBlank(languageVersion_)) {
            args.add("-languageVersion");
            args.add(languageVersion_);
        }

        // -noJdkLink
        if (noJdkLink_) {
            args.add("-noJdkLink");
        }

        // -noSkipEmptyPackages
        if (noSkipEmptyPackages_) {
            args.add("-noSkipEmptyPackages");
        }

        // -noStdlibLink
        if (noStdlibLink_) {
            args.add("-noStdlibLink");
        }

        // -reportUndocumented
        if (reportUndocumented_) {
            args.add("-reportUndocumented");
        }

        // -perPackageOptions
        if (!perPackageOptions_.isEmpty()) {
            args.add("-perPackageOptions");
            args.add(String.join(DokkaOperation.DOKKA_LIST_SEPARATOR, perPackageOptions_));
        }

        // -samples
        if (!samples_.isEmpty()) {
            args.add("-samples");
            args.add(samples_.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(DokkaOperation.DOKKA_LIST_SEPARATOR)));
        }

        // -skipDeprecated
        if (skipDeprecated_) {
            args.add("-skipDeprecated");
        }

        // -src
        if (!src_.isEmpty()) {
            args.add("-src");
            args.add(src_.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(DokkaOperation.DOKKA_LIST_SEPARATOR)));
        }

        // -srcLink
        if (!srcLinks_.isEmpty()) {
            args.add("-srcLink");
            var links = new ArrayList<String>();
            srcLinks_.forEach((k, v) -> links.add(String.format("%s=%s", k, v)));
            args.add(String.join(DokkaOperation.DOKKA_LIST_SEPARATOR, links));
        }

        // -sourceSetName
        if (TextTools.isNotBlank(sourceSetName_)) {
            args.add("-sourceSetName");
            args.add(sourceSetName_);
        }

        // -suppressedFiles
        if (!suppressedFiles_.isEmpty()) {
            args.add("-suppressedFiles");
            args.add(suppressedFiles_.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(DokkaOperation.DOKKA_LIST_SEPARATOR)));
        }

        return args;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param files one or more file
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #classpath(Collection)
     */
    public SourceSet classpath(File... files) {
        ObjectTools.requireNotEmpty(files, "SourceSet classpath files");
        classpath_.addAll(List.of(files));
        return this;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param files the collection of files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #classpath(File...)
     */
    public final SourceSet classpath(Collection<File> files) {
        ObjectTools.requireNotEmpty(files, "SourceSet classpath");
        classpath_.addAll(files);
        return this;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param files one or more file
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code files} is empty or contains empty elements
     * @see #classpathStrings(Collection)
     */
    public SourceSet classpath(String... files) {
        ObjectTools.requireNotEmpty(files, "SourceSet classpath");
        classpath_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param files one or more file
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #classpathPaths(Collection)
     */
    public SourceSet classpath(Path... files) {
        ObjectTools.requireNotEmpty(files, "SourceSet classpath");
        classpath_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Retrieves the classpath for analysis and interactive samples.
     *
     * @return the classpath
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> classpath() {
        return classpath_;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param files the collection of files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #classpath(Path...)
     */
    public final SourceSet classpathPaths(Collection<Path> files) {
        ObjectTools.requireNotEmpty(files, "SourceSet classpathPaths");
        classpath_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Sets classpath for analysis and interactive samples.
     * <p>
     * This is useful if some types that come from dependencies are not resolved/picked up automatically.
     * <p>
     * This option accepts both {@code .jar} and {@code .klib} files.
     *
     * @param files the collection of files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code files} is empty or contains empty elements
     * @see #classpath(String...)
     */
    public final SourceSet classpathStrings(Collection<String> files) {
        ObjectTools.requireNotEmpty(files, "SourceSet classpathStrings");
        classpath_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Sets the names of dependent source sets.
     *
     * @param moduleName    the module name
     * @param sourceSetName the source set name
     * @return this operation instance
     * @throws NullPointerException     if {@code moduleName} or {@code sourceSetName} is {@code null}
     * @throws IllegalArgumentException if {@code moduleName} or {@code sourceSetName} is empty
     */
    public SourceSet dependentSourceSets(String moduleName, String sourceSetName) {
        ObjectTools.requireNotEmpty(moduleName, "SourceSet dependent moduleName");
        ObjectTools.requireNotEmpty(sourceSetName, "SourceSet dependent sourceSetName");
        dependentSourceSets_.put(moduleName, sourceSetName);
        return this;
    }

    /**
     * Retrieves the names of dependent source sets.
     *
     * @return the names
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> dependentSourceSets() {
        return dependentSourceSets_;
    }

    /**
     * Sets the names of dependent source sets.
     *
     * @param dependentSourceSets the map of dependent source set names
     * @return this operation instance
     * @throws NullPointerException     if {@code dependentSourceSets} is {@code null}
     * @throws IllegalArgumentException If {@code dependentSourceSets} is empty
     * @see #dependentSourceSets(String, String)
     */
    public SourceSet dependentSourceSets(Map<String, String> dependentSourceSets) {
        ObjectTools.requireNotEmpty(dependentSourceSets, "SourceSet dependentSourceSets");
        dependentSourceSets_.putAll(dependentSourceSets);
        return this;
    }

    /**
     * Sets the display name of the source set, used both internally and externally.
     * <p>
     * The name is used both externally (for example, the source set name is visible to documentation readers) and
     * internally (for example, for logging messages of {@link #reportUndocumented reportUndocumented}).
     * <p>
     * The platform name can be used if you don't have a better alternative.
     *
     * @param displayName the display name
     * @return this operation instance
     * @throws NullPointerException     if {@code displayName} is {@code null}
     * @throws IllegalArgumentException if {@code displayName} is empty
     */
    public SourceSet displayName(String displayName) {
        displayName_ = ObjectTools.requireNotEmpty(displayName, "SourceSet displayName");
        return this;
    }

    /**
     * Retrieves the display name of the source set.
     *
     * @return the display name, or {@code null} if not set
     */
    @Nullable
    public String displayName() {
        return displayName_;
    }

    /**
     * Sets visibilities to be documented.
     * <p>
     * This can be used if you want to document protected/internal/private declarations, as well as if you want to
     * exclude public declarations and only document internal API.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param visibilities one or more visibilities
     * @return this operation instance
     * @throws NullPointerException     if {@code visibilities} is {@code null}
     * @throws IllegalArgumentException If {@code visibilities} is empty
     */
    public SourceSet documentedVisibilities(DocumentedVisibility... visibilities) {
        ObjectTools.requireNotEmpty(visibilities, "SourceSet documentedVisibilities");
        documentedVisibilities_.addAll(List.of(visibilities));
        return this;
    }

    /**
     * Retrieves the visibilities to be documented.
     *
     * @return the documented visibilities
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<DocumentedVisibility> documentedVisibilities() {
        return documentedVisibilities_;
    }

    /**
     * Sets the external documentation links.
     * <p>
     * A set of parameters for external documentation links that is applied only for this source set.
     *
     * @param url            the external documentation URL
     * @param packageListUrl the external documentation package list URL
     * @return this operation instance
     * @throws NullPointerException     if {@code url} or {@code packageListUrl} is {@code null}
     * @throws IllegalArgumentException if {@code url} or {@code packageListUrl} is empty
     */
    public SourceSet externalDocumentationLinks(String url, String packageListUrl) {
        ObjectTools.requireNotEmpty(url, "SourceSet external documentation url");
        ObjectTools.requireNotEmpty(packageListUrl, "SourceSet external documentation packageListUrl");
        externalDocumentationLinks_.put(url, packageListUrl);
        return this;
    }

    /**
     * Retrieves the external documentation links.
     *
     * @return the documentation links.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> externalDocumentationLinks() {
        return externalDocumentationLinks_;
    }

    /**
     * Sets the external documentation links.
     * <p>
     * A set of parameters for external documentation links that is applied only for this source set.
     *
     * @param externalDocumentationLinks the map of external documentation links
     * @return this operation instance
     * @throws NullPointerException     if {@code externalDocumentationLinks} is {@code null}
     * @throws IllegalArgumentException If {@code externalDocumentationLinks} is empty
     * @see #externalDocumentationLinks(String, String)
     */
    public SourceSet externalDocumentationLinks(Map<String, String> externalDocumentationLinks) {
        ObjectTools.requireNotEmpty(externalDocumentationLinks, "SourceSet externalDocumentationLinks");
        externalDocumentationLinks_.putAll(externalDocumentationLinks);
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files one or more files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includes(Collection)
     */
    public SourceSet includes(File... files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(List.of(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files the collection of files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includes(File...)
     */
    public final SourceSet includes(Collection<File> files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(files);
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files one or more files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code files} is empty or contains empty elements
     * @see #includesStrings(Collection)
     */
    public SourceSet includes(String... files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files one or more files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includesPaths(Collection)
     */
    public SourceSet includes(Path... files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Retrieves the Markdown files that contain module and package documentation.
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
     * The Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files the collection of files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null}
     * @throws IllegalArgumentException If {@code files} is empty
     * @see #includes(Path...)
     */
    public final SourceSet includesPaths(Collection<Path> files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(CollectionTools.combinePathsToFiles(files));
        return this;
    }

    /**
     * Sets the Markdown files that contain module and package documentation.
     * <p>
     * The Markdown files that contain module and package documentation.
     * <p>
     * The contents of the specified files are parsed and embedded into documentation as module and package
     * descriptions.
     *
     * @param files the collection of files
     * @return this operation instance
     * @throws NullPointerException     if {@code files} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code files} is empty or contains empty elements
     * @see #includes(String...)
     */
    public final SourceSet includesStrings(Collection<String> files) {
        ObjectTools.requireNotEmpty(files, INCLUDES);
        includes_.addAll(CollectionTools.combineStringsToFiles(files));
        return this;
    }

    /**
     * Retrieves the version of the JDK to use for linking to JDK Javadocs.
     *
     * @return the JDK version.
     */
    @Nullable
    public String jdkVersion() {
        return jdkVersion_;
    }

    /**
     * Sets the version of JDK to use for linking to JDK Javadocs.
     * <p>
     * The JDK version to use when generating external documentation links for Java types.
     * <p>
     * For example, if you use {@link java.util.UUID} in some public declaration signature, and this option is set to 8,
     * Dokka generates an external documentation link to JDK 8 Javadocs for it.
     *
     * @param jdkVersion the JDK version
     * @return this operation instance
     */
    public SourceSet jdkVersion(int jdkVersion) {
        return jdkVersion(String.valueOf(jdkVersion));
    }

    /**
     * Sets the version of JDK to use for linking to JDK Javadocs.
     * <p>
     * The JDK version to use when generating external documentation links for Java types.
     * <p>
     * For example, if you use {@link java.util.UUID} in some public declaration signature, and this option is set to 8,
     * Dokka generates an external documentation link to JDK 8 Javadocs for it.
     *
     * @param jdkVersion the JDK version
     * @return this operation instance
     * @throws NullPointerException     if {@code jdkVersion} is {@code null}
     * @throws IllegalArgumentException if {@code jdkVersion} is empty
     */
    public SourceSet jdkVersion(String jdkVersion) {
        jdkVersion_ = ObjectTools.requireNotEmpty(jdkVersion, "SourceSet jdkVersion");
        return this;
    }

    /**
     * Sets the language version used for setting up analysis and samples.
     *
     * @param languageVersion the language version
     * @return this operation instance
     */
    public SourceSet languageVersion(int languageVersion) {
        return languageVersion(String.valueOf(languageVersion));
    }

    /**
     * Sets the language version used for setting up analysis and samples.
     *
     * @param languageVersion the language version
     * @return this operation instance
     * @throws NullPointerException     if {@code languageVersion} is {@code null}
     * @throws IllegalArgumentException if {@code languageVersion} is empty
     */
    public SourceSet languageVersion(String languageVersion) {
        languageVersion_ = ObjectTools.requireNotEmpty(languageVersion, "SourceSet languageVersion");
        return this;
    }

    /**
     * Retrieves the language version used for setting up analysis and samples.
     *
     * @return the language version, or {@code null} if not set
     */
    @Nullable
    public String languageVersion() {
        return languageVersion_;
    }

    /**
     * Sets whether to generate links to JDK Javadocs.
     * <p>
     * Whether to generate external documentation links to JDK's Javadocs.
     * <p>
     * The version of JDK Javadocs is determined by the {@link #jdkVersion jdkVersion} option.
     * <p>
     * Note: Links are generated when noJdkLink is set to false.
     *
     * @param noJdkLink {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet noJdkLink(boolean noJdkLink) {
        noJdkLink_ = noJdkLink;
        return this;
    }

    /**
     * Retrieves whether to generate links to JDK Javadocs.
     *
     * @return {@code true} if JDK links are disabled, {@code false} otherwise
     */
    public boolean noJdkLink() {
        return noJdkLink_;
    }

    /**
     * Sets whether to create pages for empty packages.
     * <p>
     * Whether to skip packages that contain no visible declarations after various filters have been applied.
     *
     * @param noSkipEmptyPackages {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet noSkipEmptyPackages(boolean noSkipEmptyPackages) {
        noSkipEmptyPackages_ = noSkipEmptyPackages;
        return this;
    }

    /**
     * Retrieves whether to create pages for empty packages.
     *
     * @return {@code true} if empty packages should not be skipped, {@code false} otherwise
     */
    public boolean noSkipEmptyPackages() {
        return noSkipEmptyPackages_;
    }

    /**
     * Sets whether to generate links to Standard library.
     * <p>
     * Whether to generate external documentation links that lead to the API reference documentation of Kotlin's
     * standard library.
     * <p>
     * Note: Links are generated when noStdLibLink is set to {@code false}.
     *
     * @param noStdlibLink {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet noStdlibLink(boolean noStdlibLink) {
        noStdlibLink_ = noStdlibLink;
        return this;
    }

    /**
     * Retrieves whether to generate links to Standard library.
     *
     * @return {@code true} if stdlib links are disabled, {@code false} otherwise
     */
    public boolean noStdlibLink() {
        return noStdlibLink_;
    }

    /**
     * Retrieves the package source set configuration.
     *
     * @return the package source set configuration
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<String> perPackageOptions() {
        return perPackageOptions_;
    }

    /**
     * Set the package source set configuration.
     * <p>
     * A set of parameters specific to matched packages within this source set.
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
     * @param perPackageOptions the per package options
     * @return this operation instance
     * @throws NullPointerException     if {@code perPackageOptions} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code perPackageOptions} is empty or contains empty elements
     */
    public SourceSet perPackageOptions(String... perPackageOptions) {
        ObjectTools.requireNotEmpty(perPackageOptions, "SourceSet perPackageOptions");
        perPackageOptions_.addAll(List.of(perPackageOptions));
        return this;
    }

    /**
     * Set the package source set configuration.
     * <p>
     * A set of parameters specific to matched packages within this source set.
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
     * @param perPackageOptions the per package options
     * @return this operation instance
     * @throws NullPointerException     if {@code perPackageOptions} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code perPackageOptions} is empty or contains empty elements
     */
    public final SourceSet perPackageOptions(Collection<String> perPackageOptions) {
        ObjectTools.requireNotEmpty(perPackageOptions, "SourceSet perPackageOptions");
        perPackageOptions_.addAll(perPackageOptions);
        return this;
    }

    /**
     * Sets whether to report undocumented declarations.
     * <p>
     * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs after they
     * have been filtered by documentedVisibilities and other filters.
     * <p>
     * This setting works well with {@link DokkaOperation#failOnWarning}.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param reportUndocumented {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet reportUndocumented(boolean reportUndocumented) {
        reportUndocumented_ = reportUndocumented;
        return this;
    }

    /**
     * Retrieves whether to report undocumented declarations.
     *
     * @return {@code true} if undocumented declarations should be reported, {@code false} otherwise
     */
    public boolean reportUndocumented() {
        return reportUndocumented_;
    }

    /**
     * Retrieves the directories or files that contain sample functions.
     *
     * @return the directories or files
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> samples() {
        return samples_;
    }

    /**
     * Set the directories or files that contain sample functions.
     * <p>
     * The directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples one or more samples
     * @return this operation instance
     * @throws NullPointerException     if {@code samples} is {@code null}
     * @throws IllegalArgumentException If {@code samples} is empty
     * @see #samples(Collection)
     */
    public SourceSet samples(File... samples) {
        ObjectTools.requireNotEmpty(samples, SAMPLES);
        samples_.addAll(List.of(samples));
        return this;
    }

    /**
     * Set the directories or files that contain sample functions.
     * <p>
     * The directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples the samples
     * @return this operation instance
     * @throws NullPointerException     if {@code samples} is {@code null}
     * @throws IllegalArgumentException If {@code samples} is empty
     * @see #samples(File...)
     */
    public final SourceSet samples(Collection<File> samples) {
        ObjectTools.requireNotEmpty(samples, SAMPLES);
        samples_.addAll(samples);
        return this;
    }

    /**
     * Set the directories or files that contain sample functions.
     * <p>
     * The directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples one or more samples
     * @return this operation instance
     * @throws NullPointerException     if {@code samples} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code samples} is empty or contains empty elements
     * @see #samplesStrings(Collection)
     */
    public SourceSet samples(String... samples) {
        ObjectTools.requireNotEmpty(samples, SAMPLES);
        samples_.addAll(CollectionTools.combineStringsToFiles(samples));
        return this;
    }

    /**
     * Set the directories or files that contain sample functions.
     * <p>
     * The directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples one or more samples
     * @return this operation instance
     * @throws NullPointerException     if {@code samples} is {@code null}
     * @throws IllegalArgumentException If {@code samples} is empty
     * @see #samplesPaths(Collection)
     */
    public SourceSet samples(Path... samples) {
        ObjectTools.requireNotEmpty(samples, SAMPLES);
        samples_.addAll(CollectionTools.combinePathsToFiles(samples));
        return this;
    }

    /**
     * Set the directories or files that contain sample functions.
     * <p>
     * The directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples the samples
     * @return this operation instance
     * @throws NullPointerException     if {@code samples} is {@code null}
     * @throws IllegalArgumentException If {@code samples} is empty
     * @see #samples(Path...)
     */
    public final SourceSet samplesPaths(Collection<Path> samples) {
        ObjectTools.requireNotEmpty(samples, SAMPLES);
        samples_.addAll(CollectionTools.combinePathsToFiles(samples));
        return this;
    }

    /**
     * Set the directories or files that contain sample functions.
     * <p>
     * The directories or files that contain sample functions which are referenced via the {@code @sample} KDoc
     * tag.
     *
     * @param samples the samples
     * @return this operation instance
     * @throws NullPointerException     if {@code samples} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code samples} is empty or contains empty elements
     * @see #samples(String...)
     */
    public final SourceSet samplesStrings(Collection<String> samples) {
        ObjectTools.requireNotEmpty(samples, SAMPLES);
        samples_.addAll(CollectionTools.combineStringsToFiles(samples));
        return this;
    }

    /**
     * Sets whether to skip deprecated declarations.
     * <p>
     * Whether to document declarations annotated with {@code @Deprecated}.
     * <p>
     * This can be configured on per-package basis.
     *
     * @param skipDeprecated {@code true} or {@code false}
     * @return this operation instance
     */
    public SourceSet skipDeprecated(boolean skipDeprecated) {
        skipDeprecated_ = skipDeprecated;
        return this;
    }

    /**
     * Retrieves whether to skip deprecated declarations.
     *
     * @return {@code true} if deprecated declarations should be skipped, {@code false} otherwise
     */
    public boolean skipDeprecated() {
        return skipDeprecated_;
    }

    /**
     * Sets the name of the source set. Default is {@code main}.
     *
     * @param sourceSetName the source set name.
     * @return this operation instance
     * @throws NullPointerException     if {@code sourceSetName} is {@code null}
     * @throws IllegalArgumentException if {@code sourceSetName} is empty
     */
    public SourceSet sourceSetName(String sourceSetName) {
        sourceSetName_ = ObjectTools.requireNotEmpty(sourceSetName, "SourceSet sourceSetName");
        return this;
    }

    /**
     * Retrieves the name of the source set.
     *
     * @return the source set name, or {@code null} if not set
     */
    @Nullable
    public String sourceSetName() {
        return sourceSetName_;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src one or more source code roots
     * @return this operation instance
     * @throws NullPointerException     if {@code src} is {@code null}
     * @throws IllegalArgumentException If {@code src} is empty
     * @see #src(Collection)
     */
    public SourceSet src(File... src) {
        ObjectTools.requireNotEmpty(src, SRC);
        src_.addAll(List.of(src));
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src the source code roots
     * @return this operation instance
     * @throws NullPointerException     if {@code src} is {@code null}
     * @throws IllegalArgumentException If {@code src} is empty
     * @see #src(File...)
     */
    public final SourceSet src(Collection<File> src) {
        ObjectTools.requireNotEmpty(src, SRC);
        src_.addAll(src);
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src one or more source code roots
     * @return this operation instance
     * @throws NullPointerException     if {@code src} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code src} is empty or contains empty elements
     * @see #srcStrings(Collection)
     */
    public SourceSet src(String... src) {
        ObjectTools.requireNotEmpty(src, SRC);
        src_.addAll(CollectionTools.combineStringsToFiles(src));
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src One or more source code roots
     * @return this operation instance
     * @throws NullPointerException     if {@code src} is {@code null}
     * @throws IllegalArgumentException If {@code src} is empty
     * @see #srcPaths(Collection)
     */
    public SourceSet src(Path... src) {
        ObjectTools.requireNotEmpty(src, SRC);
        src_.addAll(CollectionTools.combinePathsToFiles(src));
        return this;
    }

    /**
     * Retrieves the source code roots to be analyzed and documented.
     *
     * @return the source code roots
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> src() {
        return src_;
    }

    /**
     * Sets the mapping between a source directory and a Web service for browsing the code.
     *
     * @param srcPath    the source path
     * @param remotePath the remote path
     * @param lineSuffix the line suffix
     * @return this operation instance
     * @throws NullPointerException     if {@code srcPath}, {@code remotePath} or {@code lineSuffix} are {@code null}
     * @throws IllegalArgumentException if {@code srcPath}, {@code remotePath} or {@code lineSuffix} are empty
     */
    public SourceSet srcLink(File srcPath, String remotePath, String lineSuffix) {
        ObjectTools.requireNonNull(srcPath, "SourceSet srcLink srcPath");
        ObjectTools.requireNotEmpty(remotePath, "SourceSet srcLink remotePath");
        ObjectTools.requireNotEmpty(lineSuffix, "SourceSet srcLink lineSuffix");
        srcLinks_.put(srcPath.getAbsolutePath(), normalizeSrcLink(remotePath, lineSuffix));
        return this;
    }

    /**
     * Sets the mapping between a source directory and a Web service for browsing the code.
     *
     * @param srcPath    the source path
     * @param remotePath the remote path
     * @param lineSuffix the line suffix
     * @return this operation instance
     * @throws NullPointerException     if {@code srcPath}, {@code remotePath} or {@code lineSuffix} are {@code null}
     * @throws IllegalArgumentException if {@code srcPath}, {@code remotePath} or {@code lineSuffix} are empty
     */
    public SourceSet srcLink(String srcPath, String remotePath, String lineSuffix) {
        ObjectTools.requireNotEmpty(srcPath, "SourceSet srcLink srcPath");
        ObjectTools.requireNotEmpty(remotePath, "SourceSet srcLink remotePath");
        ObjectTools.requireNotEmpty(lineSuffix, "SourceSet srcLink lineSuffix");
        srcLinks_.put(srcPath, normalizeSrcLink(remotePath, lineSuffix));
        return this;
    }

    /**
     * Sets the mapping between a source directory and a Web service for browsing the code.
     *
     * @param srcPath    the source path
     * @param remotePath the remote path
     * @param lineSuffix the line suffix
     * @return this operation instance
     * @throws NullPointerException if {@code srcPath}, {@code remotePath} or {@code lineSuffix} are {@code null}
     */
    public SourceSet srcLink(Path srcPath, String remotePath, String lineSuffix) {
        ObjectTools.requireNonNull(srcPath, "SourceSet srcLink srcPath");
        ObjectTools.requireNotEmpty(remotePath, "SourceSet srcLink remotePath");
        ObjectTools.requireNotEmpty(lineSuffix, "SourceSet srcLink lineSuffix");
        srcLinks_.put(srcPath.toFile().getAbsolutePath(), normalizeSrcLink(remotePath, lineSuffix));
        return this;
    }

    /**
     * Retrieves the mapping between a source directory and a Web service for browsing the code.
     *
     * @return the source links
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> srcLinks() {
        return srcLinks_;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src the source code roots
     * @return this operation instance
     * @throws NullPointerException     if {@code src} is {@code null}
     * @throws IllegalArgumentException If {@code src} is empty
     * @see #src(Path...)
     */
    public final SourceSet srcPaths(Collection<Path> src) {
        ObjectTools.requireNotEmpty(src, SRC);
        src_.addAll(CollectionTools.combinePathsToFiles(src));
        return this;
    }

    /**
     * Sets the source code roots to be analyzed and documented.
     * <p>
     * The source code roots to be analyzed and documented. Acceptable inputs are directories and individual
     * {@code .kt} / {@code .java} files.
     *
     * @param src the source code roots
     * @return this operation instance
     * @throws NullPointerException     if {@code src} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code src} is empty or contains empty elements
     * @see #src(String...)
     */
    public final SourceSet srcStrings(Collection<String> src) {
        ObjectTools.requireNotEmpty(src, SRC);
        src_.addAll(CollectionTools.combineStringsToFiles(src));
        return this;
    }

    /**
     * Retrieves the paths to files to be suppressed.
     *
     * @return the paths
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> suppressedFiles() {
        return suppressedFiles_;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles one or more suppressed files
     * @return this operation instance
     * @throws NullPointerException     if {@code suppressedFiles} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code suppressedFiles} is empty or contains empty elements
     * @see #suppressedFilesStrings(Collection)
     */
    public SourceSet suppressedFiles(String... suppressedFiles) {
        ObjectTools.requireNotEmpty(suppressedFiles, SUPPRESSED_FILES);
        suppressedFiles_.addAll(CollectionTools.combineStringsToFiles(suppressedFiles));
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles the suppressed files
     * @return this operation instance
     * @throws NullPointerException     if {@code suppressedFiles} is {@code null}
     * @throws IllegalArgumentException If {@code suppressedFiles} is empty
     * @see #suppressedFiles(File...)
     */
    public final SourceSet suppressedFiles(Collection<File> suppressedFiles) {
        ObjectTools.requireNotEmpty(suppressedFiles, SUPPRESSED_FILES);
        suppressedFiles_.addAll(suppressedFiles);
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles one or more suppressed files
     * @return this operation instance
     * @throws NullPointerException     if {@code suppressedFiles} is {@code null}
     * @throws IllegalArgumentException If {@code suppressedFiles} is empty
     * @see #suppressedFiles(Collection)
     */
    public SourceSet suppressedFiles(File... suppressedFiles) {
        ObjectTools.requireNotEmpty(suppressedFiles, SUPPRESSED_FILES);
        suppressedFiles_.addAll(List.of(suppressedFiles));
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles one or more suppressed files
     * @return this operation instance
     * @throws NullPointerException     if {@code suppressedFiles} is {@code null}
     * @throws IllegalArgumentException If {@code suppressedFiles} is empty
     * @see #suppressedFilesPaths(Collection)
     */
    public SourceSet suppressedFiles(Path... suppressedFiles) {
        ObjectTools.requireNotEmpty(suppressedFiles, SUPPRESSED_FILES);
        suppressedFiles_.addAll(CollectionTools.combinePathsToFiles(suppressedFiles));
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles the suppressed files
     * @return this operation instance
     * @throws NullPointerException     if {@code suppressedFiles} is {@code null}
     * @throws IllegalArgumentException If {@code suppressedFiles} is empty
     * @see #suppressedFiles(Path...)
     */
    public final SourceSet suppressedFilesPaths(Collection<Path> suppressedFiles) {
        ObjectTools.requireNotEmpty(suppressedFiles, SUPPRESSED_FILES);
        suppressedFiles_.addAll(CollectionTools.combinePathsToFiles(suppressedFiles));
        return this;
    }

    /**
     * Sets the paths to files to be suppressed.
     * <p>
     * The files to be suppressed when generating documentation.
     *
     * @param suppressedFiles the suppressed files
     * @return this operation instance
     * @throws NullPointerException     if {@code suppressedFiles} is {@code null} or contain {@code null} elements
     * @throws IllegalArgumentException if {@code suppressedFiles} is empty or contains empty elements
     * @see #suppressedFiles(String...)
     */
    public final SourceSet suppressedFilesStrings(Collection<String> suppressedFiles) {
        ObjectTools.requireNotEmpty(suppressedFiles, SUPPRESSED_FILES);
        suppressedFiles_.addAll(CollectionTools.combineStringsToFiles(suppressedFiles));
        return this;
    }
}