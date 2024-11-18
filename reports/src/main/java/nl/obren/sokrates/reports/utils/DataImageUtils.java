package nl.obren.sokrates.reports.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DataImageUtils {
    public static final String DEVELOPER = "data:image/png;base64,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";
    public static final String TEAM = "data:image/png;base64,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";
    public static final String DATA_IMAGE_PREFIX = "data:image/png;base64,";
    private static final Map<String, String> map = new HashMap<>();

    static {
        map.put("java", "Java.png");
        map.put("cs", "C_.png");
        map.put("c", "C.png");
        map.put("h", "C.png");
        map.put("js", "JavaScript.png");
        map.put("cjs", "commonjs.png");
        map.put("py", "Python.png");
        map.put("py3", "Python.png");
        map.put("pyx", "Python.png");
        map.put("sql", "SQL.png");
        map.put("php", "PHP.png");
        map.put("php3", "PHP.png");
        map.put("php5", "PHP.png");
        map.put("inc", "PHP.png");
        map.put("vb", "Visual_Basic.png");
        map.put("asm", "Assembly_language.png");
        map.put("go", "Go.png");
        map.put("pas", "Delphi_Object_Pascal.png");
        map.put("rb", "Ruby.png");
        map.put("erb", "Ruby.png");
        map.put("gemspec", "Ruby.png");
        map.put("graphql", "graphql.png");
        map.put("graphqls", "graphql.png");
        map.put("gql", "graphql.png");
        map.put("clj", "clojure.png");
        map.put("pp", "puppet.png");
        map.put("dart", "dart.png");
        map.put("cpp", "cpp.png");
        map.put("cxx", "cpp.png");
        map.put("cc", "cpp.png");
        map.put("hpp", "cpp.png");
        map.put("hh", "cpp.png");
        map.put("hxx", "cpp.png");
        map.put("thrift", "thrift.png");
        map.put("vue", "vue.png");
        map.put("dockerfile", "docker.png");
        map.put("mustache", "mustache.png");
        map.put("pm", "Perl.png");
        map.put("pl", "Perl.png");
        map.put("ftl", "freemarker.png");
        map.put("r", "R.png");
        map.put("m", "Objective_C.png");
        map.put("mm", "Objective_C.png");
        map.put("sh", "shell.png");
        map.put("bash", "shell.png");
        map.put("ksh", "shell.png");
        map.put("zsh", "shell.png");
        map.put("bat", "shell.png");
        map.put("rs", "rust.png");
        map.put("dhall", "dhall.png");
        map.put("rdf", "rdf.png");
        map.put("cmake", "cmake.png");
        map.put("adoc", "adoc.png");
        map.put("asciidoc", "adoc.png");
        map.put("g", "antlr.png");
        map.put("g3", "antlr.png");
        map.put("g3l", "antlr.png");
        map.put("g3p", "antlr.png");
        map.put("g3lp", "antlr.png");
        map.put("g3t", "antlr.png");
        map.put("g4", "antlr.png");
        map.put("g4l", "antlr.png");
        map.put("g4p", "antlr.png");
        map.put("g4lp", "antlr.png");
        map.put("g4t", "antlr.png");
        map.put("awk", "awk.png");
        map.put("dockerignore", "docker.png");

        map.put("groovy", "Groovy.png");
        map.put("gvy", "Groovy.png");
        map.put("gy", "Groovy.png");
        map.put("gsh", "Groovy.png");
        map.put("gradle", "gradle.png");
        map.put("bzl", "bazel.png");
        map.put(".babelrc", "babel.png");
        map.put(".babelversion", "babel.png");
        map.put(".babelrc.js", "babel.png");

        map.put("swift", "Swift.png");
        map.put("perl", "Perl.png");
        map.put("hql", "hive.png");
        map.put("kt", "Kotlin.png");
        map.put("kts", "Kotlin.png");
        map.put("jsx", "react.png");
        map.put("tsx", "react.png");
        map.put("htm", "html.png");
        map.put("html", "html.png");
        map.put("xhtml", "html.png");
        map.put("css", "css.png");
        map.put("jss", "jss.png");
        map.put("less", "less.png");
        map.put("scss", "sass.png");
        map.put("tf", "terraform.png");
        map.put("tfstate", "terraform.png");
        map.put("tfvars", "terraform.png");
        map.put("ts", "ts.png");
        map.put("yaml", "yaml.png");
        map.put("yml", "yaml.png");
        map.put("scala", "scala.png");
        map.put("jsp", "jsp.png");
        map.put("sls", "saltstack.png");
        map.put("hbs", "handlebars.png");
        map.put("handlebars", "handlebars.png");
        map.put("rst", "rest.png");
        map.put("sbt", "sbt.png");
        map.put("ipynb", "Jupyter.png");
        map.put("svg", "svg.png");
        map.put("md", "md.png");
        map.put("markdown", "md.png");
        map.put("json", "json.png");
        map.put("xml", "xml.png");
        map.put("avsc", "avro.png");
        map.put("xib", "xcode.png");
        map.put("jinja", "jinja.png");
        map.put("jsonnet", "jsonnet.png");
        map.put("json5", "json5.png");
        map.put("applescript", "applescript.png");
        map.put("erl", "erlang.png");
        map.put("hrl", "erlang.png");
        map.put("escript", "erlang.png");
        map.put("exs", "elixir.png");
        map.put("ex", "elixir.png");
        map.put("es", "es.png");
        map.put("gitignore", "git.png");
        map.put("nomad", "nomad.png");
        map.put("unity", "unity.png");
        map.put("hack", "hack.png");
        map.put("toml", "toml.png");
        map.put("ps1", "powershell.png");
        map.put("ps1xml", "powershell.png");
        map.put("psc1", "powershell.png");
        map.put("psd1", "powershell.png");
        map.put("psm1", "powershell.png");
        map.put("pssc", "powershell.png");
        map.put("psrc", "powershell.png");
        map.put("cdxml", "powershell.png");
        map.put("d", "d.png");
        map.put("prefab", "unity.png");
        map.put("cu", "nvidia.png");
        map.put("cuh", "nvidia.png");
        map.put("bazel", "bazel.png");
        map.put("xlf", "xliff.png");
        map.put("twig", "twig.png");
        map.put("pug", "pug.png");
        map.put("cls", "cls.png");
        map.put("phtml", "phtml.png");

        map.put(".github", "github.png");
        map.put(".gh-pages", "github.png");
        map.put(".gitignore", "git.png");
        map.put(".gitmodules", "git.png");
        map.put(".gitattributes", "git.png");
        map.put(".gitconfig", "git.png");
        map.put(".gitreview", "git.png");
        map.put(".githooks", "git.png");
        map.put(".gitkeep", "git.png");
        map.put(".git-blame-ignore-revs", "git.png");
        map.put(".husky", "git.png");
        map.put(".huskyrc", "git.png");
        map.put(".idea", "jetbrains.png");
        map.put(".vscode", "vscode.png");
        map.put(".mvn", "maven.png");
        map.put(".m2", "maven.png");
        map.put(".jenkins", "jenkins.png");
        map.put(".jenkins.groovy", "jenkins.png");
        map.put(".travis", "travis.png");
        map.put(".travis.yml", "travis.png");
        map.put(".travis.yaml", "travis.png");
        map.put(".travis-build", "travis.png");
        map.put(".travis.settings.xml", "travis.png");
        map.put(".travis-mvn-settings.xml", "travis.png");
        map.put(".yarn", "yarn.png");
        map.put(".yarnrc", "yarn.png");
        map.put(".docker", "docker.png");
        map.put(".dockerignore", "docker.png");
        map.put(".editorconfig", "editorconfig.png");
        map.put(".zuul.d", "zuul.png");
        map.put(".circleci", "circle-cli.png");
        map.put(".settings", "eclipse.png");
        map.put(".eclipse", "eclipse.png");
        map.put(".eclipse.templates", "eclipse.png");
        map.put(".classpath", "Java.png");
        map.put(".java-version", "Java.png");
        map.put(".jvmopts", "Java.png");
        map.put(".npmignore", "npm.png");
        map.put(".npmrc", "npm.png");
        map.put(".nvmrc", "node_js.png");
        map.put(".node-version", "node_js.png");
        map.put(".python-version", "Python.png");
        map.put(".pydevproject", "Python.png");
        map.put(".pylintrc", "Python.png");
        map.put(".eslintrc", "eslint.png");
        map.put(".eslintrc.json", "eslint.png");
        map.put(".eslintrc.yml", "eslint.png");
        map.put(".eslintrc.yaml", "eslint.png");
        map.put(".eslintignore", "eslint.png");
        map.put(".eslintrc.js", "eslint.png");
        map.put(".eslintignore-es5", "eslint.png");
        map.put(".eslintignore-es6", "eslint.png");
        map.put(".eslintrc-es5", "eslint.png");
        map.put(".eslintrc-es6", "eslint.png");
        map.put(".eslintrc-common.yml", "eslint.png");
        map.put(".eslintrc-common.yaml", "eslint.png");
        map.put(".gitlab-ci.yml", "gitlab.png");
        map.put(".bazelrc", "bazel.png");
        map.put(".bazelci", "bazel.png");
        map.put(".bazelversion", "bazel.png");
        map.put(".buildkite", "buildkite.png");
        map.put(".golangci.yml", "Go.png");
        map.put(".golangci.yaml", "Go.png");
        map.put(".storybook", "storybook.png");
        map.put(".terraform", "terraform.png");
        map.put(".terraform-version", "terraform.png");
        map.put(".terraform.lock.hcl", "terraform.png");
        map.put(".phraseapp.yml", "phaseapp.png");
        map.put(".phraseapp.yaml", "phaseapp.png");
        map.put(".phrase.yml", "phaseapp.png");
        map.put(".phrase.yaml", "phaseapp.png");
        map.put(".pre-commit-config.yml", "pre-commit.png");
        map.put(".pre-commit-config.yaml", "pre-commit.png");
        map.put(".nuget", "nuget.png");
        map.put(".sbtserver", "sbt.png");
        map.put(".sbtopts", "sbt.png");
        map.put(".ipynb_checkpoints", "Jupyter.png");
        map.put(".swiftpm", "Swift.png");
        map.put(".swift-version", "Swift.png");
        map.put(".gradle", "gradle.png");
        map.put(".vs", "visualstudio.png");
        map.put(".devcontainer", "visualstudio.png");
        map.put(".DS_Store", "macos.png");
        map.put(".snyk", "snyk.png");
        map.put(".ruby-version", "Ruby.png");
        map.put(".prettierrc", "prettier.png");
        map.put(".prettierignore", "prettier.png");
        map.put(".prettierrc.js", "prettier.png");
        map.put(".prettierrc.json", "prettier.png");
        map.put(".scalafmt.conf", "scala.png");
        map.put(".helmignore", "helm.png");
        map.put(".swiftlint.yml", "Swift.png");
        map.put(".openapi", "openapi.png");
        map.put(".openapi-generator", "openapi.png");
        map.put(".jekyll-cache", "jekyll.png");
        map.put(".nojekyll", "jekyll.png");
    }

    public static String getLangDataImage(String lang) {
        if (map.containsKey(lang.toLowerCase())) {
            return getImageBase64("/lang/" + map.get(lang.toLowerCase().trim()));
        }

        return null;
    }

    public static String getImageBase64(String imageResourcePath) {
        InputStream in = DataImageUtils.class.getResourceAsStream(imageResourcePath);
        if (in != null) {
            try {
                byte[] fileContent = in.readAllBytes();
                return DATA_IMAGE_PREFIX + Base64.getEncoder().encodeToString(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getLangDataImageDiv64(String lang) {
        return getLangDataImageDiv(lang, 60, 20, 16, 11, 13);
    }

    public static String getLangDataImageDiv42(String lang) {
        return getLangDataImageDiv(lang, 42, 16, 14, 12, 13);
    }
    public static String getLangDataImageDiv36(String lang) {
        return getLangDataImageDiv(lang, 36, 14, 12, 9, 11);
    }

    public static String getLangDataImageDiv30(String lang) {
        return getLangDataImageDiv(lang, 30, 13, 11, 7, 9);
    }
    public static String getLangDataImageDiv28(String lang) {
        return getLangDataImageDiv(lang, 28, 13, 10, 6, 8);
    }

    public static String getLangDataImageDiv(String lang, int size, int fontSize1, int fontSize2, int padding1, int padding2) {
        String image = DataImageUtils.getLangDataImage(lang);
        if (image != null) {
            return "<img title='" + lang + "' style=\"margin-right: 3px; vertical-align: top; background-color: #f1f1f1; border-radius: 50%; border: 1px solid lightgrey; width: " + size + "px; height: " + size + "px; object-fit: contain;\" src=\"" +
                    image + "\">";
        } else {
            int nameLength = lang.length();
            return "<div title='" + lang + "' style=\"" +
                    "margin-right: 3px; display: inline-block; vertical-align: top; " +
                    "padding: auto; background-color: #D6E4E1; border-radius: 50%; border: 1px solid grey; " +
                    "object-fit: contain; overflow: hidden; color: darkblue; " +
                    "width: " + size + "px; " +
                    "height: " + size + "px; " +
                    "font-size: " + (nameLength <= 3 ? fontSize1 : fontSize2) + "px; font-weight: bold; text-align: center;\">" +
                    "<div style=\"height: " + (nameLength <= 3 ? padding1 : padding2) + "px\"></div>" + lang + "</div>";

        }

    }
}
