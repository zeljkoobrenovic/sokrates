package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;
import nl.obren.sokrates.sourcecode.lang.clojure.ClojureLangAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cpp.CAnalyzer;
import nl.obren.sokrates.sourcecode.lang.cpp.CppAnalyzer;
import nl.obren.sokrates.sourcecode.lang.csharp.CSharpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.css.CssAnalyzer;
import nl.obren.sokrates.sourcecode.lang.go.GoLangAnalyzer;
import nl.obren.sokrates.sourcecode.lang.groovy.GroovyAnalyzer;
import nl.obren.sokrates.sourcecode.lang.html.HtmlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.java.JavaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.js.JavaScriptAnalyzer;
import nl.obren.sokrates.sourcecode.lang.json.JsonAnalyzer;
import nl.obren.sokrates.sourcecode.lang.jsp.JspAnalyzer;
import nl.obren.sokrates.sourcecode.lang.perl.PerlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.php.PhpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.python.PythonAnalyzer;
import nl.obren.sokrates.sourcecode.lang.ruby.RubyAnalyzer;
import nl.obren.sokrates.sourcecode.lang.sass.SassAnalyzer;
import nl.obren.sokrates.sourcecode.lang.scala.ScalaAnalyzer;
import nl.obren.sokrates.sourcecode.lang.vb.VisualBasicAnalyzer;
import nl.obren.sokrates.sourcecode.lang.xml.XmlAnalyzer;
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
        analyzersMap.put("c", CAnalyzer.class);
        analyzersMap.put("h", CAnalyzer.class);
        analyzersMap.put("cpp", CppAnalyzer.class);
        analyzersMap.put("cc", CppAnalyzer.class);
        analyzersMap.put("hpp", CppAnalyzer.class);
        analyzersMap.put("m", CppAnalyzer.class);
        analyzersMap.put("php", PhpAnalyzer.class);
        analyzersMap.put("py", PythonAnalyzer.class);
        analyzersMap.put("js", JavaScriptAnalyzer.class);
        analyzersMap.put("ts", JavaScriptAnalyzer.class);
        analyzersMap.put("scala", ScalaAnalyzer.class);
        analyzersMap.put("html", HtmlAnalyzer.class);
        analyzersMap.put("htm", HtmlAnalyzer.class);
        analyzersMap.put("xml", XmlAnalyzer.class);
        analyzersMap.put("pl", PerlAnalyzer.class);
        analyzersMap.put("pm", PerlAnalyzer.class);
        analyzersMap.put("rb", RubyAnalyzer.class);
        analyzersMap.put("groovy", GroovyAnalyzer.class);
        analyzersMap.put("css", CssAnalyzer.class);
        analyzersMap.put("sass", SassAnalyzer.class);
        analyzersMap.put("json", JsonAnalyzer.class);
        analyzersMap.put("gsp", JspAnalyzer.class);
        analyzersMap.put("jsp", JspAnalyzer.class);
        analyzersMap.put("jsx", JavaScriptAnalyzer.class);
        analyzersMap.put("vb", VisualBasicAnalyzer.class);
        analyzersMap.put("bas", VisualBasicAnalyzer.class);
        analyzersMap.put("cls", VisualBasicAnalyzer.class);
        analyzersMap.put("ctl", VisualBasicAnalyzer.class);
        analyzersMap.put("clj", ClojureLangAnalyzer.class);
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

    public LanguageAnalyzer getLanguageAnalyzer(SourceFile sourceFile) {
        try {
            Class aClass = analyzersMap.get(getAnalyzerKey(sourceFile));
            if (aClass != null) {
                return (LanguageAnalyzer) aClass.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error");
        }

        return new UnknownLanguageAnalyzer();
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
