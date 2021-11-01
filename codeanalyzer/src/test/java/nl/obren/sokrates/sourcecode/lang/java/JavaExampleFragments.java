/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.java;

public class JavaExampleFragments {
    protected static final String FRAGMENT_2 = "    /**\n" +
            "     * Validate the syntax of a proposed <code>&lt;url-pattern&gt;</code>\n" +
            "     * for conformance with specification requirements.\n" +
            "     *\n" +
            "     * @param urlPattern URL pattern to be validated\n" +
            "     * @return <code>true</code> if the URL pattern is conformant\n" +
            "     */\n" +
            "    private boolean validateURLPattern(String urlPattern) {\n" +
            "\n" +
            "        if (urlPattern == null)\n" +
            "            return false;\n" +
            "        if (urlPattern.indexOf('\\n') >= 0 || urlPattern.indexOf('\\r') >= 0) {\n" +
            "            return false;\n" +
            "        }\n" +
            "        if (urlPattern.equals(\"\")) {\n" +
            "            return true;\n" +
            "        }\n" +
            "        if (urlPattern.startsWith(\"*.\")) {\n" +
            "            if (urlPattern.indexOf('/') < 0) {\n" +
            "                checkUnusualURLPattern(urlPattern);\n" +
            "                return true;\n" +
            "            } else\n" +
            "                return false;\n" +
            "        }\n" +
            "        if (urlPattern.startsWith(\"/\") && !urlPattern.contains(\"*.\")) {\n" +
            "            checkUnusualURLPattern(urlPattern);\n" +
            "            return true;\n" +
            "        } else\n" +
            "            return false;\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    /**\n" +
            "     * Check for unusual but valid <code>&lt;url-pattern&gt;</code>s.\n" +
            "     * See Bugzilla 34805, 43079 & 43080\n" +
            "     */\n" +
            "    private void checkUnusualURLPattern(String urlPattern) {\n" +
            "        if (log.isInfoEnabled()) {\n" +
            "            // First group checks for '*' or '/foo*' style patterns\n" +
            "            // Second group checks for *.foo.bar style patterns\n" +
            "            if((urlPattern.endsWith(\"*\") && (urlPattern.length() < 2 ||\n" +
            "                        urlPattern.charAt(urlPattern.length()-2) != '/')) ||\n" +
            "                    urlPattern.startsWith(\"*.\") && urlPattern.length() > 2 &&\n" +
            "                        urlPattern.lastIndexOf('.') > 1) {\n" +
            "                log.info(sm.getString(\"standardContext.suspiciousUrl\", urlPattern, getName()));\n" +
            "            }\n" +
            "        }\n" +
            "    }\n";
    protected static final String FRAGMENT_2_CLEANED = "    private boolean validateURLPattern(String urlPattern) {\n" +
            "        if (urlPattern == null)\n" +
            "            return false;\n" +
            "        if (urlPattern.indexOf('\\n') >= 0 || urlPattern.indexOf('\\r') >= 0) {\n" +
            "            return false;\n" +
            "        }\n" +
            "        if (urlPattern.equals(\"\")) {\n" +
            "            return true;\n" +
            "        }\n" +
            "        if (urlPattern.startsWith(\"*.\")) {\n" +
            "            if (urlPattern.indexOf('/') < 0) {\n" +
            "                checkUnusualURLPattern(urlPattern);\n" +
            "                return true;\n" +
            "            } else\n" +
            "                return false;\n" +
            "        }\n" +
            "        if (urlPattern.startsWith(\"/\") && !urlPattern.contains(\"*.\")) {\n" +
            "            checkUnusualURLPattern(urlPattern);\n" +
            "            return true;\n" +
            "        } else\n" +
            "            return false;\n" +
            "    }\n" +
            "    private void checkUnusualURLPattern(String urlPattern) {\n" +
            "        if (log.isInfoEnabled()) {\n" +
            "            if((urlPattern.endsWith(\"*\") && (urlPattern.length() < 2 ||\n" +
            "                        urlPattern.charAt(urlPattern.length()-2) != '/')) ||\n" +
            "                    urlPattern.startsWith(\"*.\") && urlPattern.length() > 2 &&\n" +
            "                        urlPattern.lastIndexOf('.') > 1) {\n" +
            "                log.info(sm.getString(\"standardContext.suspiciousUrl\", urlPattern, getName()));\n" +
            "            }\n" +
            "        }\n" +
            "    }";
    protected static final String FRAGMENT_3 = "    /**\n" +
            "     * Set the appropriate context attribute for our work directory.\n" +
            "     */\n" +
            "    private void postWorkDirectory() {\n" +
            "\n" +
            "        // Acquire (or calculate) the work directory path\n" +
            "        String workDir = getWorkDir();\n" +
            "        if (workDir == null || workDir.length() == 0) {\n" +
            "\n" +
            "            // Retrieve our parent (normally a host) name\n" +
            "            String hostName = null;\n" +
            "            String engineName = null;\n" +
            "            String hostWorkDir = null;\n" +
            "            Container parentHost = getParent();\n" +
            "            if (parentHost != null) {\n" +
            "                hostName = parentHost.getName();\n" +
            "                if (parentHost instanceof StandardHost) {\n" +
            "                    hostWorkDir = ((StandardHost)parentHost).getWorkDir();\n" +
            "                }\n" +
            "                Container parentEngine = parentHost.getParent();\n" +
            "                if (parentEngine != null) {\n" +
            "                   engineName = parentEngine.getName();\n" +
            "                }\n" +
            "            }\n" +
            "            if ((hostName == null) || (hostName.length() < 1))\n" +
            "                hostName = \"_\";\n" +
            "            if ((engineName == null) || (engineName.length() < 1))\n" +
            "                engineName = \"_\";\n" +
            "\n" +
            "            String temp = getBaseName();\n" +
            "            if (temp.startsWith(\"/\"))\n" +
            "                temp = temp.substring(1);\n" +
            "            temp = temp.replace('/', '_');\n" +
            "            temp = temp.replace('\\\\', '_');\n" +
            "            if (temp.length() < 1)\n" +
            "                temp = ContextName.ROOT_NAME;\n" +
            "            if (hostWorkDir != null ) {\n" +
            "                workDir = hostWorkDir + File.separator + temp;\n" +
            "            } else {\n" +
            "                workDir = \"work\" + File.separator + engineName +\n" +
            "                    File.separator + hostName + File.separator + temp;\n" +
            "            }\n" +
            "            setWorkDir(workDir);\n" +
            "        }\n" +
            "\n" +
            "        // Create this directory if necessary\n" +
            "        File dir = new File(workDir);\n" +
            "        if (!dir.isAbsolute()) {\n" +
            "            String catalinaHomePath = null;\n" +
            "            try {\n" +
            "                catalinaHomePath = getCatalinaBase().getCanonicalPath();\n" +
            "                dir = new File(catalinaHomePath, workDir);\n" +
            "            } catch (IOException e) {\n" +
            "                log.warn(sm.getString(\"standardContext.workCreateException\",\n" +
            "                        workDir, catalinaHomePath, getName()), e);\n" +
            "            }\n" +
            "        }\n" +
            "        if (!dir.mkdirs() && !dir.isDirectory()) {\n" +
            "            log.warn(sm.getString(\"standardContext.workCreateFail\", dir,\n" +
            "                    getName()));\n" +
            "        }\n" +
            "\n" +
            "        // Set the appropriate servlet context attribute\n" +
            "        if (context == null) {\n" +
            "            getServletContext();\n" +
            "        }\n" +
            "        context.setAttribute(ServletContext.TEMPDIR, dir);\n" +
            "        context.setAttributeReadOnly(ServletContext.TEMPDIR);\n" +
            "    }\n";
    protected static final String FRAGMENT_3_CLEANED = "    private void postWorkDirectory() {\n" +
            "        String workDir = getWorkDir();\n" +
            "        if (workDir == null || workDir.length() == 0) {\n" +
            "            String hostName = null;\n" +
            "            String engineName = null;\n" +
            "            String hostWorkDir = null;\n" +
            "            Container parentHost = getParent();\n" +
            "            if (parentHost != null) {\n" +
            "                hostName = parentHost.getName();\n" +
            "                if (parentHost instanceof StandardHost) {\n" +
            "                    hostWorkDir = ((StandardHost)parentHost).getWorkDir();\n" +
            "                }\n" +
            "                Container parentEngine = parentHost.getParent();\n" +
            "                if (parentEngine != null) {\n" +
            "                   engineName = parentEngine.getName();\n" +
            "                }\n" +
            "            }\n" +
            "            if ((hostName == null) || (hostName.length() < 1))\n" +
            "                hostName = \"_\";\n" +
            "            if ((engineName == null) || (engineName.length() < 1))\n" +
            "                engineName = \"_\";\n" +
            "            String temp = getBaseName();\n" +
            "            if (temp.startsWith(\"/\"))\n" +
            "                temp = temp.substring(1);\n" +
            "            temp = temp.replace('/', '_');\n" +
            "            temp = temp.replace('\\\\', '_');\n" +
            "            if (temp.length() < 1)\n" +
            "                temp = ContextName.ROOT_NAME;\n" +
            "            if (hostWorkDir != null ) {\n" +
            "                workDir = hostWorkDir + File.separator + temp;\n" +
            "            } else {\n" +
            "                workDir = \"work\" + File.separator + engineName +\n" +
            "                    File.separator + hostName + File.separator + temp;\n" +
            "            }\n" +
            "            setWorkDir(workDir);\n" +
            "        }\n" +
            "        File dir = new File(workDir);\n" +
            "        if (!dir.isAbsolute()) {\n" +
            "            String catalinaHomePath = null;\n" +
            "            try {\n" +
            "                catalinaHomePath = getCatalinaBase().getCanonicalPath();\n" +
            "                dir = new File(catalinaHomePath, workDir);\n" +
            "            } catch (IOException e) {\n" +
            "                log.warn(sm.getString(\"standardContext.workCreateException\",\n" +
            "                        workDir, catalinaHomePath, getName()), e);\n" +
            "            }\n" +
            "        }\n" +
            "        if (!dir.mkdirs() && !dir.isDirectory()) {\n" +
            "            log.warn(sm.getString(\"standardContext.workCreateFail\", dir,\n" +
            "                    getName()));\n" +
            "        }\n" +
            "        if (context == null) {\n" +
            "            getServletContext();\n" +
            "        }\n" +
            "        context.setAttribute(ServletContext.TEMPDIR, dir);\n" +
            "        context.setAttributeReadOnly(ServletContext.TEMPDIR);\n" +
            "    }";
    protected static final String FRAGMENT_4 = "    class A { \n" +
            "    @JsonIgnore\n" +
            "    public NamedSourceCodeAspect getScope(String scope) {\n" +
            "        switch (scope.toLowerCase()) {\n" +
            "            case \"main\":\n" +
            "                return main;\n" +
            "            case \"test\":\n" +
            "                return test;\n" +
            "            case \"buildAndDeployment\":\n" +
            "                return buildAndDeployment;\n" +
            "            case \"other\":\n" +
            "                return other;\n" +
            "        }\n" +
            "        return main;\n" +
            "    }\n" +
            "}";
    protected static final String FRAGMENT_1 = "    /**\n" +
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
    protected static final String FRAGMENT_1_CLEANED = "    protected String getRelativePath(HttpServletRequest request) {\n" +
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
    protected static final String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "protected String getRelativePath(HttpServletRequest request) {\n" +
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
