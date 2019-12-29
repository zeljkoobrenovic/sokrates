/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;
import nl.obren.sokrates.sourcecode.lang.clojure.ClojureLangAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cpp.CStyleAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cpp.CppAnalyzer;
import nl.obren.sokrates.sourcecode.lang.csharp.CSharpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.css.CssAnalyzer;
import nl.obren.sokrates.sourcecode.lang.d.DAnalyzer;
import nl.obren.sokrates.sourcecode.lang.go.GoLangAnalyzer;
import nl.obren.sokrates.sourcecode.lang.groovy.GroovyAnalyzer;
import nl.obren.sokrates.sourcecode.lang.html.HtmlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.java.JavaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.js.JavaScriptAnalyzer;
import nl.obren.sokrates.sourcecode.lang.julia.JuliaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.kotlin.KotlinAnalyzer;
import nl.obren.sokrates.sourcecode.lang.lua.LuaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.r.RAnalyzer;
import nl.obren.sokrates.sourcecode.lang.rust.RustAnalyzer;
import nl.obren.sokrates.sourcecode.lang.scss.ScssAnalyzer;
import nl.obren.sokrates.sourcecode.lang.shell.ShellAnalyzer;
import nl.obren.sokrates.sourcecode.lang.sql.SqlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.ts.TypeScriptAnalyzer;
import nl.obren.sokrates.sourcecode.lang.json.JsonAnalyzer;
import nl.obren.sokrates.sourcecode.lang.jsp.JspAnalyzer;
import nl.obren.sokrates.sourcecode.lang.less.LessAnalyzer;
import nl.obren.sokrates.sourcecode.lang.perl.PerlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.php.PhpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.python.PythonAnalyzer;
import nl.obren.sokrates.sourcecode.lang.ruby.RubyAnalyzer;
import nl.obren.sokrates.sourcecode.lang.sass.SassAnalyzer;
import nl.obren.sokrates.sourcecode.lang.scala.ScalaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.swift.SwiftAnalyzer;
import nl.obren.sokrates.sourcecode.lang.vb.VisualBasicAnalyzer;
import nl.obren.sokrates.sourcecode.lang.xml.XmlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.yaml.YamlAnalyzer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageAnalyzerFactory {
    private static final Log LOG = LogFactory.getLog(LanguageAnalyzerFactory.class);

    private static LanguageAnalyzerFactory instance = new LanguageAnalyzerFactory();
    private Map<String, Class> analyzersMap = new HashMap<>();
    private List<AnalyzerOverride> overrides = new ArrayList<>();

    private LanguageAnalyzerFactory() {
        analyzersMap.put("java", JavaAnalyzer.class);
        analyzersMap.put("go", GoLangAnalyzer.class);
        analyzersMap.put("cs", CSharpAnalyzer.class);
        analyzersMap.put("c", CStyleAnalyzer.class);
        analyzersMap.put("lua", LuaAnalyzer.class);
        analyzersMap.put("h", CStyleAnalyzer.class);
        analyzersMap.put("d", DAnalyzer.class);
        analyzersMap.put("cpp", CppAnalyzer.class);
        analyzersMap.put("cc", CppAnalyzer.class);
        analyzersMap.put("hpp", CppAnalyzer.class);
        analyzersMap.put("m", CppAnalyzer.class);
        analyzersMap.put("dart", CppAnalyzer.class);
        analyzersMap.put("php", PhpAnalyzer.class);
        analyzersMap.put("py", PythonAnalyzer.class);
        analyzersMap.put("js", JavaScriptAnalyzer.class);
        analyzersMap.put("ts", TypeScriptAnalyzer.class);
        analyzersMap.put("scala", ScalaAnalyzer.class);
        analyzersMap.put("html", HtmlAnalyzer.class);
        analyzersMap.put("htm", HtmlAnalyzer.class);
        analyzersMap.put("cshtml", HtmlAnalyzer.class);
        analyzersMap.put("asp", HtmlAnalyzer.class);
        analyzersMap.put("aspx", HtmlAnalyzer.class);
        analyzersMap.put("xml", XmlAnalyzer.class);
        analyzersMap.put("xaml", XmlAnalyzer.class);
        analyzersMap.put("csproj", XmlAnalyzer.class);
        analyzersMap.put("pl", PerlAnalyzer.class);
        analyzersMap.put("pm", PerlAnalyzer.class);
        analyzersMap.put("rb", RubyAnalyzer.class);
        analyzersMap.put("groovy", GroovyAnalyzer.class);
        analyzersMap.put("css", CssAnalyzer.class);
        analyzersMap.put("less", LessAnalyzer.class);
        analyzersMap.put("sass", SassAnalyzer.class);
        analyzersMap.put("scss", ScssAnalyzer.class);
        analyzersMap.put("json", JsonAnalyzer.class);
        analyzersMap.put("gsp", JspAnalyzer.class);
        analyzersMap.put("jsp", JspAnalyzer.class);
        analyzersMap.put("jsx", JavaScriptAnalyzer.class);
        analyzersMap.put("vb", VisualBasicAnalyzer.class);
        analyzersMap.put("bas", VisualBasicAnalyzer.class);
        analyzersMap.put("cls", VisualBasicAnalyzer.class);
        analyzersMap.put("ctl", VisualBasicAnalyzer.class);
        analyzersMap.put("clj", ClojureLangAnalyzer.class);
        analyzersMap.put("cljs", ClojureLangAnalyzer.class);
        analyzersMap.put("cljc", ClojureLangAnalyzer.class);
        analyzersMap.put("edn", ClojureLangAnalyzer.class);
        analyzersMap.put("swift", SwiftAnalyzer.class);
        analyzersMap.put("kt", KotlinAnalyzer.class);
        analyzersMap.put("kts", KotlinAnalyzer.class);
        analyzersMap.put("sql", SqlAnalyzer.class);
        analyzersMap.put("sh", ShellAnalyzer.class);
        analyzersMap.put("zsh", ShellAnalyzer.class);
        analyzersMap.put("bash", ShellAnalyzer.class);
        analyzersMap.put("yml", YamlAnalyzer.class);
        analyzersMap.put("yaml", YamlAnalyzer.class);
        analyzersMap.put("r", RAnalyzer.class);
        analyzersMap.put("rds", RAnalyzer.class);
        analyzersMap.put("rda", RAnalyzer.class);
        analyzersMap.put("rdata", RAnalyzer.class);
        analyzersMap.put("jl", JuliaAnalyzer.class);
        analyzersMap.put("rs", RustAnalyzer.class);
        analyzersMap.put("rlib", RustAnalyzer.class);
    }

    public static LanguageAnalyzerFactory getInstance() {
        return instance;
    }

    public Map<String, Class> getAnalyzersMap() {
        return analyzersMap;
    }

    public List<AnalyzerOverride> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<AnalyzerOverride> overrides) {
        this.overrides = overrides;
    }

    public LanguageAnalyzer getLanguageAnalyzerByExtension(String extension) {
        try {
            Class aClass = analyzersMap.get(extension);
            if (aClass != null) {
                return (LanguageAnalyzer) aClass.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error");
        }

        return new DefaultLanguageAnalyzer();
    }

    public LanguageAnalyzer getLanguageAnalyzer(SourceFile sourceFile) {
        return getLanguageAnalyzerByExtension(getAnalyzerKey(sourceFile));
    }

    private String getAnalyzerKey(SourceFile sourceFile) {
        for (AnalyzerOverride override : overrides) {
            boolean overridden = false;
            for (SourceFileFilter sourceFileFilter : override.getFilters()) {
                if (sourceFileFilter.matches(sourceFile) && sourceFileFilter.getInclude()) {
                    overridden = true;
                } else if (sourceFileFilter.matches(sourceFile) && !sourceFileFilter.getInclude()) {
                    overridden = false;
                    break;
                }
            }
            if (overridden) {
                return override.getAnalyzer();
            }
        }
        return FilenameUtils.getExtension(sourceFile.getFile().getPath()).toLowerCase();
    }

}
