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
import static rife.bld.extension.TestUtil.localPath;

class SourceSetTest {
    public static final String SAMPLES_1 = "samples1";
    public static final String SAMPLES_2 = "samples2";
    public static final String SUP_1 = "sup1";
    public static final String SUP_2 = "sup2";

    @Test
    void sourceSetCollectionsTest() {
        var args = new SourceSet()
                .classpath(List.of(new File("path1"), new File("path2")))
                .dependentSourceSets(Map.of("set1", "set2", "set3", "set4"))
                .externalDocumentationLinks(Map.of("link1", "link2", "link3", "link4"))
                .perPackageOptions(List.of("option1", "option2"))
                .samples(List.of(new File(SAMPLES_1)))
                .samples(new File(SAMPLES_2))
                .samples("samples3")
                .suppressedFiles(List.of(new File(SUP_1)))
                .suppressedFiles(new File(SUP_2))
                .suppressedFiles("sup3")
                .args();

        var matches = List.of(
                "-classpath", localPath("path1", "path2"),
                "-dependentSourceSets", "set1/set2;set3/set4",
                "-externalDocumentationLinks", "link3^link4^^link1^link2",
                "-perPackageOptions", "option1;option2",
                "-samples", localPath(SAMPLES_1, SAMPLES_2, "samples3"),
                "-suppressedFiles", localPath(SUP_1, SUP_2, "sup3")
        );

        assertThat(args).hasSize(matches.size());

        IntStream.range(0, args.size()).forEach(i -> assertThat(args.get(i)).isEqualTo(matches.get(i)));
    }

    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    void sourceSetTest() throws IOException {
        var args = Files.readAllLines(Paths.get("src", "test", "resources", "dokka-sourceset-args.txt"));

        assertThat(args).isNotEmpty();

        var sourceSet = new SourceSet()
                .analysisPlatform(AnalysisPlatform.JVM)
                .apiVersion("1.0")
                .classpath("classpath1")
                .classpath(new File("classpath2"))
                .dependentSourceSets("moduleName", "sourceSetName")
                .dependentSourceSets("moduleName2", "sourceSetName2")
                .displayName("name")
                .documentedVisibilities(DocumentedVisibility.PACKAGE, DocumentedVisibility.PRIVATE)
                .externalDocumentationLinks("url1", "packageListUrl1")
                .externalDocumentationLinks("url2", "packageListUrl2")
                .includes("includes1", "includes2")
                .includes(new File("includes3"))
                .includes(List.of(new File("includes4")))
                .jdkVersion(18)
                .languageVersion("2.0")
                .noJdkLink(true)
                .noSkipEmptyPackages(true)
                .noStdlibLink(true)
                .perPackageOptions("options1", "options2")
                .reportUndocumented(true)
                .samples(SAMPLES_1, SAMPLES_2)
                .skipDeprecated(true)
                .sourceSetName("setName")
                .src("src1", "src2")
                .src(new File("src3"))
                .src(List.of(new File("src4")))
                .srcLink("path1", "remote1", "#suffix1")
                .srcLink(new File("path2"), "remote2", "#suffix2")
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
                "-classpath", localPath("classpath1", "classpath2"),
                "-dependentSourceSets", "moduleName/sourceSetName;moduleName2/sourceSetName2",
                "-displayName", "name",
                "-documentedVisibilities", "package;private",
                "-externalDocumentationLinks", "url1^packageListUrl1^^url2^packageListUrl2",
                "-jdkVersion", "18",
                "-includes", localPath("includes1", "includes2", "includes3", "includes4"),
                "-languageVersion", "2.0",
                "-noJdkLink", "true",
                "-noSkipEmptyPackages", "true",
                "-noStdlibLink", "true",
                "-reportUndocumented", "true",
                "-perPackageOptions", "options1;options2",
                "-samples", localPath(SAMPLES_1, SAMPLES_2),
                "-skipDeprecated", "true",
                "-src", localPath("src1", "src2", "src3", "src4"),
                "-srcLink", localPath("path2") + "=remote2#suffix2;path1=remote1#suffix1",
                "-sourceSetName", "setName",
                "-suppressedFiles", localPath(SUP_1, SUP_2));

        assertThat(params).hasSize(matches.size());

        IntStream.range(0, params.size()).forEach(i -> assertThat(params.get(i)).isEqualTo(matches.get(i)));

        sourceSet.classpath(List.of(new File("classpath1"), new File("classpath2")));

        IntStream.range(0, params.size()).forEach(i -> assertThat(params.get(i)).isEqualTo(matches.get(i)));
    }
}
