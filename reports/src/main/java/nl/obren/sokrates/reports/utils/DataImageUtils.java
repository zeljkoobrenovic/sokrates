package nl.obren.sokrates.reports.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DataImageUtils {
    public static final String DEVELOPER = "data:image/png;base64,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";
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
            return getImageBase64("/lang/" + map.get(lang.toLowerCase()));
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
        return getLangDataImageDiv(lang, 42, 16, 14, 11, 13);
    }

    public static String getLangDataImageDiv30(String lang) {
        return getLangDataImageDiv(lang, 30, 13, 11, 7, 9);
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
                    "padding: auto; background-color: #f1f1f1; border-radius: 50%; border: 1px solid grey; " +
                    "object-fit: contain; overflow: hidden; color: darkblue; " +
                    "width: " + size + "px; " +
                    "height: " + size + "px; " +
                    "font-size: " + (nameLength <= 3 ? fontSize1 : fontSize2) + "px; font-weight: bold; text-align: center;\">" +
                    "<div style=\"height: " + (nameLength <= 3 ? padding1 : padding2) + "px\"></div>" + lang + "</div>";

        }

    }
}
