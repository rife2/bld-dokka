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

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.extension.dokka.LoggingLevel;
import rife.bld.extension.dokka.OutputFormat;
import rife.bld.extension.dokka.SourceSet;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.bld.testing.LoggingExtension;
import rife.bld.testing.TestLogHandler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@ExtendWith(LoggingExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DokkaOperationTest {

    private static final File EXAMPLES = new File("examples");
    private static final String FILE_1 = "file1";
    private static final String FILE_2 = "file2";
    private static final String FILE_3 = "file3";
    private static final String FILE_4 = "file4";
    private static final String OPTION_1 = "option1";
    private static final String OPTION_2 = "option2";
    private static final String OPTION_3 = "option3";
    private static final String OPTION_4 = "option4";
    private static final String PATH_1 = "path1";
    private static final String PATH_2 = "path2";
    private static final String PATH_3 = "path3";
    private static final String PATH_4 = "path4";
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private static final Logger logger = Logger.getLogger(DokkaOperation.class.getName());
    private static final TestLogHandler testLogHandler = new TestLogHandler();

    @RegisterExtension
    @SuppressWarnings("unused")
    private static final LoggingExtension loggingExtension = new LoggingExtension(
            logger,
            testLogHandler
    );

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        private static void deleteRecursively(Path dir) throws IOException {
            if (!Files.exists(dir)) {
                return;
            }
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        }

        private static boolean isNotEmpty(Path dir) throws IOException {
            if (!Files.isDirectory(dir)) {
                return false;
            }
            try (var stream = Files.list(dir)) {
                return stream.findAny().isPresent();
            }
        }

        @Test
        void execute() {
            testLogHandler.setLevel(Level.FINE);
            var op = new DokkaOperation()
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "examples",
                            "Examples"))
                    .outputDir("build/javadoc")
                    .outputFormat(OutputFormat.JAVADOC);
            assertThatCode(op::execute).doesNotThrowAnyException();
            testLogHandler.printLogMessages();
        }

        @Test
        @EnabledOnOs(OS.LINUX)
        void executeConstructProcessCommandListTest() throws IOException {
            var args = Files.readAllLines(Paths.get("src", "test", "resources", "dokka-args.txt"));
            assertThat(args).as("args should not be empty").isNotEmpty();

            var jsonConf = new File("config.json").getAbsolutePath();
            var op = new DokkaOperation()
                    .delayTemplateSubstitution(true)
                    .failOnWarning(true)
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "example", "Example"))
                    .globalLinks("s", "gLink1")
                    .globalLinks(Map.of("s2", "gLink2"))
                    .globalPackageOptions(OPTION_1, OPTION_2)
                    .globalPackageOptions(List.of(OPTION_3, OPTION_4))
                    .globalSrcLink("link1", "link2")
                    .globalSrcLink(List.of("link3", "link4"))
                    .globalSuppressAnnotatedWith("foo.bar", "foo.baz")
                    .globalSuppressAnnotatedWith(List.of("bar.foo", "baz.foo"))
                    .includes(new File(FILE_1))
                    .includes(FILE_2)
                    .includes(List.of(new File(FILE_3), new File(FILE_4)))
                    .json(jsonConf)
                    .loggingLevel(LoggingLevel.DEBUG)
                    .moduleName("name")
                    .moduleVersion("1.0")
                    .noSuppressObviousFunctions(true)
                    .offlineMode(true)
                    .outputDir(new File(EXAMPLES, "build"))
                    .outputFormat(OutputFormat.JAVADOC)
                    .pluginConfigurations("org.jetbrains.dokka.android.AndroidDocumentationPlugin",
                            "{\"android\":{\"packageOptions\":[{\"prefix\":\"com.myapp.internal\"," +
                                    "\"suppress\":true}]}}")
                    .pluginConfigurations("org.jetbrains.dokka.base.DokkaBase",
                            "{\"separateInheritedMembers\":false," +
                                    "\"mergeImplicitExpectActualDeclarations\":true}")
                    .pluginsClasspath(new File(PATH_1))
                    .pluginsClasspath(PATH_2)
                    .pluginsClasspath(List.of(new File(PATH_3), new File(PATH_4)))
                    .sourceSet(new SourceSet().classpath(
                            List.of(
                                    new File("examples/foo.jar"),
                                    new File("examples/bar.jar")
                            )))
                    .suppressInheritedMembers(true);

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(op.globalLinks()).as("globalLinks").hasSize(2);
                softly.assertThat(op.globalPackageOptions()).as("globalPackageOptions").hasSize(4);
                softly.assertThat(op.globalSrcLink()).as("globalSrcLink").hasSize(4);
                softly.assertThat(op.globalSuppressAnnotatedWith()).as("globalSuppressAnnotatedWith").hasSize(4);
                softly.assertThat(op.includes()).as("includes").hasSize(4);
                softly.assertThat(op.pluginConfigurations()).as("pluginConfigurations").hasSize(2);
                softly.assertThat(op.pluginsClasspath()).as("pluginsClasspath").hasSize(4);
            }

            var params = op.executeConstructProcessCommandList();

            try (var softly = new AutoCloseableSoftAssertions()) {
                for (var p : args) {
                    var found = false;
                    for (var a : params) {
                        if (a.startsWith(p)) {
                            found = true;
                            break;
                        }
                    }
                    softly.assertThat(found).as("%s not found.", p).isTrue();
                }
            }

            var path = EXAMPLES.getAbsolutePath();
            var dokkaJar = "2.3.0-Beta.jar";
            var matches = List.of("java",
                    "-cp", path + "/lib/bld/dokka-cli-" + dokkaJar,
                    "org.jetbrains.dokka.MainKt",
                    "-pluginsClasspath", TestUtils.localPath(PATH_1, PATH_2, PATH_3, PATH_4),
                    "-sourceSet", "-classpath " + path + "/foo.jar;" + path + "/bar.jar",
                    "-outputDir", path + "/build",
                    "-delayTemplateSubstitution",
                    "-failOnWarning",
                    "-globalLinks", "s^gLink1^^s2^gLink2",
                    "-globalPackageOptions", OPTION_1 + ';' + OPTION_2 + ';' + OPTION_3 + ';' + OPTION_4,
                    "-globalSrcLinks", "link1;link2;link3;link4",
                    "-globalSuppressAnnotatedWith", "foo.bar;foo.baz;bar.foo;baz.foo",
                    "-includes", TestUtils.localPath(FILE_1, FILE_2, FILE_3, FILE_4),
                    "-loggingLevel", "debug",
                    "-moduleName", "name",
                    "-moduleVersion", "1.0",
                    "-noSuppressObviousFunctions",
                    "-offlineMode",
                    "-pluginsConfiguration", "org.jetbrains.dokka.android.AndroidDocumentationPlugin={\"android\":" +
                            "{\"packageOptions\":[{\"prefix\":\"com.myapp.internal\",\"suppress\":true}]}}" +
                            "^^org.jetbrains.dokka.base.DokkaBase={\"separateInheritedMembers\":false," +
                            "\"mergeImplicitExpectActualDeclarations\":true}",
                    "-suppressInheritedMembers",
                    jsonConf);

            assertThat(params).as("params should match").hasSize(matches.size());

            try (var softly = new AutoCloseableSoftAssertions()) {
                IntStream.range(0, params.size()).forEach(i -> {
                    var actual = params.get(i);
                    var expected = matches.get(i);

                    if (actual.contains(".jar") || expected.contains(".jar")) {
                        // split by both separators : and ;
                        for (var token : expected.split("[;:]")) {
                            var t = token.trim();
                            if (t.isEmpty() || "-classpath".equals(t) || "-src".equals(t)) {
                                continue;
                            }
                            // token like "-classpath /path/foo.jar"
                            if (t.contains(" ")) {
                                t = t.substring(t.lastIndexOf(' ') + 1).trim();
                            }
                            softly.assertThat(actual)
                                    .as("param[%d] '%s' should contain '%s'", i, actual, t)
                                    .contains(t);
                        }
                    } else {
                        softly.assertThat(actual).as("param[%d]", i).isEqualTo(expected);
                    }
                });
            }
        }

        @Test
        void executeNoOutputFormat() {
            var op = new DokkaOperation()
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "examples",
                            "Examples"))
                    .outputDir("build/javadoc");
            assertThatThrownBy(op::execute).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("outputFormat");
        }

        @Test
        void executeNoProject() {
            var op = new DokkaOperation();
            assertThatThrownBy(op::execute).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("project");
        }

        @Test
        void executeOutputJavadocFormat() throws IOException, ExitStatusException, InterruptedException {
            var outputDir = Path.of("examples/build/javadoc");

            deleteRecursively(outputDir);
            assertThat(outputDir).doesNotExist();

            var op = new DokkaOperation()
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "examples",
                            "Examples"))
                    .outputDir(outputDir.toString())
                    .outputFormat(OutputFormat.JAVADOC);

            op.execute();

            assertThat(isNotEmpty(outputDir)).isTrue();
            assertThat(new File(outputDir.toFile(), "index.html")).exists();
        }

        @Test
        void executeOutputHtmlFormat() throws IOException, ExitStatusException, InterruptedException {
            var outputDir = Path.of("examples/build/dokka/html");

            deleteRecursively(outputDir);
            assertThat(outputDir).doesNotExist();

            var op = new DokkaOperation()
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "examples",
                            "Examples"))
                    .outputDir(outputDir.toString())
                    .outputFormat(OutputFormat.HTML);

            op.execute();

            assertThat(isNotEmpty(outputDir)).isTrue();
            assertThat(new File(outputDir.toFile(), "index.html")).exists();
        }

        @Test
        void executeOutputJekyllFormat() throws IOException, ExitStatusException, InterruptedException {
            var outputDir = Path.of("examples/build/dokka/jekyll");

            deleteRecursively(outputDir);
            assertThat(outputDir).doesNotExist();

            var op = new DokkaOperation()
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "examples",
                            "Examples"))
                    .outputDir(outputDir.toString())
                    .outputFormat(OutputFormat.JEKYLL);

            op.execute();

            assertThat(isNotEmpty(outputDir)).isTrue();
            assertThat(new File(outputDir.toFile(), "index.md")).exists();
        }

        @Test
        void executeOutputMarkdownFormat() throws IOException, ExitStatusException, InterruptedException {
            var outputDir = Path.of("examples/build/dokka/gfm");

            deleteRecursively(outputDir);
            assertThat(outputDir).doesNotExist();

            var op = new DokkaOperation()
                    .fromProject(new BaseProjectBlueprint(EXAMPLES, "com.example", "examples",
                            "Examples"))
                    .outputDir(outputDir.toString())
                    .outputFormat(OutputFormat.MARKDOWN);

            op.execute();

            assertThat(isNotEmpty(outputDir)).isTrue();
            assertThat(new File(outputDir.toFile(), "index.md")).exists();
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Nested
        @DisplayName("Includes Tests")
        class IncludesTests {

            private final DokkaOperation op = new DokkaOperation();

            @Test
            void includesAsFileArray() {
                op.includes().clear();
                op.includes(new File(FILE_1), new File(FILE_2));
                assertThat(op.includes()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void includesAsFileList() {
                op.includes().clear();
                op.includes(List.of(new File(FILE_1), new File(FILE_2)));
                assertThat(op.includes()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void includesAsPathArray() {
                var op = new DokkaOperation();
                op = op.includes(Path.of(FILE_1), Path.of(FILE_2));
                assertThat(op.includes()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void includesAsPathList() {
                op.includes().clear();
                op.includesPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
                assertThat(op.includes()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void includesAsStringArray() {
                op.includes().clear();
                op.includes(FILE_1, FILE_2);
                assertThat(op.includes()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void includesAsStringList() {
                op.includes().clear();
                op.includesStrings(List.of(FILE_1, FILE_2));
                assertThat(op.includes()).containsExactly(new File(FILE_1), new File(FILE_2));
            }
        }

        @Nested
        @DisplayName("JSON Tests")
        class JsonTests {

            private final DokkaOperation op = new DokkaOperation();

            @Test
            void jsonAsFile() {
                var newOp = op.json(new File(FILE_3));
                assertThat(newOp.json()).isEqualTo(new File(FILE_3));
            }

            @Test
            void jsonAsPath() {
                var op = new DokkaOperation();
                op = op.json(Path.of(FILE_2));
                assertThat(op.json()).isEqualTo(new File(FILE_2));
            }

            @Test
            void jsonAsString() {
                op.json(FILE_1);
                assertThat(op.json()).isEqualTo(new File(FILE_1));
            }
        }

        @Nested
        @DisplayName("Output Dir Tests")
        class OutputDirTests {

            private final DokkaOperation op = new DokkaOperation();

            @Test
            void outputDirAsFile() {
                op.outputDir(new File(FILE_1));
                assertThat(op.outputDir()).isEqualTo(new File(FILE_1));
            }

            @Test
            void outputDirAsPath() {
                var op = new DokkaOperation();
                op = op.outputDir(Path.of(FILE_2));
                assertThat(op.outputDir()).isEqualTo(new File(FILE_2));
            }

            @Test
            void outputDirAsString() {
                op.outputDir(FILE_3);
                assertThat(op.outputDir()).isEqualTo(new File(FILE_3));
            }
        }

        @Nested
        @DisplayName("Output Format Tests")
        class OutputFormatTests {

            @Test
            void outputFormat() {
                var op = new DokkaOperation();
                op.outputFormat(OutputFormat.JAVADOC);
                assertThat(op.outputFormat()).isEqualTo(OutputFormat.JAVADOC);
            }

            @Test
            @SuppressWarnings("DataFlowIssue")
            void outputFormatNull() {
                var op = new DokkaOperation();
                assertThatThrownBy(() -> op.outputFormat(null)).isInstanceOf(NullPointerException.class);
            }

        }

        @Nested
        @DisplayName("Plugin Classpath Tests")
        class PluginClasspathTests {

            private final DokkaOperation op = new DokkaOperation();

            @Test
            void pluginClasspathAsFileArray() {
                op.pluginsClasspath().clear();
                op.pluginsClasspath(new File(FILE_1), new File(FILE_2));
                assertThat(op.pluginsClasspath()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void pluginClasspathAsFileList() {
                op.pluginsClasspath().clear();
                op.pluginsClasspath(List.of(new File(FILE_1), new File(FILE_2)));
                assertThat(op.pluginsClasspath()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void pluginClasspathAsPathArray() {
                var op = new DokkaOperation();
                op = op.pluginsClasspath(Path.of(FILE_1), Path.of(FILE_2));
                assertThat(op.pluginsClasspath()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void pluginClasspathAsPathList() {
                op.pluginsClasspath().clear();
                op.pluginsClasspathPaths(List.of(new File(FILE_1).toPath(), new File(FILE_2).toPath()));
                assertThat(op.pluginsClasspath()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void pluginClasspathAsStringArray() {
                op.pluginsClasspath().clear();
                op.pluginsClasspath(FILE_1, FILE_2);
                assertThat(op.pluginsClasspath()).containsExactly(new File(FILE_1), new File(FILE_2));
            }

            @Test
            void pluginClasspathAsStringList() {
                op.pluginsClasspath().clear();
                op.pluginsClasspathStrings(List.of(FILE_1, FILE_2));
                assertThat(op.pluginsClasspath()).containsExactly(new File(FILE_1), new File(FILE_2));
            }
        }
    }

    @Nested
    @DisplayName("Plugin Configurations Tests")
    class PluginConfigurationsTests {

        private static final String JSON_BASE = "{\"separateInheritedMembers\":false}";
        private static final String JSON_GFM = "{\"gfm\":{\"hardLineBreaks\":true}}";
        private static final String PLUGIN_BASE = "org.jetbrains.dokka.base.DokkaBase";
        private static final String PLUGIN_GFM = "org.jetbrains.dokka.gfm.GfmPlugin";

        @Test
        void pluginConfigurationsEmptyJson() {
            var op = new DokkaOperation();
            assertThatThrownBy(() -> op.pluginConfigurations(PLUGIN_BASE, ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void pluginConfigurationsEmptyPluginId() {
            var op = new DokkaOperation();
            assertThatThrownBy(() -> op.pluginConfigurations("", JSON_BASE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void pluginConfigurationsMultiplePreservesOrder() {
            var op = new DokkaOperation();
            op.pluginConfigurations(PLUGIN_BASE, JSON_BASE)
                    .pluginConfigurations(PLUGIN_GFM, JSON_GFM);

            assertThat(op.pluginConfigurations().keySet())
                    .containsExactly(PLUGIN_BASE, PLUGIN_GFM);
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void pluginConfigurationsNullJson() {
            var op = new DokkaOperation();
            assertThatThrownBy(() -> op.pluginConfigurations(PLUGIN_BASE, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void pluginConfigurationsNullPluginId() {
            var op = new DokkaOperation();
            assertThatThrownBy(() -> op.pluginConfigurations(null, JSON_BASE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void pluginConfigurationsOverwritesSamePlugin() {
            var op = new DokkaOperation();
            op.pluginConfigurations(PLUGIN_BASE, JSON_BASE)
                    .pluginConfigurations(PLUGIN_BASE, JSON_GFM);

            assertThat(op.pluginConfigurations())
                    .containsExactly(entry(PLUGIN_BASE, JSON_GFM));
        }

        @Test
        void pluginConfigurationsSingle() {
            var op = new DokkaOperation();
            op.pluginConfigurations(PLUGIN_BASE, JSON_BASE);
            assertThat(op.pluginConfigurations())
                    .containsExactly(entry(PLUGIN_BASE, JSON_BASE));
        }

        @Nested
        @DisplayName("Plugin Configurations Map Overload Tests")
        class PluginConfigurationsMapTests {

            private static final String JSON_BASE = "{\"separateInheritedMembers\":false}";
            private static final String JSON_GFM = "{\"gfm\":{\"hardLineBreaks\":true}}";
            private static final String PLUGIN_BASE = "org.jetbrains.dokka.base.DokkaBase";
            private static final String PLUGIN_GFM = "org.jetbrains.dokka.gfm.GfmPlugin";

            @Test
            void pluginConfigurationsMapAddsAll() {
                var op = new DokkaOperation();
                var configs = new LinkedHashMap<String, String>();
                configs.put(PLUGIN_BASE, JSON_BASE);
                configs.put(PLUGIN_GFM, JSON_GFM);

                op.pluginConfigurations(configs);

                assertThat(op.pluginConfigurations())
                        .containsExactly(
                                entry(PLUGIN_BASE, JSON_BASE),
                                entry(PLUGIN_GFM, JSON_GFM)
                        );
            }

            @Test
            void pluginConfigurationsMapEmptyMapNotAllowed() {
                var op = new DokkaOperation();
                assertThatThrownBy(() ->
                        op.pluginConfigurations(Collections.emptyMap())).isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            void pluginConfigurationsMapMergesWithExisting() {
                var op = new DokkaOperation();
                op.pluginConfigurations(PLUGIN_BASE, JSON_BASE);

                var moreConfigs = Map.of(PLUGIN_GFM, JSON_GFM);
                op.pluginConfigurations(moreConfigs);

                assertThat(op.pluginConfigurations())
                        .hasSize(2)
                        .containsEntry(PLUGIN_BASE, JSON_BASE)
                        .containsEntry(PLUGIN_GFM, JSON_GFM);
            }

            @Test
            @SuppressWarnings("DataFlowIssue")
            void pluginConfigurationsMapNullThrows() {
                var op = new DokkaOperation();
                assertThatThrownBy(() -> op.pluginConfigurations(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("pluginConfigurations");
            }

            @Test
            void pluginConfigurationsMapOverwritesExistingKeys() {
                var op = new DokkaOperation();
                op.pluginConfigurations(PLUGIN_BASE, JSON_BASE);

                var newConfigs = Map.of(PLUGIN_BASE, JSON_GFM);
                op.pluginConfigurations(newConfigs);

                assertThat(op.pluginConfigurations())
                        .containsExactly(entry(PLUGIN_BASE, JSON_GFM));
            }

            @Test
            void pluginConfigurationsMapPreservesOrder() {
                var configs = new LinkedHashMap<String, String>();
                configs.put(PLUGIN_GFM, JSON_GFM);  // insert GFM first
                configs.put(PLUGIN_BASE, JSON_BASE);

                var op = new DokkaOperation().pluginConfigurations(configs);

                assertThat(op.pluginConfigurations().keySet())
                        .containsExactly(PLUGIN_GFM, PLUGIN_BASE); // LinkedHashMap preserves it
            }

            @Test
            void pluginConfigurationsMapWithEmptyKeyNotAllowed() {
                var op = new DokkaOperation();
                var configs = Map.of("", JSON_BASE); // empty string key

                assertThatThrownBy(() -> op.pluginConfigurations(configs)).isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            void pluginConfigurationsMapWithEmptyValuesNotAllowed() {
                var op = new DokkaOperation();
                var configs = Map.of(PLUGIN_BASE, ""); // empty string value

                assertThatThrownBy(() -> op.pluginConfigurations(configs)).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("DokkaOperation Validation Tests")
    @SuppressWarnings({"DataFlowIssue", "RedundantCast"})
    class ValidationTests {

        @Test
        void executeRequiresProject() {
            assertThatThrownBy(() -> new DokkaOperation()
                    .outputFormat(OutputFormat.HTML)
                    .sourceSet(new SourceSet())
                    .execute())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("project");
        }

        @Test
        void fromProjectWithNull() {
            assertThatThrownBy(() -> new DokkaOperation().fromProject(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void globalLinksWithNullOrEmpty(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().globalLinks(arg, "pkg"))
                    .isInstanceOf(arg == null ? NullPointerException.class : IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalLinks("url", arg))
                    .isInstanceOf(arg == null ? NullPointerException.class : IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalLinks((Map<String, String>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalLinks(Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void globalPackageOptionsWithNullOrEmpty() {
            assertThatThrownBy(() -> new DokkaOperation().globalPackageOptions((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalPackageOptions((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalPackageOptions(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalPackageOptions("opt", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalPackageOptions("opt", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void globalSrcLinkWithNullOrEmpty() {
            assertThatThrownBy(() -> new DokkaOperation().globalSrcLink((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalSrcLink((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalSrcLink(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalSrcLink("link", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().globalSrcLink("link", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @EmptySource
        void includesWithEmpty(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().includes(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().includesStrings(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().includes(new String[0]))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().includesStrings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void includesWithNull(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().includes(arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includesStrings(List.of("foo", arg)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includes((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includesStrings((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includes((Path[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includesPaths((Collection<Path>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includes((File[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().includes((Collection<File>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void jsonWithNullOrEmpty() {
            assertThatThrownBy(() -> new DokkaOperation().json((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().json((Path) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().json((String) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().json(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void loggingLevelWithNull() {
            assertThatThrownBy(() -> new DokkaOperation().loggingLevel(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void moduleNameWithNullOrEmpty(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().moduleName(arg))
                    .isInstanceOf(arg == null ? NullPointerException.class : IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void moduleVersionWithNullOrEmpty(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().moduleVersion(arg))
                    .isInstanceOf(arg == null ? NullPointerException.class : IllegalArgumentException.class);
        }

        @Test
        void outputDirWithNullOrEmpty() {
            assertThatThrownBy(() -> new DokkaOperation().outputDir((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().outputDir((Path) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().outputDir((String) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().outputDir(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void outputFormatWithNull() {
            assertThatThrownBy(() -> new DokkaOperation().outputFormat(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void pluginConfigurationsWithNullOrEmpty() {
            assertThatThrownBy(() -> new DokkaOperation().pluginConfigurations(null, "{}"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginConfigurations("plugin", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginConfigurations("", "{}"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginConfigurations("plugin", ""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginConfigurations((Map<String, String>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void pluginsClasspathWithEmpty(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath(arg))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspathStrings(List.of("foo", arg)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath(new String[0]))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspathStrings(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void pluginsClasspathWithNull(String arg) {
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath(arg))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspathStrings(List.of("foo", arg)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath((String[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspathStrings((Collection<String>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath((Path[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspathPaths((Collection<Path>) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath((File[]) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DokkaOperation().pluginsClasspath((Collection<File>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void sourceSetWithNull() {
            assertThatThrownBy(() -> new DokkaOperation().sourceSet(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
