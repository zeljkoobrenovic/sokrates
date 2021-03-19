/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;
import nl.obren.sokrates.sourcecode.lang.adabasnatural.AdabasNaturalAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cfg.CfgAnalyzer;
import nl.obren.sokrates.sourcecode.lang.clojure.ClojureLangAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cpp.CStyleAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cpp.CppAnalyzer;
import nl.obren.sokrates.sourcecode.lang.csharp.CSharpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.css.CssAnalyzer;
import nl.obren.sokrates.sourcecode.lang.d.DAnalyzer;
import nl.obren.sokrates.sourcecode.lang.dbc.DbcAnalyzer;
import nl.obren.sokrates.sourcecode.lang.go.GoLangAnalyzer;
import nl.obren.sokrates.sourcecode.lang.groovy.GroovyAnalyzer;
import nl.obren.sokrates.sourcecode.lang.html.HtmlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.java.JavaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.js.JavaScriptAnalyzer;
import nl.obren.sokrates.sourcecode.lang.json.JsonAnalyzer;
import nl.obren.sokrates.sourcecode.lang.jsp.JspAnalyzer;
import nl.obren.sokrates.sourcecode.lang.julia.JuliaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.kotlin.KotlinAnalyzer;
import nl.obren.sokrates.sourcecode.lang.less.LessAnalyzer;
import nl.obren.sokrates.sourcecode.lang.lua.LuaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.objectpascal.ObjectPascalAnalyzer;
import nl.obren.sokrates.sourcecode.lang.perl.PerlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.php.PhpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.puppet.PuppetAnalyzer;
import nl.obren.sokrates.sourcecode.lang.python.PythonAnalyzer;
import nl.obren.sokrates.sourcecode.lang.r.RAnalyzer;
import nl.obren.sokrates.sourcecode.lang.ruby.RubyAnalyzer;
import nl.obren.sokrates.sourcecode.lang.rust.RustAnalyzer;
import nl.obren.sokrates.sourcecode.lang.sass.SassAnalyzer;
import nl.obren.sokrates.sourcecode.lang.scala.ScalaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.scss.ScssAnalyzer;
import nl.obren.sokrates.sourcecode.lang.shell.ShellAnalyzer;
import nl.obren.sokrates.sourcecode.lang.sql.SqlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.swift.SwiftAnalyzer;
import nl.obren.sokrates.sourcecode.lang.thrift.ThriftAnalyzer;
import nl.obren.sokrates.sourcecode.lang.ts.TypeScriptAnalyzer;
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
        // java
        analyzersMap.put("java", JavaAnalyzer.class);
        analyzersMap.put("ck", JavaAnalyzer.class);
        analyzersMap.put("j", JavaAnalyzer.class);
        analyzersMap.put("uc", JavaAnalyzer.class);

        // javascript
        analyzersMap.put("js", JavaScriptAnalyzer.class);
        analyzersMap.put("cy", JavaScriptAnalyzer.class);
        analyzersMap.put("jsx", HtmlAnalyzer.class);
        analyzersMap.put("_js", JavaScriptAnalyzer.class);
        analyzersMap.put("bones", JavaScriptAnalyzer.class);
        analyzersMap.put("cjs", JavaScriptAnalyzer.class);
        analyzersMap.put("es", JavaScriptAnalyzer.class);
        analyzersMap.put("es6", JavaScriptAnalyzer.class);
        analyzersMap.put("frag", JavaScriptAnalyzer.class);
        analyzersMap.put("gs", JavaScriptAnalyzer.class);
        analyzersMap.put("jake", JavaScriptAnalyzer.class);
        analyzersMap.put("jsb", JavaScriptAnalyzer.class);
        analyzersMap.put("jscad", JavaScriptAnalyzer.class);
        analyzersMap.put("jsfl", JavaScriptAnalyzer.class);
        analyzersMap.put("jsm", JavaScriptAnalyzer.class);
        analyzersMap.put("njs", JavaScriptAnalyzer.class);
        analyzersMap.put("pac", JavaScriptAnalyzer.class);
        analyzersMap.put("sjs", JavaScriptAnalyzer.class);
        analyzersMap.put("ssjs", JavaScriptAnalyzer.class);
        analyzersMap.put("xsjs", JavaScriptAnalyzer.class);
        analyzersMap.put("xsjslib", JavaScriptAnalyzer.class);

        // typescript
        analyzersMap.put("ts", TypeScriptAnalyzer.class);
        analyzersMap.put("tsx", TypeScriptAnalyzer.class);

        analyzersMap.put("go", GoLangAnalyzer.class);
        analyzersMap.put("v", GoLangAnalyzer.class);

        // C#
        analyzersMap.put("cs", CSharpAnalyzer.class);
        analyzersMap.put("cake", CSharpAnalyzer.class);
        analyzersMap.put("csx", CSharpAnalyzer.class);

        analyzersMap.put("lua", LuaAnalyzer.class);
        analyzersMap.put("nse", LuaAnalyzer.class);
        analyzersMap.put("p8", LuaAnalyzer.class);
        analyzersMap.put("pd_lua", LuaAnalyzer.class);
        analyzersMap.put("rbxs", LuaAnalyzer.class);
        analyzersMap.put("rockspec", LuaAnalyzer.class);
        analyzersMap.put("wlua", LuaAnalyzer.class);

        analyzersMap.put("d", DAnalyzer.class);
        analyzersMap.put("di", DAnalyzer.class);

        // c
        analyzersMap.put("c", CStyleAnalyzer.class);
        analyzersMap.put("cats", CStyleAnalyzer.class);
        analyzersMap.put("idc", CStyleAnalyzer.class);

        // cpp
        analyzersMap.put("cpp", CppAnalyzer.class);
        analyzersMap.put("c++", CppAnalyzer.class);
        analyzersMap.put("cc", CppAnalyzer.class);
        analyzersMap.put("cp", CppAnalyzer.class);
        analyzersMap.put("cxx", CppAnalyzer.class);
        analyzersMap.put("h", CppAnalyzer.class);
        analyzersMap.put("h++", CppAnalyzer.class);
        analyzersMap.put("hh", CppAnalyzer.class);
        analyzersMap.put("hpp", CppAnalyzer.class);
        analyzersMap.put("hxx", CppAnalyzer.class);
        analyzersMap.put("inl", CppAnalyzer.class);
        analyzersMap.put("ino", CppAnalyzer.class);
        analyzersMap.put("ipp", CppAnalyzer.class);
        analyzersMap.put("re", CppAnalyzer.class);
        analyzersMap.put("tcc", CppAnalyzer.class);
        analyzersMap.put("tpp", CppAnalyzer.class);
        analyzersMap.put("m", CppAnalyzer.class);
        analyzersMap.put("mm", CppAnalyzer.class);
        analyzersMap.put("dart", CppAnalyzer.class);

        // php
        analyzersMap.put("php", PhpAnalyzer.class);
        analyzersMap.put("inc", PhpAnalyzer.class);
        analyzersMap.put("php3", PhpAnalyzer.class);
        analyzersMap.put("php4", PhpAnalyzer.class);
        analyzersMap.put("php5", PhpAnalyzer.class);
        analyzersMap.put("phps", PhpAnalyzer.class);
        analyzersMap.put("phpt", PhpAnalyzer.class);
        analyzersMap.put("ctp", PhpAnalyzer.class);
        analyzersMap.put("aw", PhpAnalyzer.class);

        registerPython();

        // scala
        analyzersMap.put("scala", ScalaAnalyzer.class);
        analyzersMap.put("kojo", ScalaAnalyzer.class);
        analyzersMap.put("sbt", ScalaAnalyzer.class);
        analyzersMap.put("sc", ScalaAnalyzer.class);
        registerHtml();

        // asp
        analyzersMap.put("asp", HtmlAnalyzer.class);
        analyzersMap.put("aspx", HtmlAnalyzer.class);
        analyzersMap.put("asax", HtmlAnalyzer.class);
        analyzersMap.put("ascx", HtmlAnalyzer.class);
        analyzersMap.put("ashx", HtmlAnalyzer.class);
        analyzersMap.put("asmx", HtmlAnalyzer.class);
        analyzersMap.put("axd", HtmlAnalyzer.class);

        registerXml();

        // perl
        analyzersMap.put("pl", PerlAnalyzer.class);
        analyzersMap.put("al", PerlAnalyzer.class);
        analyzersMap.put("perl", PerlAnalyzer.class);
        analyzersMap.put("ph", PerlAnalyzer.class);
        analyzersMap.put("plx", PerlAnalyzer.class);
        analyzersMap.put("pm", PerlAnalyzer.class);
        analyzersMap.put("psgi", PerlAnalyzer.class);
        analyzersMap.put("t", PerlAnalyzer.class);

        registerRuby();

        // groovy
        analyzersMap.put("groovy", GroovyAnalyzer.class);
        analyzersMap.put("grt", GroovyAnalyzer.class);
        analyzersMap.put("gtpl", GroovyAnalyzer.class);
        analyzersMap.put("gvy", GroovyAnalyzer.class);

        analyzersMap.put("css", CssAnalyzer.class);
        analyzersMap.put("less", LessAnalyzer.class);
        analyzersMap.put("sass", SassAnalyzer.class);
        analyzersMap.put("scss", ScssAnalyzer.class);
        registerJson();

        analyzersMap.put("gsp", JspAnalyzer.class);
        analyzersMap.put("jsp", JspAnalyzer.class);

        registerVisualBasic();

        registerClojure();

        analyzersMap.put("swift", SwiftAnalyzer.class);

        // kotlin
        analyzersMap.put("kt", KotlinAnalyzer.class);
        analyzersMap.put("ktm", KotlinAnalyzer.class);
        analyzersMap.put("kts", KotlinAnalyzer.class);

        registerSql();

        // shell
        analyzersMap.put("sh", ShellAnalyzer.class);
        analyzersMap.put("bash", ShellAnalyzer.class);
        analyzersMap.put("bats", ShellAnalyzer.class);
        analyzersMap.put("command", ShellAnalyzer.class);
        analyzersMap.put("ksh", ShellAnalyzer.class);
        analyzersMap.put("tmux", ShellAnalyzer.class);
        analyzersMap.put("tool", ShellAnalyzer.class);
        analyzersMap.put("zsh", ShellAnalyzer.class);

        analyzersMap.put("dbc", DbcAnalyzer.class);
        analyzersMap.put("cfg", CfgAnalyzer.class);

        registerYaml();

        registerR();

        analyzersMap.put("jl", JuliaAnalyzer.class);

        analyzersMap.put("rs", RustAnalyzer.class);
        analyzersMap.put("in", RustAnalyzer.class);
        analyzersMap.put("rlib", RustAnalyzer.class);

        analyzersMap.put("pas", ObjectPascalAnalyzer.class);
        analyzersMap.put("pp", ObjectPascalAnalyzer.class);
        analyzersMap.put("p", ObjectPascalAnalyzer.class);
        analyzersMap.put("dfm", ObjectPascalAnalyzer.class);
        analyzersMap.put("dpr", ObjectPascalAnalyzer.class);
        analyzersMap.put("lpr", ObjectPascalAnalyzer.class);
        analyzersMap.put("pascal", ObjectPascalAnalyzer.class);

        analyzersMap.put("nsp", AdabasNaturalAnalyzer.class);
        analyzersMap.put("nsm", AdabasNaturalAnalyzer.class);
        analyzersMap.put("nsh", AdabasNaturalAnalyzer.class);
        analyzersMap.put("nsd", AdabasNaturalAnalyzer.class);
        analyzersMap.put("nsn", AdabasNaturalAnalyzer.class);

        analyzersMap.put("pp", PuppetAnalyzer.class);
    }

    private void registerRuby() {
        // ruby
        analyzersMap.put("rb", RubyAnalyzer.class);
        analyzersMap.put("builder", RubyAnalyzer.class);
        analyzersMap.put("eye", RubyAnalyzer.class);
        analyzersMap.put("gemspec", RubyAnalyzer.class);
        analyzersMap.put("god", RubyAnalyzer.class);
        analyzersMap.put("jbuilder", RubyAnalyzer.class);
        analyzersMap.put("mspec", RubyAnalyzer.class);
        analyzersMap.put("podspec", RubyAnalyzer.class);
        analyzersMap.put("rabl", RubyAnalyzer.class);
        analyzersMap.put("rake", RubyAnalyzer.class);
        analyzersMap.put("rbi", RubyAnalyzer.class);
        analyzersMap.put("rbuild", RubyAnalyzer.class);
        analyzersMap.put("rbw", RubyAnalyzer.class);
        analyzersMap.put("rbx", RubyAnalyzer.class);
        analyzersMap.put("ru", RubyAnalyzer.class);
        analyzersMap.put("ruby", RubyAnalyzer.class);
        analyzersMap.put("thor", RubyAnalyzer.class);
        analyzersMap.put("watchr", RubyAnalyzer.class);
    }

    private void registerVisualBasic() {
        // vb
        analyzersMap.put("vb", VisualBasicAnalyzer.class);
        analyzersMap.put("bas", VisualBasicAnalyzer.class);
        analyzersMap.put("cls", VisualBasicAnalyzer.class);
        analyzersMap.put("ctl", VisualBasicAnalyzer.class);
        analyzersMap.put("frm", VisualBasicAnalyzer.class);
        analyzersMap.put("frx", VisualBasicAnalyzer.class);
        analyzersMap.put("vba", VisualBasicAnalyzer.class);
        analyzersMap.put("vbs", VisualBasicAnalyzer.class);
    }

    private void registerClojure() {
        analyzersMap.put("clj", ClojureLangAnalyzer.class);
        analyzersMap.put("cljs", ClojureLangAnalyzer.class);
        analyzersMap.put("cljscm", ClojureLangAnalyzer.class);
        analyzersMap.put("cljc", ClojureLangAnalyzer.class);
        analyzersMap.put("cljx", ClojureLangAnalyzer.class);
        analyzersMap.put("hl", ClojureLangAnalyzer.class);
        analyzersMap.put("hic", ClojureLangAnalyzer.class);
        analyzersMap.put("cl2", ClojureLangAnalyzer.class);
        analyzersMap.put("boot", ClojureLangAnalyzer.class);
        analyzersMap.put("edn", ClojureLangAnalyzer.class);
        analyzersMap.put("rg", ClojureLangAnalyzer.class);
        analyzersMap.put("wisp", ClojureLangAnalyzer.class);
    }

    private void registerYaml() {
        analyzersMap.put("yml", YamlAnalyzer.class);
        analyzersMap.put("yaml", YamlAnalyzer.class);
        analyzersMap.put("mir", YamlAnalyzer.class);
        analyzersMap.put("reek", YamlAnalyzer.class);
        analyzersMap.put("rviz", YamlAnalyzer.class);
        analyzersMap.put("syntax", YamlAnalyzer.class);
        analyzersMap.put("sublime-syntax", YamlAnalyzer.class);
        analyzersMap.put("yaml-tmlanguage", YamlAnalyzer.class);
        analyzersMap.put("sed", YamlAnalyzer.class);
    }

    private void registerPython() {
        analyzersMap.put("py", PythonAnalyzer.class);
        analyzersMap.put("gyp", PythonAnalyzer.class);
        analyzersMap.put("gypi", PythonAnalyzer.class);
        analyzersMap.put("lmi", PythonAnalyzer.class);
        analyzersMap.put("py3", PythonAnalyzer.class);
        analyzersMap.put("pyde", PythonAnalyzer.class);
        analyzersMap.put("pyi", PythonAnalyzer.class);
        analyzersMap.put("pyp", PythonAnalyzer.class);
        analyzersMap.put("pyt", PythonAnalyzer.class);
        analyzersMap.put("pyw", PythonAnalyzer.class);
        analyzersMap.put("rpy", PythonAnalyzer.class);
        analyzersMap.put("smk", PythonAnalyzer.class);
        analyzersMap.put("tac", PythonAnalyzer.class);
        analyzersMap.put("wsgi", PythonAnalyzer.class);
        analyzersMap.put("xpy", PythonAnalyzer.class);
        analyzersMap.put("eb", PythonAnalyzer.class);
        analyzersMap.put("gn", PythonAnalyzer.class);
        analyzersMap.put("pyx", PythonAnalyzer.class);
        analyzersMap.put("pxd", PythonAnalyzer.class);
        analyzersMap.put("pxi", PythonAnalyzer.class);
        analyzersMap.put("numpy", PythonAnalyzer.class);
        analyzersMap.put("numpyw", PythonAnalyzer.class);
        analyzersMap.put("numsc", PythonAnalyzer.class);
        analyzersMap.put("pytb", PythonAnalyzer.class);
    }

    private void registerHtml() {
        //
        analyzersMap.put("html", HtmlAnalyzer.class);
        analyzersMap.put("htm", HtmlAnalyzer.class);
        analyzersMap.put("cshtml", HtmlAnalyzer.class);
        analyzersMap.put("vbhtml", HtmlAnalyzer.class);
        analyzersMap.put("razor", HtmlAnalyzer.class);
        analyzersMap.put("soy", HtmlAnalyzer.class);
        analyzersMap.put("st", HtmlAnalyzer.class);
        analyzersMap.put("xht", HtmlAnalyzer.class);
        analyzersMap.put("xhtml", HtmlAnalyzer.class);
        analyzersMap.put("jinja", HtmlAnalyzer.class);
        analyzersMap.put("jinja2", HtmlAnalyzer.class);
        analyzersMap.put("mustache", HtmlAnalyzer.class);
        analyzersMap.put("njk", HtmlAnalyzer.class);
        analyzersMap.put("ecr", HtmlAnalyzer.class);
        analyzersMap.put("eex", HtmlAnalyzer.class);
        analyzersMap.put("erb", HtmlAnalyzer.class);
        analyzersMap.put("deface", HtmlAnalyzer.class);
        analyzersMap.put("haml", HtmlAnalyzer.class);
        analyzersMap.put("mtml", HtmlAnalyzer.class);
        analyzersMap.put("rtml", HtmlAnalyzer.class);
        analyzersMap.put("vue", HtmlAnalyzer.class);
        analyzersMap.put("phtml", HtmlAnalyzer.class);
        analyzersMap.put("hack", HtmlAnalyzer.class);
        analyzersMap.put("hhi", HtmlAnalyzer.class);
        analyzersMap.put("hbs", HtmlAnalyzer.class);
        analyzersMap.put("handlebars", HtmlAnalyzer.class);

    }

    private void registerR() {
        analyzersMap.put("r", RAnalyzer.class);
        analyzersMap.put("rds", RAnalyzer.class);
        analyzersMap.put("rda", RAnalyzer.class);
        analyzersMap.put("rdata", RAnalyzer.class);
        analyzersMap.put("rd", RAnalyzer.class);
        analyzersMap.put("rsx", RAnalyzer.class);
    }

    private void registerSql() {
        analyzersMap.put("pls", SqlAnalyzer.class);
        analyzersMap.put("bdy", SqlAnalyzer.class);
        analyzersMap.put("fnc", SqlAnalyzer.class);
        analyzersMap.put("pck", SqlAnalyzer.class);
        analyzersMap.put("pkb", SqlAnalyzer.class);
        analyzersMap.put("pks", SqlAnalyzer.class);
        analyzersMap.put("plb", SqlAnalyzer.class);
        analyzersMap.put("plsql", SqlAnalyzer.class);
        analyzersMap.put("prc", SqlAnalyzer.class);
        analyzersMap.put("spc", SqlAnalyzer.class);
        analyzersMap.put("tpb", SqlAnalyzer.class);
        analyzersMap.put("tps", SqlAnalyzer.class);
        analyzersMap.put("trg", SqlAnalyzer.class);
        analyzersMap.put("vw", SqlAnalyzer.class);
        analyzersMap.put("sql", SqlAnalyzer.class);
        analyzersMap.put("cql", SqlAnalyzer.class);
        analyzersMap.put("ddl", SqlAnalyzer.class);
        analyzersMap.put("mysql", SqlAnalyzer.class);
        analyzersMap.put("tab", SqlAnalyzer.class);
        analyzersMap.put("udf", SqlAnalyzer.class);
        analyzersMap.put("viw", SqlAnalyzer.class);

    }

    private void registerJson() {
        // json
        analyzersMap.put("json", JsonAnalyzer.class);
        analyzersMap.put("json5", JsonAnalyzer.class);
        analyzersMap.put("jsonld", JsonAnalyzer.class);
        analyzersMap.put("jsoniq", JsonAnalyzer.class);
        analyzersMap.put("avsc", JsonAnalyzer.class);
        analyzersMap.put("geojson", JsonAnalyzer.class);
        analyzersMap.put("gltf", JsonAnalyzer.class);
        analyzersMap.put("har", JsonAnalyzer.class);
        analyzersMap.put("ice", JsonAnalyzer.class);
        analyzersMap.put("JSON-tmLanguage", JsonAnalyzer.class);
        analyzersMap.put("jsonl", JsonAnalyzer.class);
        analyzersMap.put("mcmeta", JsonAnalyzer.class);
        analyzersMap.put("tfstate", JsonAnalyzer.class);
        analyzersMap.put("tfstate.backup", JsonAnalyzer.class);
        analyzersMap.put("topojson", JsonAnalyzer.class);
        analyzersMap.put("webapp", JsonAnalyzer.class);
        analyzersMap.put("webmanifest", JsonAnalyzer.class);
        analyzersMap.put("yy", JsonAnalyzer.class);
        analyzersMap.put("yyp", JsonAnalyzer.class);
        analyzersMap.put("jsonc", JsonAnalyzer.class);
        analyzersMap.put("sublime-build", JsonAnalyzer.class);
        analyzersMap.put("sublime-commands", JsonAnalyzer.class);
        analyzersMap.put("sublime-completions", JsonAnalyzer.class);
        analyzersMap.put("sublime-keymap", JsonAnalyzer.class);
        analyzersMap.put("sublime-macro", JsonAnalyzer.class);
        analyzersMap.put("sublime-menu", JsonAnalyzer.class);
        analyzersMap.put("sublime-mousemap", JsonAnalyzer.class);
        analyzersMap.put("sublime-project", JsonAnalyzer.class);
        analyzersMap.put("sublime-settings", JsonAnalyzer.class);
        analyzersMap.put("sublime-theme", JsonAnalyzer.class);
        analyzersMap.put("sublime-workspace", JsonAnalyzer.class);
        analyzersMap.put("sublime_metrics", JsonAnalyzer.class);
        analyzersMap.put("sublime_session", JsonAnalyzer.class);
    }

    private void registerXml() {
        // xml
        analyzersMap.put("xml", XmlAnalyzer.class);
        analyzersMap.put("xaml", XmlAnalyzer.class);
        analyzersMap.put("owl", XmlAnalyzer.class);
        analyzersMap.put("adml", XmlAnalyzer.class);
        analyzersMap.put("admx", XmlAnalyzer.class);
        analyzersMap.put("ant", XmlAnalyzer.class);
        analyzersMap.put("axml", XmlAnalyzer.class);
        analyzersMap.put("builds", XmlAnalyzer.class);
        analyzersMap.put("ccproj", XmlAnalyzer.class);
        analyzersMap.put("ccxml", XmlAnalyzer.class);
        analyzersMap.put("clixml", XmlAnalyzer.class);
        analyzersMap.put("cproject", XmlAnalyzer.class);
        analyzersMap.put("cscfg", XmlAnalyzer.class);
        analyzersMap.put("csdef", XmlAnalyzer.class);
        analyzersMap.put("csl", XmlAnalyzer.class);
        analyzersMap.put("csproj", XmlAnalyzer.class);
        analyzersMap.put("ct", XmlAnalyzer.class);
        analyzersMap.put("depproj", XmlAnalyzer.class);
        analyzersMap.put("dita", XmlAnalyzer.class);
        analyzersMap.put("ditamap", XmlAnalyzer.class);
        analyzersMap.put("ditaval", XmlAnalyzer.class);
        analyzersMap.put("dll.config", XmlAnalyzer.class);
        analyzersMap.put("dotsettings", XmlAnalyzer.class);
        analyzersMap.put("filters", XmlAnalyzer.class);
        analyzersMap.put("fsproj", XmlAnalyzer.class);
        analyzersMap.put("fxml", XmlAnalyzer.class);
        analyzersMap.put("glade", XmlAnalyzer.class);
        analyzersMap.put("gml", XmlAnalyzer.class);
        analyzersMap.put("gmx", XmlAnalyzer.class);
        analyzersMap.put("grxml", XmlAnalyzer.class);
        analyzersMap.put("iml", XmlAnalyzer.class);
        analyzersMap.put("ivy", XmlAnalyzer.class);
        analyzersMap.put("jelly", XmlAnalyzer.class);
        analyzersMap.put("jsproj", XmlAnalyzer.class);
        analyzersMap.put("kml", XmlAnalyzer.class);
        analyzersMap.put("launch", XmlAnalyzer.class);
        analyzersMap.put("mdpolicy", XmlAnalyzer.class);
        analyzersMap.put("mjml", XmlAnalyzer.class);
        analyzersMap.put("mod", XmlAnalyzer.class);
        analyzersMap.put("mxml", XmlAnalyzer.class);
        analyzersMap.put("natvis", XmlAnalyzer.class);
        analyzersMap.put("ncl", XmlAnalyzer.class);
        analyzersMap.put("ndproj", XmlAnalyzer.class);
        analyzersMap.put("nproj", XmlAnalyzer.class);
        analyzersMap.put("nuspec", XmlAnalyzer.class);
        analyzersMap.put("odd", XmlAnalyzer.class);
        analyzersMap.put("osm", XmlAnalyzer.class);
        analyzersMap.put("pkgproj", XmlAnalyzer.class);
        analyzersMap.put("pluginspec", XmlAnalyzer.class);
        analyzersMap.put("proj", XmlAnalyzer.class);
        analyzersMap.put("props", XmlAnalyzer.class);
        analyzersMap.put("ps1xml", XmlAnalyzer.class);
        analyzersMap.put("psc1", XmlAnalyzer.class);
        analyzersMap.put("pt", XmlAnalyzer.class);
        analyzersMap.put("rdf", XmlAnalyzer.class);
        analyzersMap.put("resx", XmlAnalyzer.class);
        analyzersMap.put("rss", XmlAnalyzer.class);
        analyzersMap.put("sch", XmlAnalyzer.class);
        analyzersMap.put("scxml", XmlAnalyzer.class);
        analyzersMap.put("sfproj", XmlAnalyzer.class);
        analyzersMap.put("shproj", XmlAnalyzer.class);
        analyzersMap.put("srdf", XmlAnalyzer.class);
        analyzersMap.put("storyboard", XmlAnalyzer.class);
        analyzersMap.put("sublime-snippet", XmlAnalyzer.class);
        analyzersMap.put("targets", XmlAnalyzer.class);
        analyzersMap.put("tml", XmlAnalyzer.class);
        analyzersMap.put("ui", XmlAnalyzer.class);
        analyzersMap.put("urdf", XmlAnalyzer.class);
        analyzersMap.put("ux", XmlAnalyzer.class);
        analyzersMap.put("vbproj", XmlAnalyzer.class);
        analyzersMap.put("vcxproj", XmlAnalyzer.class);
        analyzersMap.put("vsixmanifest", XmlAnalyzer.class);
        analyzersMap.put("vssettings", XmlAnalyzer.class);
        analyzersMap.put("vstemplate", XmlAnalyzer.class);
        analyzersMap.put("vxml", XmlAnalyzer.class);
        analyzersMap.put("wixproj", XmlAnalyzer.class);
        analyzersMap.put("workflow", XmlAnalyzer.class);
        analyzersMap.put("wsdl", XmlAnalyzer.class);
        analyzersMap.put("wsf", XmlAnalyzer.class);
        analyzersMap.put("wxi", XmlAnalyzer.class);
        analyzersMap.put("wxl", XmlAnalyzer.class);
        analyzersMap.put("wxs", XmlAnalyzer.class);
        analyzersMap.put("x3d", XmlAnalyzer.class);
        analyzersMap.put("xacro", XmlAnalyzer.class);
        analyzersMap.put("xib", XmlAnalyzer.class);
        analyzersMap.put("xlf", XmlAnalyzer.class);
        analyzersMap.put("xliff", XmlAnalyzer.class);
        analyzersMap.put("xmi", XmlAnalyzer.class);
        analyzersMap.put("xml.dist", XmlAnalyzer.class);
        analyzersMap.put("xproj", XmlAnalyzer.class);
        analyzersMap.put("xsd", XmlAnalyzer.class);
        analyzersMap.put("xspec", XmlAnalyzer.class);
        analyzersMap.put("xul", XmlAnalyzer.class);
        analyzersMap.put("zcml", XmlAnalyzer.class);
        analyzersMap.put("plist", XmlAnalyzer.class);
        analyzersMap.put("stTheme", XmlAnalyzer.class);
        analyzersMap.put("tmCommand", XmlAnalyzer.class);
        analyzersMap.put("tmLanguage", XmlAnalyzer.class);
        analyzersMap.put("tmPreferences", XmlAnalyzer.class);
        analyzersMap.put("tmSnippet", XmlAnalyzer.class);
        analyzersMap.put("tmTheme", XmlAnalyzer.class);
        analyzersMap.put("xsp-config", XmlAnalyzer.class);
        analyzersMap.put("xpl", XmlAnalyzer.class);
        analyzersMap.put("xproc", XmlAnalyzer.class);
        analyzersMap.put("xquery", XmlAnalyzer.class);
        analyzersMap.put("xq", XmlAnalyzer.class);
        analyzersMap.put("xql", XmlAnalyzer.class);
        analyzersMap.put("xqm", XmlAnalyzer.class);
        analyzersMap.put("xqy", XmlAnalyzer.class);
        analyzersMap.put("xsl", XmlAnalyzer.class);
        analyzersMap.put("xslt", XmlAnalyzer.class);
        analyzersMap.put("thrift", ThriftAnalyzer.class);
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
                if (sourceFileFilter.matches(sourceFile) && !sourceFileFilter.getException()) {
                    overridden = true;
                } else if (sourceFileFilter.matches(sourceFile) && sourceFileFilter.getException()) {
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
