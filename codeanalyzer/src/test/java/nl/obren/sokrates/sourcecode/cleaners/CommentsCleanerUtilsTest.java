/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommentsCleanerUtilsTest {
    @Test
    public void testCommentInString() {
        String sample = "\n" +
                "    /**\n" +
                "     * Gets all partition paths assuming date partitioning (year, month, day) three levels down.\n" +
                "     */\n" +
                "    public static List<String> getAllFoldersThreeLevelsDown(FileSystem fs, String basePath) throws IOException {\n" +
                "        List<String> datePartitions = new ArrayList<>();\n" +
                "        FileStatus[] folders = fs.globStatus(new Path(basePath + \"/*/*/*\"));\n" +
                "        for (FileStatus status : folders) {\n" +
                "            Path path = status.getPath();\n" +
                "            datePartitions.add(String.format(\"%s/%s/%s\", path.getParent().getParent().getName(),\n" +
                "                    path.getParent().getName(), path.getName()));\n" +
                "        }\n" +
                "        return datePartitions;\n" +
                "    }\n\n";
        String content = CommentsCleanerUtils.cleanBlockComments(sample, "/*", "*/");
        assertEquals(content, "\n" +
                "    \n" +
                "\n" +
                "\n" +
                "    public static List<String> getAllFoldersThreeLevelsDown(FileSystem fs, String basePath) throws IOException {\n" +
                "        List<String> datePartitions = new ArrayList<>();\n" +
                "        FileStatus[] folders = fs.globStatus(new Path(basePath + \"/*/*/*\"));\n" +
                "        for (FileStatus status : folders) {\n" +
                "            Path path = status.getPath();\n" +
                "            datePartitions.add(String.format(\"%s/%s/%s\", path.getParent().getParent().getName(),\n" +
                "                    path.getParent().getName(), path.getName()));\n" +
                "        }\n" +
                "        return datePartitions;\n" +
                "    }\n\n");
    }
    @Test
    public void cleanBlockCommentsInMultipleLinesComplexExample() throws Exception {
        String sample = "/*\n" +
                " * Copyright 2012 Netflix, Inc.\n" +
                " *\n" +
                " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                " * you may not use this file except in compliance with the License.\n" +
                " * You may obtain a copy of the License at\n" +
                " *\n" +
                " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
                " *\n" +
                " * Unless required by applicable law or agreed to in writing, software\n" +
                " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                " * See the License for the specific language governing permissions and\n" +
                " * limitations under the License.\n" +
                " */\n" +
                "package com.netflix.asgard\n" +
                "\n" +
                "import com.amazonaws.services.autoscaling.model.AutoScalingGroup\n" +
                "import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction\n" +
                "import com.netflix.grails.contextParam.ContextParam\n" +
                "import grails.converters.JSON\n" +
                "import grails.converters.XML\n" +
                "\n" +
                "@ContextParam('region')\n" +
                "class ScheduledActionController {\n" +
                "\n" +
                "    def awsAutoScalingService\n" +
                "    Caches caches\n" +
                "\n" +
                "    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']";
        String content = CommentsCleanerUtils.cleanBlockComments(sample, "/*", "*/");
        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("package .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[{]", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[}]", content);
        assertEquals(content, "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "@ContextParam('region')\n" +
                "class ScheduledActionController {\n" +
                "\n" +
                "def awsAutoScalingService\n" +
                "Caches caches\n" +
                "\n" +
                "def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']\n");
    }

    @Test
    public void cleanBlockCommentsInMultipleLines() throws Exception {
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block comment */}", "/*", "*/"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block\ncomment */}", "/*", "*/"), "class A {\n}");
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block\ncomment\nwith\nseveral\nlines */}", "/*", "*/"), "class A {\n\n\n\n}");
    }

    @Test
    public void cleanBlockComments() throws Exception {
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {}", "/*", "*/"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block comment */}", "/*", "*/"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block comment *//* one more */}", "/*", "*/"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block comment */ /* one more */}", "/*", "*/"), "class A { }");
        assertEquals(CommentsCleanerUtils.cleanBlockComments("class A {/* block comment */\n/* one more */}", "/*", "*/"), "class A {\n}");
    }

    @Test
    public void cleanLineComments1() throws Exception {
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {}", "//"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {} // line comment", "//"), "class A {} ");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { // line comment\n}", "//"), "class A { \n}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { // line comment\n} // line comment", "//"), "class A { \n} ");

        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {}", "//"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {} // line comment", "//"), "class A {} ");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { // line comment\r\n}", "//"), "class A { \n}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { // line comment\r\n} // line comment", "//"), "class A { \n} ");
    }

    @Test
    public void cleanLineComments2() throws Exception {
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {}", "//"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {} // line comment", "//"), "class A {} ");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { // line comment\n}", "//"), "class A { \n}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { // line comment\n} // line comment", "//"), "class A { \n} ");

        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {}", "//"), "class A {}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A {} # line comment", "#"), "class A {} ");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { # line comment\n}", "#"), "class A { \n}");
        assertEquals(CommentsCleanerUtils.cleanLineComments("class A { # line comment\n} # line comment", "#"), "class A { \n} ");
    }

}
