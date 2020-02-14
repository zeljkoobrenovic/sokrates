/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

public class ExtensionGroupExtractor {
    private static final Log LOG = LogFactory.getLog(ExtensionGroupExtractor.class);

    // based on https://raw.githubusercontent.com/github/linguist/master/lib/linguist/languages.yml
    private static final List<String> knownSourceExtensions = Arrays.asList(
            "inc", "m", "fcgi", "cls", "pro", "sql", "mod", "l", "x", "sch", "fs", "gs", "gml", "t", "asc", "i", "h",
            "cl", "v", "d", "f", "for", "fr", "php", "ms", "ncl", "pl", "cgi", "pm", "spec", "as", "vhost", "asm",
            "asy", "bb", "b", "bf", "cs", "cake", "cp", "hh", "re", "w", "ch", "lisp", "lsp", "ecl", "brd", "emacs",
            "emacsdesktop", "es", "fx", "dsp", "g", "gd", "tst", "md", "frag", "shader", "gn", "gitconfig", "p", "cfg",
            "workflow", "st", "q", "properties", "gitignore", "ice", "yy", "j", "ls", "m4", "mqh", "mask", "nb", "moo",
            "s", "nl", "n", "odin", "mm", "plist", "ddl", "prc", "pp", "pod", "rpy", "r", "rno", "rs", "01", "1in",
            "1m", "1x", "02", "03", "3in", "3m", "3p", "3pm", "3qt", "3x", "04", "05", "06", "07", "08", "09", "man",
            "mdoc", "pluginspec", "srt", "sls", "sc", "tsx", "toc", "ts", "vba", "cproject", "dircolors", "nanorc",
            "bsl", "os", "4dm", "abap", "abnf", "ash", "ampl", "g4", "apib", "apl", "dyalog", "asn", "asn1", "asp",
            "asax", "ascx", "ashx", "asmx", "aspx", "axd", "dats", "hats", "sats", "adb", "ada", "ads", "afm", "agda",
            "als", "OutJob", "PcbDoc", "PrjPCB", "SchDoc", "angelscript", "apacheconf", "htaccess", "agc",
            "applescript", "scpt", "arc", "asciidoc", "adoc", "aj", "a51", "nasm", "aug", "ahk", "ahkl", "au3", "awk",
            "auk", "gawk", "mawk", "nawk", "bal", "bat", "cmd", "befunge", "bib", "bibtex", "bison", "blade",
            "bladephp", "decls", "bmx", "bsv", "boo", "brs", "c", "cats", "idc", "csx", "cpp", "c++", "cc", "cxx",
            "h++", "hpp", "hxx", "inl", "ino", "ipp", "tcc", "tpp", "c-objdump", "chs", "clp", "cmake", "cmakein",
            "cob", "cbl", "ccp", "cobol", "cpy", "dae", "cson", "css", "csv", "cabal", "capnp", "mss", "ceylon",
            "chpl", "ck", "cirru", "clw", "icl", "dcl", "click", "clj", "boot", "cl2", "cljc", "cljs", "cljshl",
            "cljscm", "cljx", "hic", "soy", "conllu", "conll", "coffee", "_coffee", "cjsx", "iced", "cfm", "cfml",
            "cfc", "asd", "ny", "podsl", "sexp", "cwl", "cps", "coq", "cppobjdump", "c++-objdump", "c++objdump",
            "cpp-objdump", "cxx-objdump", "creole", "cr", "orc", "udo", "csd", "sco", "cu", "cuh", "cy", "pyx", "pxd",
            "pxi", "di", "d-objdump", "com", "dm", "zone", "arpa", "darcspatch", "dpatch", "dart", "dwl", "dhall",
            "diff", "patch", "dockerfile", "djs", "dylan", "dyl", "intr", "lid", "E", "ebnf", "eclxml", "ejs", "eml",
            "mbox", "eq", "eb", "epj", "editorconfig", "edc", "e", "ex", "exs", "elm", "abbrev_defs", "gnus",
            "spacemacs", "viper", "el", "em", "emberscript", "erl", "appsrc", "escript", "hrl", "xrl", "yrl", "fsi",
            "fsx", "fst", "flf", "flux", "factor", "factor-boot-rc", "factor-rc", "fy", "fancypack", "fan", "eamfs",
            "fth", "4th", "forth", "frt", "f90", "f03", "f08", "f77", "f95", "fpp", "ftl", "cnc", "gco", "gcode",
            "gaml", "gms", "gap", "gi", "gdb", "gdbinit", "glsl", "fp", "frg", "fsh", "fshader", "geo", "geom",
            "glslf", "glslv", "gshader", "tesc", "tese", "vert", "vrx", "vsh", "vshader", "gni", "kid", "ebuild",
            "eclass", "gbr", "gbl", "gbo", "gbp", "gbs", "gko", "gpb", "gpt", "gtl", "gto", "gtp", "gts", "po", "pot",
            "feature", "gitattributes", "gitmodules", "glf", "bdf", "gp", "gnu", "gnuplot", "plot", "plt", "go",
            "golo", "gst", "gsx", "vark", "grace", "gradle", "gf", "graphql", "gql", "graphqls", "dot", "gv", "groovy",
            "grt", "gtpl", "gvy", "gsp", "hcl", "tf", "tfvars", "hlsl", "cginc", "fxh", "hlsli", "html", "htm",
            "htmlhl", "xht", "xhtml", "jinja", "jinja2", "mustache", "njk", "ecr", "eex", "erb", "erbdeface", "phtml",
            "cshtml", "razor", "http", "hxml", "hack", "hhi", "haml", "hamldeface", "handlebars", "hbs", "hb", "hs",
            "hs-boot", "hsc", "hx", "hxsl", "hql", "hc", "hy", "dlm", "ipf", "ini", "dof", "lektorproject", "prefs",
            "irclog", "weechatlog", "idr", "lidr", "atomignore", "babelignore", "bzrignore", "coffeelintignore",
            "cvsignore", "dockerignore", "eslintignore", "nodemonignore", "npmignore", "prettierignore",
            "stylelintignore", "vscodeignore", "ni", "i7x", "iss", "io", "ik", "thy", "ijs", "flex", "jflex", "json",
            "avsc", "geojson", "gltf", "har", "JSON-tmLanguage", "jsonl", "mcmeta", "tfstate", "tfstatebackup",
            "topojson", "webapp", "webmanifest", "yyp", "arcconfig", "htmlhintrc", "tern-config", "tern-project",
            "watchmanconfig", "sublime-build", "sublime-commands", "sublime-completions", "sublime-keymap",
            "sublime-macro", "sublime-menu", "sublime-mousemap", "sublime-project", "sublime-settings", "sublime-theme",
            "sublime-workspace", "sublime_metrics", "sublime_session", "babelrc", "eslintrcjson", "jscsrc", "jshintrc",
            "jslintrc", "json5", "jsonld", "jq", "jsx", "java", "jsp", "js", "_js", "bones", "cjs", "es6", "jake",
            "jsb", "jscad", "jsfl", "jsm", "jss", "mjs", "njs", "pac", "sjs", "ssjs", "xsjs", "xsjslib", "jserb",
            "jison", "jisonlex", "ol", "iol", "jsonnet", "libsonnet", "jl", "ipynb", "krl", "kicad_pcb", "kicad_mod",
            "kicad_wks", "kit", "kt", "ktm", "kts", "lfe", "ll", "lol", "lsl", "lslp", "lvproj", "lasso", "las",
            "lasso8", "lasso9", "latte", "lean", "hlean", "less", "lex", "ly", "ily", "ld", "lds", "liquid", "lagda",
            "litcoffee", "lhs", "_ls", "xm", "xi", "lgt", "logtalk", "lookml", "modellkml", "viewlkml", "lua", "nse",
            "p8", "pd_lua", "rbxs", "rockspec", "wlua", "mumps", "matlab", "mcr", "mlir", "mq4", "mq5", "mtml", "muf",
            "mak", "make", "mk", "mkfile", "mako", "mao", "markdown", "mdown", "mdwn", "mdx", "mkd", "mkdn", "mkdown",
            "ronn", "workbook", "marko", "mathematica", "cdf", "ma", "mt", "nbp", "wl", "wlt", "maxpat", "maxhelp",
            "maxproj", "mxt", "pat", "mediawiki", "wiki", "metal", "minid", "druby", "duby", "mirah", "mo", "i3", "ig",
            "m3", "mg", "mms", "mmk", "monkey", "monkey2", "moon", "x68", "muse", "myt", "npmrc", "nsi", "nsh", "ne",
            "nearley", "axs", "axi", "axserb", "axierb", "nlogo", "nf", "nginxconf", "nim", "nimcfg", "nimble",
            "nimrod", "nims", "ninja", "nit", "nix", "nu", "numpy", "numpyw", "numsc", "ml", "eliom", "eliomi",
            "ml4", "mli", "mll", "mly", "objdump", "sj", "omgrofl", "opa", "opal", "rego", "opencl", "scad", "fea",
            "org", "ox", "oxh", "oxo", "oxygene", "oz", "p4", "aw", "ctp", "php3", "php4", "php5", "phps", "phpt",
            "php_cs", "php_csdist", "pls", "bdy", "fnc", "pck", "pkb", "pks", "plb", "plsql", "spc", "tpb", "tps",
            "trg", "vw", "pgsql", "pov", "pan", "psc", "parrot", "pasm", "pir", "pas", "dfm", "dpr", "lpr", "pascal",
            "pwn", "sma", "pep", "al", "perl", "ph", "plx", "psgi", "pic", "chem", "pkl", "pig", "pike", "pmod",
            "pod6", "pogo", "pony", "pcss", "postcss", "ps", "eps", "epsi", "pfa", "pbt", "sra", "sru", "srw", "ps1",
            "psd1", "psm1", "prisma", "pde", "prolog", "yap", "spin", "proto", "pub", "jade", "pug", "pd", "pb",
            "pbi", "purs", "py", "gyp", "gypi", "lmi", "py3", "pyde", "pyi", "pyp", "pyt", "pyw", "smk", "tac",
            "wsgi", "xpy", "gclient", "pytb", "qml", "qbs", "pri", "rd", "rsx", "Rprofile", "raml", "rdoc", "rbbas",
            "rbfrm", "rbmnu", "rbres", "rbtbar", "rbuistate", "rexx", "pprx", "rex", "rhtml", "rmd", "rnh", "rkt",
            "rktd", "rktl", "scrbl", "rl", "6pl", "6pm", "nqp", "p6", "p6l", "p6m", "pl6", "pm6", "rsc", "raw",
            "inputrc", "rei", "reb", "r2", "r3", "rebol", "red", "reds", "cw", "regexp", "regex", "rsh", "rtf",
            "ring", "riot", "robot", "roff", "me", "nr", "tmac", "rg", "rb", "builder", "eye", "gemspec", "god",
            "jbuilder", "mspec", "podspec", "rabl", "rake", "rbi", "rbuild", "rbw", "rbx", "ru", "ruby", "thor",
            "watchr", "irbrc", "pryrc", "rsin", "sas", "scss", "smt2", "smt", "sparql", "rq", "sqf", "hqf", "cql",
            "mysql", "tab", "udf", "viw", "db2", "ston", "svg", "sage", "sagews", "sass", "scala", "kojo", "sbt",
            "scaml", "scm", "sld", "sps", "ss", "sci", "sce", "self", "sh", "bash", "bats", "command", "ksh", "shin",
            "tmux", "tool", "zsh", "bash_aliases", "bash_history", "bash_logout", "bash_profile", "bashrc", "cshrc",
            "login", "profile", "zlogin", "zlogout", "zprofile", "zshenv", "zshrc", "sh-session", "shen", "sl", "slim",
            "cocci", "smali", "tpl", "sp", "sfd", "nut", "stan", "ML", "fun", "sig", "sml", "bzl", "do", "ado", "doh",
            "ihlp", "mata", "matah", "sthlp", "styl", "sss", "scd", "svelte", "swift", "sv", "svh", "vh", "8xp", "8xk",
            "8xktxt", "8xptxt", "tla", "toml", "txl", "tcl", "adp", "tm", "tcsh", "csh", "tex", "aux", "bbx", "cbx",
            "dtx", "ins", "lbx", "ltx", "mkii", "mkiv", "mkvi", "sty", "tea", "texinfo", "texi", "txi", "txt", "no",
            "textile", "thrift", "tu", "ttl", "twig", "tl", "upc", "anim", "asset", "mat", "meta", "prefab", "unity",
            "uno", "uc", "ur", "urs", "bas", "frm", "frx", "vbs", "vcl", "vhdl", "vhd", "vhf", "vhi", "vho", "vhs",
            "vht", "vhw", "vala", "vapi", "veo", "snip", "snippet", "snippets", "vim", "vmb", "gvimrc", "nvimrc",
            "vimrc", "vb", "vbhtml", "volt", "vue", "mtl", "obj", "owl", "wast", "wat", "webidl", "vtt", "wgetrc",
            "reg", "wlk", "xbm", "xpm", "x10", "xc", "XCompose", "xml", "adml", "admx", "ant", "axml", "builds",
            "ccproj", "ccxml", "clixml", "cscfg", "csdef", "csl", "csproj", "ct", "depproj", "dita", "ditamap",
            "ditaval", "dllconfig", "dotsettings", "filters", "fsproj", "fxml", "glade", "gmx", "grxml", "iml", "ivy",
            "jelly", "jsproj", "kml", "launch", "mdpolicy", "mjml", "mxml", "natvis", "ndproj", "nproj", "nuspec",
            "odd", "osm", "pkgproj", "proj", "props", "ps1xml", "psc1", "pt", "rdf", "resx", "rss", "scxml", "sfproj",
            "shproj", "srdf", "storyboard", "sublime-snippet", "targets", "tml", "ui", "urdf", "ux", "vbproj",
            "vcxproj", "vsixmanifest", "vssettings", "vstemplate", "vxml", "wixproj", "wsdl", "wsf", "wxi", "wxl",
            "wxs", "x3d", "xacro", "xaml", "xib", "xlf", "xliff", "xmi", "xmldist", "xproj", "xsd", "xspec", "xul",
            "zcml", "classpath", "project", "stTheme", "tmCommand", "tmLanguage", "tmPreferences", "tmSnippet",
            "tmTheme", "xsp-config", "xspmetadata", "xpl", "xproc", "xquery", "xq", "xql", "xqm", "xqy", "xs", "xslt",
            "xsl", "xojo_code", "xojo_menu", "xojo_report", "xojo_script", "xojo_toolbar", "xojo_window", "xtend",
            "yml", "mir", "reek", "rviz", "sublime-syntax", "syntax", "yaml", "yaml-tmlanguage", "yamlsed", "ymlmysql",
            "clang-format", "clang-tidy", "gemrc", "yang", "yar", "yara", "yasnippet", "y", "yacc", "zap", "xzap",
            "zil", "mud", "zeek", "bro", "zs", "zep", "zig", "zimpl", "zmpl", "zpl", "curlrc", "desktop", "desktopin",
            "dir_colors", "ec", "eh", "edn", "fish", "mrc", "mcfunction", "mu", "nc", "ooc", "rst", "rest", "resttxt",
            "rsttxt", "sed", "wdl", "wisp", "prg", "prw"
    );
    private static final List<String> knownBinaryExtensions = Arrays.asList("3ds", "3g2", "3gp", "7z", "a", "aac", "adp",
            "ai", "aif", "aiff", "alz", "ape", "apk", "ar", "arj", "asf", "au", "avi", "bak", "bh", "bin", "bk", "bmp",
            "btif", "bz2", "bzip2", "cab", "caf", "cgm", "class", "cmx", "cpio", "cr2", "csv", "cur", "dat", "deb", "dex",
            "djvu", "dll", "dmg", "dng", "doc", "docm", "docx", "dot", "dotm", "dra", "DS_Store", "dsk", "dts", "dtshd",
            "dvb", "dwg", "dxf", "ecelp4800", "ecelp7470", "ecelp9600", "egg", "eol", "eot", "epub", "exe", "f4v", "fbs",
            "fh", "fla", "flac", "fli", "flv", "fpx", "fst", "fvt", "g3", "gif", "graffle", "gz", "gzip", "h261", "h263",
            "h264", "ico", "ief", "img", "ipa", "iso", "jar", "jpeg", "jpg", "jpgv", "jpm", "jxr", "key", "ktx", "lha",
            "lvp", "lz", "lzh", "lzma", "lzo", "m3u", "m4a", "m4v", "mar", "mdi", "mht", "mid", "midi", "mj2", "mka",
            "mkv", "mmr", "mng", "mobi", "mov", "movie", "mp3", "mp4", "mp4a", "mpeg", "mpg", "mpga", "mxu", "nef", "npx",
            "numbers", "o", "oga", "ogg", "ogv", "otf", "pages", "pbm", "pcx", "pdf", "pea", "pgm", "pic", "png", "pnm",
            "pot", "potm", "potx", "ppa", "ppam", "ppm", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "psd", "pya", "pyc",
            "pyo", "pyv", "qt", "rar", "ras", "raw", "rgb", "rip", "rlc", "rmf", "rmvb", "rtf", "rz", "s3m", "s7z", "scpt",
            "sgi", "shar", "sil", "slk", "smv", "so", "sub", "swf", "tar", "tbz", "tbz2", "tga", "tgz", "thmx", "tif",
            "tiff", "tlz", "ttc", "ttf", "txz", "udf", "uvh", "uvi", "uvm", "uvp", "uvs", "uvu", "viv", "vob",
            "war", "wav", "wax", "wbmp", "wdp", "weba", "webm", "webp", "whl", "wim", "wm", "wma", "wmv", "wmx", "woff",
            "woff2", "wvx", "xbm", "xif", "xla", "xlam", "xls", "xlsb", "xlsm", "xlsx", "xlt", "xltm", "xltx", "xm", "xmind",
            "xpi", "xpm", "xwd", "xz", "z", "zip", "zipx");

