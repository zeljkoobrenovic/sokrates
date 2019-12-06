package nl.obren.sokrates.sourcecode.lang.jsp;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class JspAnalyzerTest {
    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        JspAnalyzer analyzer = new JspAnalyzer();

        String sample = "<%--\n" +
                "\n" +
                "    Copyright 2013 Netflix, Inc.\n" +
                "\n" +
                "    Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "    you may not use this file except in compliance with the License.\n" +
                "    You may obtain a copy of the License at\n" +
                "\n" +
                "        http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "    Unless required by applicable law or agreed to in writing, software\n" +
                "    distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "    See the License for the specific language governing permissions and\n" +
                "    limitations under the License.\n" +
                "\n" +
                "--%>\n" +
                "<tr class=\"prop ${rowClass}\">\n" +
                "  <td class=\"name\">\n" +
                "    <label for=\"instanceType\"><g:link controller=\"instanceType\" action=\"list\">Instance Type:</g:link></label>\n" +
                "  </td>\n" +
                "  <td class=\"value\">\n" +
                "    <select id=\"instanceType\" name=\"instanceType\">\n" +
                "      <g:each var=\"t\" in=\"${instanceTypes}\">\n" +
                "        <option value=\"${t.name}\" ${t.name == params.instanceType || t.name == instanceType ? 'selected' : ''}>${t.name} ${t.monthlyLinuxOnDemandPrice ? t" +
                ".monthlyLinuxOnDemandPrice + '/mo' : ''}</option>\n" +
                "      </g:each>\n" +
                "    </select>\n" +
                "  </td>\n" +
                "</tr>";

        SourceFile sourceFile = new SourceFile(new File("dummy.jsp"), sample);
        assertEquals(analyzer.cleanForDuplicationCalculations(sourceFile).getCleanedLinesCount(), 7);
        assertEquals(analyzer.cleanForDuplicationCalculations(sourceFile).getCleanedContent(), "<tr class=\"prop ${rowClass}\">\n" +
                "<td class=\"name\">\n" +
                "<label for=\"instanceType\"><g:link controller=\"instanceType\" action=\"list\">Instance Type:</g:link></label>\n" +
                "<td class=\"value\">\n" +
                "<select id=\"instanceType\" name=\"instanceType\">\n" +
                "<g:each var=\"t\" in=\"${instanceTypes}\">\n" +
                "<option value=\"${t.name}\" ${t.name == params.instanceType || t.name == instanceType ? 'selected' : ''}>${t.name} ${t.monthlyLinuxOnDemandPrice ? t.monthlyLinuxOnDemandPrice + " +
                "'/mo' : ''}</option>");
    }

}
