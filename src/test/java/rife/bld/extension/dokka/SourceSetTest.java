/*
 * Copyright 2023-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    @Test
    void sourceSetCollectionsTest() {
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
                "-externalDocumentationLinks", "link3^link4^^link1^link2",
                "-perPackageOptions", OPTION_1 + ';' + OPTION_2,
                "-samples", localPath(SAMPLES_1, SAMPLES_2, SAMPLES_3),
                "-suppressedFiles", localPath(SUP_1, SUP_2, SUP_3)
        );

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));
    }

    @Test
    void sourceSetIntVersionsTest() {
        var args = new SourceSet().apiVersion(1).languageVersion(2);
        assertThat(args.args()).containsExactly("-apiVersion", "1", "-languageVersion", "2");
    }

    @Test
    void sourceSetTest() throws IOException {
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
                .suppressedFiles(SUP_1, SUP_2);

        assertThat(sourceSet.classpath()).as("classpath").hasSize(2);
        assertThat(sourceSet.dependentSourceSets()).as("dependentSourceSets").hasSize(2);
        assertThat(sourceSet.documentedVisibilities()).as("documentedVisibilities").hasSize(2);
        assertThat(sourceSet.externalDocumentationLinks()).as("externalDocumentationLinks").hasSize(2);
        assertThat(sourceSet.includes()).as("includes").hasSize(4);
        assertThat(sourceSet.perPackageOptions()).as("perPackageOptions").hasSize(2);
        assertThat(sourceSet.samples()).as("samples").hasSize(2);
        assertThat(sourceSet.src()).as("src").hasSize(4);
        assertThat(sourceSet.srcLinks()).as("srcLinks").hasSize(2);
        assertThat(sourceSet.suppressedFiles()).as("suppressedFiles").hasSize(2);

        var params = sourceSet.args();

        for (var p : args) {
            var found = false;
            for (var a : params) {
                if (a.startsWith(p)) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as(p + " not found.").isTrue();
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
                "-srcLink", localPath(PATH_2) + "=remote2#suffix2;path1=remote1#suffix1",
                "-sourceSetName", "setName",

                "-suppressedFiles", localPath(SUP_1, SUP_2));

        assertThat(params).hasSize(matches.size());

        IntStream.range(0, params.size()).forEach(i -> assertThat(params.get(i)).isEqualTo(matches.get(i)));

        sourceSet.classpath(List.of(new File(CLASSPATH_1), new File(CLASSPATH_2)));

        IntStream.range(0, params.size()).forEach(i -> assertThat(params.get(i)).isEqualTo(matches.get(i)));
    }
}