    private static final List<String> knownIgnorableExtension = Arrays.asList(
            "ds_store", "iml", "ser");

    private Map<String, ExtensionGroup> extensionsMap = new HashMap<>();

    public static boolean isKnownSourceCodeExtension(String extension) {
        return LanguageAnalyzerFactory.getInstance().getAnalyzersMap().containsKey(extension.toLowerCase())
                || knownSourceExtensions.contains(extension.toLowerCase());
    }

    public static boolean isKnownBinaryExtension(String extension) {
        return knownBinaryExtensions.contains(extension.toLowerCase());
    }

    public static boolean isKnownIgnorableExtension(String extension) {
        return knownIgnorableExtension.contains(extension.toLowerCase());
    }

    public void extractExtensionsInfo(File root) {
        if (root.isDirectory()) {
            for (File file : root.listFiles()) {
                extractExtensionsInfo(file);
            }
        } else {
            updateExtensionInfo(root);
        }
    }

    private void updateExtensionInfo(File file) {
        String extension = FilenameUtils.getExtension(file.getPath());
        if (!extension.isEmpty() && !isKnownBinaryExtension(extension) && !isKnownIgnorableExtension(extension)) {
            updateMap(file, extension);
        }
    }

    private void updateMap(File file, String extension) {
        if (!file.exists()) return;
        if (extensionsMap.containsKey(extension)) {
            ExtensionGroup extensionGroup = extensionsMap.get(extension);
            extensionGroup.setNumberOfFiles(extensionGroup.getNumberOfFiles() + 1);
            extensionGroup.setTotalSizeInBytes((int) (extensionGroup.getTotalSizeInBytes() + FileUtils.sizeOf(file)));
        } else {
            int fileSizeInBytes = getFileSizeInBytes(file);
            if (fileSizeInBytes > 0) {
                ExtensionGroup extensionGroup = new ExtensionGroup(extension);
                extensionGroup.setNumberOfFiles(1);
                extensionGroup.setTotalSizeInBytes(fileSizeInBytes);
                extensionsMap.put(extension, extensionGroup);
            }
        }
    }

