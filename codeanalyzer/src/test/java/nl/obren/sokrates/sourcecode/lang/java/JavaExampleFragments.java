package nl.obren.sokrates.sourcecode.lang.java;

public class JavaExampleFragments {
    public static String FRAGMENT_1 = "    /**\n" +
            "     * Return the relative path associated with this servlet.\n" +
            "     *\n" +
            "     * @param request The servlet request we are processing\n" +
            "     * @return the relative path\n" +
            "     */\n" +
            "    protected String getRelativePath(HttpServletRequest request) {\n" +
            "        return getRelativePath(request, false);\n" +
            "    }\n" +
            "\n" +
            "    protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {\n" +
            "        // IMPORTANT: DefaultServlet can be mapped to '/' or '/path/*' but always\n" +
            "        // serves resources from the web app root with context rooted paths.\n" +
            "        // i.e. it cannot be used to mount the web app root under a sub-path\n" +
            "        // This method must construct a complete context rooted path, although\n" +
            "        // subclasses can change this behaviour.\n" +
            "\n" +
            "        String servletPath;\n" +
            "        String pathInfo;\n" +
            "\n" +
            "        if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {\n" +
            "            // For includes, get the info from the attributes\n" +
            "            pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);\n" +
            "            servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);\n" +
            "        } else {\n" +
            "            pathInfo = request.getPathInfo();\n" +
            "            servletPath = request.getServletPath();\n" +
            "        }\n" +
            "\n" +
            "        StringBuilder result = new StringBuilder();\n" +
            "        if (servletPath.length() > 0) {\n" +
            "            result.append(servletPath);\n" +
            "        }\n" +
            "        if (pathInfo != null) {\n" +
            "            result.append(pathInfo);\n" +
            "        }\n" +
            "        if (result.length() == 0 && !allowEmptyPath) {\n" +
            "            result.append('/');\n" +
            "        }\n" +
            "\n" +
            "        return result.toString();\n" +
            "    }\n";

    public static String FRAGMENT_1_CLEANED = "    protected String getRelativePath(HttpServletRequest request) {\n" +
            "        return getRelativePath(request, false);\n" +
            "    }\n" +
            "    protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {\n" +
            "        String servletPath;\n" +
            "        String pathInfo;\n" +
            "        if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {\n" +
            "            pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);\n" +
            "            servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);\n" +
            "        } else {\n" +
            "            pathInfo = request.getPathInfo();\n" +
            "            servletPath = request.getServletPath();\n" +
            "        }\n" +
            "        StringBuilder result = new StringBuilder();\n" +
            "        if (servletPath.length() > 0) {\n" +
            "            result.append(servletPath);\n" +
            "        }\n" +
            "        if (pathInfo != null) {\n" +
            "            result.append(pathInfo);\n" +
            "        }\n" +
            "        if (result.length() == 0 && !allowEmptyPath) {\n" +
            "            result.append('/');\n" +
            "        }\n" +
            "        return result.toString();\n" +
            "    }";

    public static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "protected String getRelativePath(HttpServletRequest request) {\n" +
            "return getRelativePath(request, false);\n" +
            "protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {\n" +
            "String servletPath;\n" +
            "String pathInfo;\n" +
            "if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {\n" +
            "pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);\n" +
            "servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);\n" +
            "} else {\n" +
            "pathInfo = request.getPathInfo();\n" +
            "servletPath = request.getServletPath();\n" +
            "StringBuilder result = new StringBuilder();\n" +
            "if (servletPath.length() > 0) {\n" +
            "result.append(servletPath);\n" +
            "if (pathInfo != null) {\n" +
            "result.append(pathInfo);\n" +
            "if (result.length() == 0 && !allowEmptyPath) {\n" +
            "result.append('/');\n" +
            "return result.toString();";
}
