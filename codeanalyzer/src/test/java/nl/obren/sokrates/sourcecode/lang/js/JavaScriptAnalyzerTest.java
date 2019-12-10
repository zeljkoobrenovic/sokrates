/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.js;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.lang.js.JavaScriptAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class JavaScriptAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "/* Declare the function 'myFunc' */\n" +
                "function myFunc(theObject) {\n" +
                "   theObject.brand = \"Toyota\";\n" +
                " }\n" +
                " \n" +
                " /*\n" +
                "  * Declare variable 'mycar';\n" +
                "  * create and initialize a new Object;\n" +
                "  * assign reference to it to 'mycar'\n" +
                "  */\n" +
                " var mycar = {\n" +
                "   brand: \"Honda\",\n" +
                "   model: \"Accord\",\n" +
                "   year: 1998\n" +
                " };\n" +
                "\n" +
                " /* Logs 'Honda' */\n" +
                " console.log(mycar.brand);\n" +
                "\n" +
                " /* Pass object reference to the function */\n" +
                " myFunc(mycar);\n" +
                "\n" +
                " /*\n" +
                "  * Logs 'Toyota' as the value of the 'brand' property\n" +
                "  * of the object, as changed to by the function.\n" +
                "  */\n" +
                " console.log(mycar.brand);";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.js"), code)).getCleanedContent(), "function myFunc(theObject) {\n" +
                "   theObject.brand = \"Toyota\";\n" +
                " }\n" +
                " var mycar = {\n" +
                "   brand: \"Honda\",\n" +
                "   model: \"Accord\",\n" +
                "   year: 1998\n" +
                " };\n" +
                " console.log(mycar.brand);\n" +
                " myFunc(mycar);\n" +
                " console.log(mycar.brand);");
    }

    @Test
    public void extractUnits() {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "define(function(require, exports, module) {\n" +
                "\n" +
                "/*\n" +
                " * DOM Level 2\n" +
                " * Object DOMException\n" +
                " * @see http://www.w3.org/TR/REC-DOM-Level-1/ecma-script-language-binding.html\n" +
                " * @see http://www.w3.org/TR/2000/REC-DOM-Level-2-Core-20001113/ecma-script-binding.html\n" +
                " */\n" +
                "\n" +
                "function copy(src,dest){\n" +
                "    for(var p in src){\n" +
                "        dest[p] = src[p];\n" +
                "    }\n" +
                "}\n" +
                "/**\n" +
                "^\\w+\\.prototype\\.([_\\w]+)\\s*=\\s*((?:.*\\{\\s*?[\\r\\n][\\s\\S]*?^})|\\S.*?(?=[;\\r\\n]));?\n" +
                "^\\w+\\.prototype\\.([_\\w]+)\\s*=\\s*(\\S.*?(?=[;\\r\\n]));?\n" +
                " */\n" +
                "function _extends(Class,Super){\n" +
                "    var pt = Class.prototype;\n" +
                "    if(Object.create){\n" +
                "        var ppt = Object.create(Super.prototype)\n" +
                "        pt.__proto__ = ppt;\n" +
                "    }\n" +
                "    if(!(pt instanceof Super)){\n" +
                "        function t(){};\n" +
                "        t.prototype = Super.prototype;\n" +
                "        t = new t();\n" +
                "        copy(pt,t);\n" +
                "        Class.prototype = pt = t;\n" +
                "    }\n" +
                "    if(pt.constructor != Class){\n" +
                "        if(typeof Class != 'function'){\n" +
                "        \tconsole.error(\"unknow Class:\"+Class)\n" +
                "        }\n" +
                "        pt.constructor = Class\n" +
                "    }\n" +
                "}\n" +
                "var htmlns = 'http://www.w3.org/1999/xhtml' ;\n" +
                "// Node Types\n" +
                "var NodeType = {}\n" +
                "var ELEMENT_NODE                = NodeType.ELEMENT_NODE                = 1;\n" +
                "var ATTRIBUTE_NODE              = NodeType.ATTRIBUTE_NODE              = 2;\n" +
                "var TEXT_NODE                   = NodeType.TEXT_NODE                   = 3;\n" +
                "var CDATA_SECTION_NODE          = NodeType.CDATA_SECTION_NODE          = 4;\n" +
                "var ENTITY_REFERENCE_NODE       = NodeType.ENTITY_REFERENCE_NODE       = 5;\n" +
                "var ENTITY_NODE                 = NodeType.ENTITY_NODE                 = 6;\n" +
                "var PROCESSING_INSTRUCTION_NODE = NodeType.PROCESSING_INSTRUCTION_NODE = 7;\n" +
                "var COMMENT_NODE                = NodeType.COMMENT_NODE                = 8;\n" +
                "var DOCUMENT_NODE               = NodeType.DOCUMENT_NODE               = 9;\n" +
                "var DOCUMENT_TYPE_NODE          = NodeType.DOCUMENT_TYPE_NODE          = 10;\n" +
                "var DOCUMENT_FRAGMENT_NODE      = NodeType.DOCUMENT_FRAGMENT_NODE      = 11;\n" +
                "var NOTATION_NODE               = NodeType.NOTATION_NODE               = 12;\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.js"), code));
        assertEquals(units.size(), 3);
        assertEquals(units.get(0).getShortName(), "define()");
        assertEquals(units.get(0).getLinesOfCode(), 16);
        assertEquals(units.get(0).getStartLine(), 1);
        assertEquals(units.get(0).getEndLine(), 54);
        assertEquals(units.get(1).getShortName(), "function copy()");
        assertEquals(units.get(1).getLinesOfCode(), 5);
        assertEquals(units.get(1).getStartLine(), 10);
        assertEquals(units.get(1).getEndLine(), 14);
        assertEquals(units.get(2).getShortName(), "function _extends()");
        assertEquals(units.get(2).getLinesOfCode(), 20);
        assertEquals(units.get(2).getStartLine(), 19);
        assertEquals(units.get(2).getEndLine(), 38);
    }

    @Test
    public void extractUnits2() {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "    this.$renderToken = function(stringBuilder, screenColumn, token, value) {\n" +
                "        var self = this;\n" +
                "        var replaceReg = /\\t|&|<|>|( +)|([\\x00-\\x1f\\x80-\\xa0\\xad\\u1680\\u180E\\u2000-\\u200f\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF\\uFFF9-\\uFFFC])" +
                "|[\\u1100-\\u115F\\u11A3-\\u11A7\\u11FA-\\u11FF\\u2329-\\u232A\\u2E80-\\u2E99\\u2E9B-\\u2EF3\\u2F00-\\u2FD5\\u2FF0-\\u2FFB\\u3000-\\u303E\\u3041-\\u3096\\u3099-\\u30FF\\u3105" +
                "-\\u312D\\u3131-\\u318E\\u3190-\\u31BA\\u31C0-\\u31E3\\u31F0-\\u321E\\u3220-\\u3247\\u3250-\\u32FE\\u3300-\\u4DBF\\u4E00-\\uA48C\\uA490-\\uA4C6\\uA960-\\uA97C\\uAC00-\\uD7A3\\uD7B0" +
                "-\\uD7C6\\uD7CB-\\uD7FB\\uF900-\\uFAFF\\uFE10-\\uFE19\\uFE30-\\uFE52\\uFE54-\\uFE66\\uFE68-\\uFE6B\\uFF01-\\uFF60\\uFFE0-\\uFFE6]/g;\n" +
                "        var replaceFunc = function(c, a, b, tabIdx, idx4) {\n" +
                "            if (a) {\n" +
                "                return self.showInvisibles\n" +
                "                    ? \"<span class='ace_invisible ace_invisible_space'>\" + lang.stringRepeat(self.SPACE_CHAR, c.length) + \"</span>\"\n" +
                "                    : c;\n" +
                "            } else if (c == \"&\") {\n" +
                "                return \"&#38;\";\n" +
                "            } else if (c == \"<\") {\n" +
                "                return \"&#60;\";\n" +
                "            } else if (c == \">\") {\n" +
                "                // normally escaping this is not needed, but xml documents throw error when setting innerHTML to ]]>\n" +
                "                return \"&#62;\";\n" +
                "            } else if (c == \"\\t\") {\n" +
                "                var tabSize = self.session.getScreenTabSize(screenColumn + tabIdx);\n" +
                "                screenColumn += tabSize - 1;\n" +
                "                return self.$tabStrings[tabSize];\n" +
                "            } else if (c == \"\\u3000\") {\n" +
                "                // U+3000 is both invisible AND full-width, so must be handled uniquely\n" +
                "                var classToUse = self.showInvisibles ? \"ace_cjk ace_invisible ace_invisible_space\" : \"ace_cjk\";\n" +
                "                var space = self.showInvisibles ? self.SPACE_CHAR : \"\";\n" +
                "                screenColumn += 1;\n" +
                "                return \"<span class='\" + classToUse + \"' style='width:\" +\n" +
                "                    (self.config.characterWidth * 2) +\n" +
                "                    \"px'>\" + space + \"</span>\";\n" +
                "            } else if (b) {\n" +
                "                return \"<span class='ace_invisible ace_invisible_space ace_invalid'>\" + self.SPACE_CHAR + \"</span>\";\n" +
                "            } else {\n" +
                "                screenColumn += 1;\n" +
                "                return \"<span class='ace_cjk' style='width:\" +\n" +
                "                    (self.config.characterWidth * 2) +\n" +
                "                    \"px'>\" + c + \"</span>\";\n" +
                "            }\n" +
                "        };\n" +
                "\n" +
                "        var output = value.replace(replaceReg, replaceFunc);\n" +
                "\n" +
                "        if (!this.$textToken[token.type]) {\n" +
                "            var classes = \"ace_\" + token.type.replace(/\\./g, \" ace_\");\n" +
                "            var style = \"\";\n" +
                "            if (token.type == \"fold\")\n" +
                "                style = \" style='width:\" + (token.value.length * this.config.characterWidth) + \"px;' \";\n" +
                "            stringBuilder.push(\"<span class='\", classes, \"'\", style, \">\", output, \"</span>\");\n" +
                "        }\n" +
                "        else {\n" +
                "            stringBuilder.push(output);\n" +
                "        }\n" +
                "        return screenColumn + value.length;\n" +
                "    };\n";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.js"), code));
        assertEquals(units.size(), 2);
        assertEquals(units.get(0).getShortName(), "this.$renderToken = function()");
        assertEquals(units.get(0).getLinesOfCode(), 16);
        assertEquals(units.get(0).getStartLine(), 1);
        assertEquals(units.get(0).getEndLine(), 51);
        assertEquals(units.get(1).getShortName(), "var replaceFunc = function()");
        assertEquals(units.get(1).getLinesOfCode(), 31);
        assertEquals(units.get(1).getStartLine(), 4);
        assertEquals(units.get(1).getEndLine(), 36);
    }

    @Test
    public void extractUnits4() {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "/* ***** BEGIN LICENSE BLOCK *****\n" +
                " * Distributed under the BSD license:\n" +
                " *\n" +
                " * Copyright (c) 2010, Ajax.org B.V.\n" +
                " * All rights reserved.\n" +
                " * \n" +
                " * Redistribution and use in source and binary forms, with or without\n" +
                " * modification, are permitted provided that the following conditions are met:\n" +
                " *     * Redistributions of source code must retain the above copyright\n" +
                " *       notice, this list of conditions and the following disclaimer.\n" +
                " *     * Redistributions in binary form must reproduce the above copyright\n" +
                " *       notice, this list of conditions and the following disclaimer in the\n" +
                " *       documentation and/or other materials provided with the distribution.\n" +
                " *     * Neither the name of Ajax.org B.V. nor the\n" +
                " *       names of its contributors may be used to endorse or promote products\n" +
                " *       derived from this software without specific prior written permission.\n" +
                " * \n" +
                " * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND\n" +
                " * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" +
                " * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n" +
                " * DISCLAIMED. IN NO EVENT SHALL AJAX.ORG B.V. BE LIABLE FOR ANY\n" +
                " * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n" +
                " * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n" +
                " * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND\n" +
                " * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" +
                " * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" +
                " * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n" +
                " *\n" +
                " * ***** END LICENSE BLOCK ***** */\n" +
                "\n" +
                "define(function(require, exports, module) {\n" +
                "var BaseTokenizer = require(\"./tokenizer\").Tokenizer;\n" +
                "\n" +
                "// tokenizing lines longer than this makes editor very slow\n" +
                "var MAX_TOKEN_COUNT = 100000;\n" +
                "/*\n" +
                " * version of Tokenizer with additional logging\n" +
                " * and infinite loop checks\n" +
                " * can be used for developing/testing new modes\n" +
                " **/\n" +
                "\n" +
                "var Tokenizer = function(rules) {\n" +
                "    BaseTokenizer.call(this, rules);\n" +
                "\n" +
                "    /**\n" +
                "     * Returns an object containing two properties: `tokens`, which contains all the tokens; and `state`, the current state.\n" +
                "     * @returns {Object}\n" +
                "     **/\n" +
                "    this.getLineTokens = function(line, startState) {\n" +
                "        if (startState && typeof startState != \"string\") {\n" +
                "            var stack = startState.slice(0);\n" +
                "            startState = stack[0];\n" +
                "        } else\n" +
                "            var stack = [];\n" +
                "\n" +
                "        var currentState = startState || \"start\";\n" +
                "        var state = this.states[currentState];\n" +
                "        var mapping = this.matchMappings[currentState];\n" +
                "        var re = this.regExps[currentState];\n" +
                "        re.lastIndex = 0;\n" +
                "\n" +
                "        var match, tokens = [];\n" +
                "\n" +
                "        var lastIndex = 0;\n" +
                "\n" +
                "        var stateTransitions = [];\n" +
                "        function onStateChange() {\n" +
                "            stateTransitions.push(startState+\"@\"+lastIndex);\n" +
                "        }\n" +
                "        function initState() {\n" +
                "            onStateChange();\n" +
                "            stateTransitions = [];\n" +
                "            onStateChange();\n" +
                "        }\n" +
                "        \n" +
                "        var token = {\n" +
                "            type: null,\n" +
                "            value: \"\",\n" +
                "            state: currentState\n" +
                "        };\n" +
                "        initState();\n" +
                "        \n" +
                "        var maxRecur = 100000;\n" +
                "        \n" +
                "        while (match = re.exec(line)) {\n" +
                "            var type = mapping.defaultToken;\n" +
                "            var rule = null;\n" +
                "            var value = match[0];\n" +
                "            var index = re.lastIndex;\n" +
                "\n" +
                "            if (index - value.length > lastIndex) {\n" +
                "                var skipped = line.substring(lastIndex, index - value.length);\n" +
                "                if (token.type == type) {\n" +
                "                    token.value += skipped;\n" +
                "                } else {\n" +
                "                    if (token.type)\n" +
                "                        tokens.push(token);\n" +
                "                    token = {type: type, value: skipped};\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            for (var i = 0; i < match.length-2; i++) {\n" +
                "                if (match[i + 1] === undefined)\n" +
                "                    continue;\n" +
                "                \n" +
                "                if (!maxRecur--) {\n" +
                "                    throw \"infinite\" + state[mapping[i]] + currentState\n" +
                "                }\n" +
                "\n" +
                "                rule = state[mapping[i]];\n" +
                "\n" +
                "                if (rule.onMatch)\n" +
                "                    type = rule.onMatch(value, currentState, stack);\n" +
                "                else\n" +
                "                    type = rule.token;\n" +
                "\n" +
                "                if (rule.next) {\n" +
                "                    if (typeof rule.next == \"string\")\n" +
                "                        currentState = rule.next;\n" +
                "                    else\n" +
                "                        currentState = rule.next(currentState, stack);\n" +
                "\n" +
                "                    state = this.states[currentState];\n" +
                "                    if (!state) {\n" +
                "                        window.console && console.error && console.error(currentState, \"doesn't exist\");\n" +
                "                        currentState = \"start\";\n" +
                "                        state = this.states[currentState];\n" +
                "                    }\n" +
                "                    mapping = this.matchMappings[currentState];\n" +
                "                    lastIndex = index;\n" +
                "                    re = this.regExps[currentState];\n" +
                "                    re.lastIndex = index;\n" +
                "\n" +
                "                    onStateChange();\n" +
                "                }\n" +
                "                break;\n" +
                "            }\n" +
                "\n" +
                "            if (value) {\n" +
                "                if (typeof type == \"string\") {\n" +
                "                    if ((!rule || rule.merge !== false) && token.type === type) {\n" +
                "                        token.value += value;\n" +
                "                    } else {\n" +
                "                        if (token.type)\n" +
                "                            tokens.push(token);\n" +
                "                        token = {type: type, value: value};\n" +
                "                    }\n" +
                "                } else {\n" +
                "                    if (token.type)\n" +
                "                        tokens.push(token);\n" +
                "                    token = {type: null, value: \"\"};\n" +
                "                    for (var i = 0; i < type.length; i++)\n" +
                "                        tokens.push(type[i]);\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            if (lastIndex == line.length)\n" +
                "                break;\n" +
                "\n" +
                "            lastIndex = index;\n" +
                "\n" +
                "            if (tokens.length > MAX_TOKEN_COUNT) {\n" +
                "                token.value += line.substr(lastIndex);\n" +
                "                currentState = \"start\"\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        if (token.type)\n" +
                "            tokens.push(token);\n" +
                "\n" +
                "        return {\n" +
                "            tokens : tokens,\n" +
                "            state : stack.length ? stack : currentState\n" +
                "        };\n" +
                "    };\n" +
                "\n" +
                "};\n" +
                "\n" +
                "Tokenizer.prototype = BaseTokenizer.prototype;\n" +
                "\n" +
                "exports.Tokenizer = Tokenizer;\n" +
                "});\n";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.js"), code));
        assertEquals(units.size(), 5);
        assertEquals(units.get(0).getShortName(), "define()");
        assertEquals(units.get(0).getLinesOfCode(), 6);
        assertEquals(units.get(0).getStartLine(), 31);
        assertEquals(units.get(0).getEndLine(), 183);
        assertEquals(units.get(0).getChildren().size(), 1);
        assertEquals(units.get(0).getChildren().get(0).getShortName(), "var Tokenizer = function()");
        assertEquals(units.get(1).getShortName(), "var Tokenizer = function()");
        assertEquals(units.get(1).getLinesOfCode(), 3);
        assertEquals(units.get(1).getStartLine(), 42);
        assertEquals(units.get(1).getEndLine(), 178);
        assertEquals(units.get(1).getChildren().size(), 1);
        assertEquals(units.get(1).getChildren().get(0).getShortName(), "this.getLineTokens = function()");
        assertEquals(units.get(2).getShortName(), "this.getLineTokens = function()");
        assertEquals(units.get(2).getLinesOfCode(), 83);
        assertEquals(units.get(2).getStartLine(), 49);
        assertEquals(units.get(2).getEndLine(), 176);
        assertEquals(units.get(2).getChildren().size(), 2);
        assertEquals(units.get(3).getShortName(), "function onStateChange()");
        assertEquals(units.get(3).getLinesOfCode(), 3);
        assertEquals(units.get(3).getStartLine(), 67);
        assertEquals(units.get(3).getEndLine(), 69);
        assertEquals(units.get(3).getChildren().size(), 0);
        assertEquals(units.get(4).getShortName(), "function initState()");
        assertEquals(units.get(4).getLinesOfCode(), 5);
        assertEquals(units.get(4).getStartLine(), 70);
        assertEquals(units.get(4).getEndLine(), 74);
        assertEquals(units.get(4).getChildren().size(), 0);
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "/* Declare the function 'myFunc' */\n" +
                "function myFunc(theObject) {\n" +
                "   theObject.brand = \"Toyota\";\n" +
                " }\n" +
                " \n" +
                " /*\n" +
                "  * Declare variable 'mycar';\n" +
                "  * create and initialize a new Object;\n" +
                "  * assign reference to it to 'mycar'\n" +
                "  */\n" +
                " var mycar = {\n" +
                "   brand: \"Honda\",\n" +
                "   model: \"Accord\",\n" +
                "   year: 1998\n" +
                " };\n" +
                "\n" +
                " /* Logs 'Honda' */\n" +
                " console.log(mycar.brand);\n" +
                "\n" +
                " /* Pass object reference to the function */\n" +
                " myFunc(mycar);\n" +
                "\n" +
                " /*\n" +
                "  * Logs 'Toyota' as the value of the 'brand' property\n" +
                "  * of the object, as changed to by the function.\n" +
                "  */\n" +
                " console.log(mycar.brand);";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.js"), code));
        assertEquals(cleanedContent.getCleanedContent(), "function myFunc(theObject) {\n" +
                "theObject.brand = \"Toyota\";\n" +
                "var mycar = {\n" +
                "brand: \"Honda\",\n" +
                "model: \"Accord\",\n" +
                "year: 1998\n" +
                "};\n" +
                "console.log(mycar.brand);\n" +
                "myFunc(mycar);\n" +
                "console.log(mycar.brand);");
        assertEquals(cleanedContent.getCleanedLinesCount(), 10);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 10);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 10);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 11);
        assertEquals(cleanedContent.getFileLineIndexes().get(4).intValue(), 12);
        assertEquals(cleanedContent.getFileLineIndexes().get(5).intValue(), 13);
        assertEquals(cleanedContent.getFileLineIndexes().get(6).intValue(), 14);
        assertEquals(cleanedContent.getFileLineIndexes().get(7).intValue(), 17);
        assertEquals(cleanedContent.getFileLineIndexes().get(8).intValue(), 20);
        assertEquals(cleanedContent.getFileLineIndexes().get(9).intValue(), 26);
    }


    @Test
    public void extractUnits1() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "/* Declare the function 'myFunc' */\n" +
                "function myFunc(theObject) {\n" +
                "   theObject.brand = \"Toyota\";\n" +
                " }\n" +
                " ";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.js"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "function myFunc()");
        assertEquals(units.get(0).getLinesOfCode(), 3);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnits3() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "function checkData(dummy) {\n" +
                "  if (document.form1.threeChar.value.length == 3) {\n" +
                "    return true;\n" +
                "  } else {\n" +
                "    while(true) {\n" +
                "       alert(\"Enter exactly three characters. \" +\n" +
                "       document.form1.threeChar.value + \" is not valid.\");\n" +
                "       return false;\n" +
                "    }\n" +
                "  }\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.js"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "function checkData()");
        assertEquals(units.get(0).getLinesOfCode(), 11);
        assertEquals(units.get(0).getMcCabeIndex(), 3);
        assertEquals(units.get(0).getNumberOfParameters(), 1);

    }

    @Test
    public void extractUnits5() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        String code = "checkData(dummy) {\n" +
                "  if (document.form1.threeChar.value.length == 3) {\n" +
                "    return true;\n" +
                "  } else {\n" +
                "    while(true) {\n" +
                "       alert(\"Enter exactly three characters. \" +\n" +
                "       document.form1.threeChar.value + \" is not valid.\");\n" +
                "       return false;\n" +
                "    }\n" +
                "    while (true) {\n" +
                "       document.form1.threeChar.value + \" is not valid.\");\n" +
                "    }\n" +
                "    if(true) {\n" +
                "       return false;\n" +
                "    }\n" +
                "    if (true) {\n" +
                "       return false;\n" +
                "    }\n" +
                "    switch(expression) {\n" +
                "       case 1: return false;\n" +
                "    }\n" +
                "    switch (expression) {\n" +
                "       case 1: return false;\n" +
                "    }\n" +
                "    catch(expression) {\n" +
                "       return false;\n" +
                "    }\n" +
                "    catch (expression) {\n" +
                "       return false;\n" +
                "    }\n" +
                "  }\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.js"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "checkData()");
        assertEquals(units.get(0).getLinesOfCode(), 32);
        assertEquals(units.get(0).getMcCabeIndex(), 10);
        assertEquals(units.get(0).getNumberOfParameters(), 1);

    }

    @Test
    public void extractDependencies() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();
        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(), new ProgressFeedback()).getDependencies();
        assertEquals(dependencies.size(), 0);
    }

    @Test
    public void doesNotStartWithKeyword() throws Exception {
        JavaScriptAnalyzer analyzer = new JavaScriptAnalyzer();

        assertTrue(analyzer.doesNotStartWithKeyword("myFunction() {"));
        assertTrue(analyzer.doesNotStartWithKeyword("myFunction (param1, param2) {"));

        assertFalse(analyzer.doesNotStartWithKeyword("while (true) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("while(true) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("if (a) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("if(a) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("switch(b) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("switch (b) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("catch (e) {"));
        assertFalse(analyzer.doesNotStartWithKeyword("catch(e) {"));
    }
}