    private int getFileSizeInBytes(File file) {
        try {
            return (int) FileUtils.sizeOf(file);
        } catch (IllegalArgumentException e) {
            LOG.error(e);
        }
        return 0;
    }

    public List<ExtensionGroup> getExtensionsList() {
        List<ExtensionGroup> list = new ArrayList<>();
        extensionsMap.keySet().forEach(key -> {
            ExtensionGroup extensionGroup = extensionsMap.get(key);
            list.add(extensionGroup);
        });
        sort(list);

        return list;
    }

    private void sort(List<ExtensionGroup> list) {
        Collections.sort(list, (o1, o2) -> {
            if (isKnownSourceCodeExtension(o1.getExtension()) && !isKnownSourceCodeExtension(o2.getExtension())) {
                return -1;
            } else if (!isKnownSourceCodeExtension(o1.getExtension()) && isKnownSourceCodeExtension(o2.getExtension())) {
                return 1;
            }
            return Integer.compare(o2.getNumberOfFiles(), o1.getNumberOfFiles());
        });
    }

    public Map<String, ExtensionGroup> getExtensionsMap() {
        return extensionsMap;
    }

    public void setExtensionsMap(Map<String, ExtensionGroup> extensionsMap) {
        this.extensionsMap = extensionsMap;
    }
}

