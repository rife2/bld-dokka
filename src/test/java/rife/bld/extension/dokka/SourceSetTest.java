/*
 * Copyright 2023-2025 the original author or authors.
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

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static rife.bld.extension.TestUtils.localPath;

class SourceSetTest {
    private static final String CLASSPATH_1 = "classpath1";
    private static final String CLASSPATH_2 = "classpath2";
    private static final String INCLUDES_1 = "includes1";
    private static final String INCLUDES_2 = "includes2";
    private static final String INCLUDES_3 = "includes3";
    private static final String INCLUDES_4 = "includes4";
    private static final String OPTION_1 = "option1";
    private static final String OPTION_2 = "option2";
    private static final String PATH_1 = "path1";
    private static final String PATH_2 = "path2";
    private static final String PATH_3 = "path3";
    private static final String SAMPLES_1 = "samples1";
    private static final String SAMPLES_2 = "samples2";
    private static final String SAMPLES_3 = "samples3";
    private static final String SRC_1 = "src1";
    private static final String SRC_2 = "src2";
    private static final String SRC_3 = "src3";
    private static final String SRC_4 = "src4";
    private static final String SUP_1 = "sup1";
    private static final String SUP_2 = "sup2";
    private static final String SUP_3 = "sup3";

    @ParameterizedTest
    @ValueSource(strings = {"1.8", "19", "22"})
    void jdkVersion(String version) {
        var args = new SourceSet().jdkVersion(version);
        assertThat(args.jdkVersion()).isEqualTo(version);
    }

    @Test
    void sourceSet() throws IOException {
        var args = Files.readAllLines(Paths.get("src", "test", "resources", "dokka-sourceset-args.txt"));

        assertThat(args).isNotEmpty();

        var sourceSet = new SourceSet()
                .analysisPlatform(AnalysisPlatform.JVM)
                .apiVersion("1.0")
                .classpath(CLASSPATH_1)
                .classpath(new File(CLASSPATH_2))
                .dependentSourceSets("moduleName", "sourceSetName")
                .dependentSourceSets("moduleName2", "sourceSetName2")
                .displayName("name")
                .documentedVisibilities(DocumentedVisibility.PACKAGE, DocumentedVisibility.PRIVATE)
                .externalDocumentationLinks("url1", "packageListUrl1")
                .externalDocumentationLinks("url2", "packageListUrl2")
                .includes(INCLUDES_1, INCLUDES_2)
                .includes(new File(INCLUDES_3))
                .includes(List.of(new File(INCLUDES_4)))
                .jdkVersion(18)
                .languageVersion("2.0")
                .noJdkLink(true)
                .noSkipEmptyPackages(true)
                .noStdlibLink(true)
                .perPackageOptions(OPTION_1, OPTION_2)
                .reportUndocumented(true)
                .samples(SAMPLES_1, SAMPLES_2)
                .skipDeprecated(true)
                .sourceSetName("setName")
                .src(SRC_1, SRC_2)
                .src(new File(SRC_3))
                .src(List.of(new File(SRC_4)))
                .srcLink(PATH_1, "remote1", "#suffix1")
                .srcLink(new File(PATH_2), "remote2", "#suffix2")
                .srcLink(Path.of(PATH_3), "remote3", "#suffix3")
                .suppressedFiles(SUP_1, SUP_2);

        try (var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(sourceSet.classpath()).as("classpath").hasSize(2);
            softly.assertThat(sourceSet.dependentSourceSets()).as("dependentSourceSets").hasSize(2);
            softly.assertThat(sourceSet.documentedVisibilities()).as("documentedVisibilities").hasSize(2);
            softly.assertThat(sourceSet.externalDocumentationLinks()).as("externalDocumentationLinks").hasSize(2);
            softly.assertThat(sourceSet.includes()).as("includes").hasSize(4);
            softly.assertThat(sourceSet.perPackageOptions()).as("perPackageOptions").hasSize(2);
            softly.assertThat(sourceSet.samples()).as("samples").hasSize(2);
            softly.assertThat(sourceSet.src()).as("src").hasSize(4);
            softly.assertThat(sourceSet.srcLinks()).as("srcLinks").hasSize(3);
            softly.assertThat(sourceSet.suppressedFiles()).as("suppressedFiles").hasSize(2);
        }

        var params = sourceSet.args();

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

        var matches = List.of(
                "-analysisPlatform", "jvm",
                "-apiVersion", "1.0",
                "-classpath", localPath(CLASSPATH_1, CLASSPATH_2),
                "-dependentSourceSets", "moduleName/sourceSetName;moduleName2/sourceSetName2",
                "-displayName", "name",
                "-documentedVisibilities", "package;private",
                "-externalDocumentationLinks", "url1^packageListUrl1^^url2^packageListUrl2",
                "-jdkVersion", "18",
                "-includes", localPath(INCLUDES_1, INCLUDES_2, INCLUDES_3, INCLUDES_4),
                "-languageVersion", "2.0",
                "-noJdkLink",
                "-noSkipEmptyPackages",
                "-noStdlibLink",
                "-reportUndocumented",
                "-perPackageOptions", OPTION_1 + ';' + OPTION_2,
                "-samples", localPath(SAMPLES_1, SAMPLES_2),
                "-skipDeprecated",
                "-src", localPath(SRC_1, SRC_2, SRC_3, SRC_4),
                "-srcLink", localPath(PATH_2) + "=remote2#suffix2;" + localPath(PATH_3) + "=remote3#suffix3;" +
                        "path1=remote1#suffix1",
                "-sourceSetName", "setName",

                "-suppressedFiles", localPath(SUP_1, SUP_2));

        assertThat(params).hasSize(matches.size());

        IntStream.range(0, params.size()).forEach(i -> assertThat(params.get(i)).isEqualTo(matches.get(i)));

        sourceSet.classpath(List.of(new File(CLASSPATH_1), new File(CLASSPATH_2)));

        IntStream.range(0, params.size()).forEach(i -> assertThat(params.get(i)).isEqualTo(matches.get(i)));
    }

    @Test
    void sourceSetCollections() {
        var args = new SourceSet()
                .classpath(List.of(new File(PATH_1), new File(PATH_2)))
                .dependentSourceSets(Map.of("set1", "set2", "set3", "set4"))
                .externalDocumentationLinks(Map.of("link1", "link2", "link3", "link4"))
                .perPackageOptions(List.of(OPTION_1, OPTION_2))
                .samples(List.of(new File(SAMPLES_1)))
                .samples(new File(SAMPLES_2))
                .samples(SAMPLES_3)
                .suppressedFiles(List.of(new File(SUP_1)))
                .suppressedFiles(new File(SUP_2))
                .suppressedFiles(SUP_3)
                .args();

        var matches = List.of(
                "-classpath", localPath(PATH_1, PATH_2),
                "-dependentSourceSets", "set1/set2;set3/set4",
                "-externalDocumentationLinks", "link1^link2^^link3^link4",
                "-perPackageOptions", OPTION_1 + ';' + OPTION_2,
                "-samples", localPath(SAMPLES_1, SAMPLES_2, SAMPLES_3),
                "-suppressedFiles", localPath(SUP_1, SUP_2, SUP_3)
        );

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));
    }

    @Test
    void sourceSetIntVersions() {
        var args = new SourceSet().apiVersion(1).languageVersion(2);
        assertThat(args.args()).containsExactly("-apiVersion", "1", "-languageVersion", "2");
    }

    @Nested
    @DisplayName("Classpath Tests")
    class ClasspathTests {
        @Test
        void classpathAsFileArray() {
            var args = new SourceSet();
            args.classpath(new File(CLASSPATH_1), new File(CLASSPATH_2));
            assertThat(args.classpath()).containsExactly(new File(CLASSPATH_1), new File(CLASSPATH_2));
        }

        @Test
        void classpathAsFileList() {
            var args = new SourceSet();
            args.classpath(List.of(new File(CLASSPATH_1), new File(CLASSPATH_2)));
            assertThat(args.classpath()).containsExactly(new File(CLASSPATH_1), new File(CLASSPATH_2));
        }

        @Test
        void classpathAsPathArray() {
            var args = new SourceSet();
            args = args.classpath(Path.of(CLASSPATH_1), Path.of(CLASSPATH_2));
            assertThat(args.classpath()).containsExactly(new File(CLASSPATH_1), new File(CLASSPATH_2));
        }

        @Test
        void classpathAsPathList() {
            var args = new SourceSet();
            args.classpathPaths(List.of(new File(CLASSPATH_1).toPath(), new File(CLASSPATH_2).toPath()));
            assertThat(args.classpath()).containsExactly(new File(CLASSPATH_1), new File(CLASSPATH_2));
        }

        @Test
        void classpathAsStringArray() {
            var args = new SourceSet();
            args.classpath(CLASSPATH_1, CLASSPATH_2);
            assertThat(args.classpath()).containsExactly(new File(CLASSPATH_1), new File(CLASSPATH_2));
        }

        @Test
        void classpathAsStringList() {
            var args = new SourceSet();
            args.classpathStrings(List.of(CLASSPATH_1, CLASSPATH_2));
            assertThat(args.classpath()).containsExactly(new File(CLASSPATH_1), new File(CLASSPATH_2));
        }
    }

    @Nested
    @DisplayName("Includes Tests")
    class IncludesTests {
        @Test
        void includesAsFileArray() {
            var args = new SourceSet();
            args.includes(new File(INCLUDES_1), new File(INCLUDES_2));
            assertThat(args.includes()).containsExactly(new File(INCLUDES_1), new File(INCLUDES_2));
        }

        @Test
        void includesAsFileList() {
            var args = new SourceSet();
            args.includes(List.of(new File(INCLUDES_1), new File(INCLUDES_2)));
            assertThat(args.includes()).containsExactly(new File(INCLUDES_1), new File(INCLUDES_2));
        }

        @Test
        void includesAsPathArray() {
            var args = new SourceSet();
            args = args.includes(Path.of(INCLUDES_1), Path.of(INCLUDES_2));
            assertThat(args.includes()).containsExactly(new File(INCLUDES_1), new File(INCLUDES_2));
        }

        @Test
        void includesAsPathList() {
            var args = new SourceSet();
            args.includesPaths(List.of(new File(INCLUDES_1).toPath(), new File(INCLUDES_2).toPath()));
            assertThat(args.includes()).containsExactly(new File(INCLUDES_1), new File(INCLUDES_2));
        }

        @Test
        void includesAsStringArray() {
            var args = new SourceSet();
            args.includes(INCLUDES_1, INCLUDES_2);
            assertThat(args.includes()).containsExactly(new File(INCLUDES_1), new File(INCLUDES_2));
        }

        @Test
        void includesAsStringList() {
            var args = new SourceSet();
            args.includesStrings(List.of(INCLUDES_1, INCLUDES_2));
            assertThat(args.includes()).containsExactly(new File(INCLUDES_1), new File(INCLUDES_2));
        }
    }

    @Nested
    @DisplayName("Samples Tests")
    class SamplesTests {
        @Test
        void samplesAsFileArray() {
            var args = new SourceSet();
            args.samples(new File(SAMPLES_1), new File(SAMPLES_2));
            assertThat(args.samples()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void samplesAsFileList() {
            var args = new SourceSet();
            args.samples(List.of(new File(SAMPLES_1), new File(SAMPLES_2)));
            assertThat(args.samples()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void samplesAsPathArray() {
            var args = new SourceSet();
            args = args.samples(Path.of(SAMPLES_1), Path.of(SAMPLES_2));
            assertThat(args.samples()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void samplesAsPathList() {
            var args = new SourceSet();
            args.samplesPaths(List.of(new File(SAMPLES_1).toPath(), new File(SAMPLES_2).toPath()));
            assertThat(args.samples()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void samplesAsStringArray() {
            var args = new SourceSet();
            args.samples(SAMPLES_1, SAMPLES_2);
            assertThat(args.samples()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void samplesAsStringList() {
            var args = new SourceSet();
            args.samplesStrings(List.of(SAMPLES_1, SAMPLES_2));
            assertThat(args.samples()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }
    }

    @Nested
    @DisplayName("Source Tests")
    class SrcTests {
        private static final String main = "src/main";
        private static final String src = "src";
        private static final String test = "src/test";
        private final File mainFile = new File(main);
        private final File srcFile = new File(src);
        private final File testFile = new File(test);

        @Test
        void srcAsFileArray() {
            var args = new SourceSet();
            args.src(srcFile, mainFile);
            assertThat(args.src()).containsExactly(srcFile, mainFile);
        }

        @Test
        void srcAsFileList() {
            var args = new SourceSet();
            args.src(List.of(srcFile, mainFile));
            assertThat(args.src()).containsExactly(srcFile, mainFile);
        }

        @Test
        void srcAsPathArray() {
            var args = new SourceSet();
            args = args.src(srcFile.toPath(), mainFile.toPath());
            assertThat(args.src()).containsExactly(srcFile, mainFile);
        }

        @Test
        void srcAsPathList() {
            var args = new SourceSet();
            args.srcPaths(List.of(srcFile.toPath(), testFile.toPath()));
            assertThat(args.src()).containsExactly(srcFile, testFile);
        }

        @Test
        void srcAsStringArray() {
            var args = new SourceSet();
            args.src(src, main);
            assertThat(args.src()).containsExactly(srcFile, mainFile);
        }

        @Test
        void srcAsStringList() {
            var args = new SourceSet().srcStrings(List.of(src, main));
            assertThat(args.src()).containsExactly(srcFile, mainFile);
        }
    }

    @Nested
    @DisplayName("Suppressed Files Tests")
    class SuppressedFilesTests {
        @Test
        void suppressedFilesAsFileArray() {
            var args = new SourceSet();
            args.suppressedFiles(new File(SAMPLES_1), new File(SAMPLES_2));
            assertThat(args.suppressedFiles()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void suppressedFilesAsFileList() {
            var args = new SourceSet();
            args.suppressedFiles(List.of(new File(SAMPLES_1), new File(SAMPLES_2)));
            assertThat(args.suppressedFiles()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void suppressedFilesAsPathArray() {
            var args = new SourceSet();
            args = args.suppressedFiles(Path.of(SAMPLES_1), Path.of(SAMPLES_2));
            assertThat(args.suppressedFiles()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void suppressedFilesAsPathList() {
            var args = new SourceSet();
            args.suppressedFilesPaths(List.of(new File(SAMPLES_1).toPath(), new File(SAMPLES_2).toPath()));
            assertThat(args.suppressedFiles()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void suppressedFilesAsStringArray() {
            var args = new SourceSet();
            args.suppressedFiles(SAMPLES_1, SAMPLES_2);
            assertThat(args.suppressedFiles()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }

        @Test
        void suppressedFilesAsStringList() {
            var args = new SourceSet();
            args.suppressedFilesStrings(List.of(SAMPLES_1, SAMPLES_2));
            assertThat(args.suppressedFiles()).containsExactly(new File(SAMPLES_1), new File(SAMPLES_2));
        }
    }
}
