/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

public class ReportConstants {
    public static final String REPORT_SVG_ICON = "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"80\" height=\"80\" viewBox=\"0 0 100 100\"><rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"></rect><g style=\"fill:#000000\"><svg fill=\"#000000\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\" x=\"0px\" y=\"0px\" viewBox=\"0 0 800 800\" style=\"enable-background:new 0 0 800 800;\" xml:space=\"preserve\"><rect x=\"229.5\" y=\"356.4\" width=\"293\" height=\"64.5\"></rect><rect x=\"229\" y=\"232\" width=\"146\" height=\"64\"></rect><rect x=\"229.5\" y=\"481.3\" width=\"293\" height=\"64\"></rect><rect x=\"228.5\" y=\"606\" width=\"293\" height=\"64\"></rect><path d=\"M610.6,709.2c0,8.1-6.6,14.7-14.7,14.7H156.4c-8.1,0-14.7-6.6-14.7-14.7V152.4c0-8.1,6.6-14.7,14.7-14.7h214.5  c3.3-10.2,7.5-20,12.4-29.3H156.4c-24.3,0-44,19.7-44,44v556.8c0,24.3,19.7,44,44,44H596c24.3,0,44-19.7,44-44v-344  c-9.3,4.9-19.1,9.1-29.3,12.4v331.6L610.6,709.2L610.6,709.2z\"></path><g><path d=\"M537.6,142.4c-23.3,0-41.9,18.6-41.9,41.9s18.6,41.9,41.9,41.9c23.2,0,41.8-18.6,41.8-41.9   C579.4,161.1,560.8,142.4,537.6,142.4z\"></path><path d=\"M550.9,51.2c-80.9,0-146.5,65.6-146.5,146.5S470,344.2,550.9,344.2s146.5-65.6,146.5-146.5S631.9,51.2,550.9,51.2z    M625.7,272.4c-4.5,4.5-11.7,4.5-16.2,0l-35-35c-10.5,7.3-23.1,11.7-36.8,11.7c-35.6,0-64.8-29.1-64.8-64.8   c0-35.6,29.1-64.7,64.8-64.7c35.6,0,64.7,29.1,64.7,64.7c0,13.7-4.3,26.4-11.7,36.9l35,35C630.1,260.7,630.1,267.9,625.7,272.4z\"></path></g></svg></g></svg>";

    public final static String TAB_STYLE = ".tab {\n" +
            "  overflow: hidden;\n" +
            "  border: 1px solid #ccc;\n" +
            "  background-color: #f1f1f1;\n" +
            "}\n" +
            "\n" +
            ".tab button {\n" +
            "  font-family: Vollkorn, Ubuntu, Optima, Segoe, Segoe UI, Candara, Calibri, Arial, sans-serif;\n" +
            "  font-size: 100%;\n" +
            "  background-color: inherit;\n" +
            "  float: left;\n" +
            "  border: none;\n" +
            "  outline: none;\n" +
            "  cursor: pointer;\n" +
            "  padding: 14px 16px;\n" +
            "  transition: 0.3s;\n" +
            "}\n" +
            "\n" +
            ".tab button:hover {\n" +
            "  background-color: #ddd;\n" +
            "}\n" +
            "\n" +
            ".tab button.active {\n" +
            "  background-color: #ccc;\n" +
            "}\n" +
            "\n" +
            ".tabcontent {\n" +
            "  display: none;\n" +
            "  padding: 6px 12px;\n" +
            "  border: 1px solid #ccc;\n" +
            "  border-top: none;\n" +
            "}";

    public final static String REPORTS_HTML_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style type=\"text/css\" media=\"all\">\n" +
            "        body {\n" +
            "            font-family: Vollkorn, Ubuntu, Optima, Segoe, Segoe UI, Candara, Calibri, Arial, sans-serif;\n" +
            "            margin-left: ${margin-left};\n" +
            "            margin-right: ${margin-right};\n" +
            "        }\n" +
            "\n" +
            "        table {\n" +
            "            color: #333;\n" +
            "            border-collapse: collapse;\n" +
            "            border-spacing: 0;\n" +
            "        }\n" +
            "\n" +
            "        td, th {\n" +
            "            border: 1px solid #CCC;\n" +
            "            height: 30px;\n" +
            "            padding-left: 10px;\n" +
            "            padding-right: 10px;\n" +
            "        }\n" +
            "\n" +
            "        th {\n" +
            "            background: #F3F3F3;\n" +
            "            font-weight: bold;\n" +
            "        }\n" +
            "\n" +
            "        ul {\n" +
            "            padding-left: 20px;\n" +
            "        }\n" +
            "\n" +
            "        td {\n" +
            "            background: #FFFFFF;\n" +
            "            text-align: left;\n" +
            "        }\n" +
            "\n" +
            "        h3 {\n" +
            "            margin-top: 0\n" +
            "        }\n" +
            "\n" +
            "        h1 {\n" +
            "            margin-bottom: 6px;\n" +
            "        }\n" +
            "\n" +
            "        p {\n" +
            "            margin-top: 0;\n" +
            "        }\n" +
            "\n" +
            "        .reportSubtitle {\n" +
            "            color: grey;\n" +
            "        }\n" +
            "\n" +
            "        .group {\n" +
            "            margin-bottom: 10px;\n" +
            "            border: solid lightgrey 1px;\n" +
            "            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.2), 0 3px 10px 0 rgba(0, 0, 0, 0.19);\n" +
            "            border-radius: 2px;\n" +
            "        }\n" +
            "\n" +
            "        .section {\n" +
            "            margin-bottom: 30px;\n" +
            "            border: solid lightgrey 1px;\n" +
            "            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.2), 0 3px 10px 0 rgba(0, 0, 0, 0.19);\n" +
            "            border-radius: 2px;\n" +
            "            padding: 0;\n" +
            "        }\n" +
            "\n" +
            "        .sectionHeader {\n" +
            "            margin-bottom: 30px;\n" +
            "            padding: 20px;\n" +
            "            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.2), 0 3px 10px 0 rgba(0, 0, 0, 0.19);\n" +
            "            background-color: lightskyblue;\n" +
            "        }\n" +
            "\n" +
            "        .sectionTitle {\n" +
            "            font-size: 140%;\n" +
            "        }\n" +
            "\n" +
            "        .sectionSubtitle {\n" +
            "            font-size: 90%; color: grey;\n" +
            "        }\n" +
            "\n" +
            "        .subSectionTitle {\n" +
            "            font-size: 140%;\n" +
            "        }\n" +
            "\n" +
            "        .subSectionSubtitle {\n" +
            "            font-size: 90%; color: grey;\n" +
            "        }\n" +
            "\n" +
            "        .subSection {\n" +
            "            margin-bottom: 30px;\n" +
            "            border: solid lightgrey 1px;\n" +
            "            padding-top: 0;\n" +
            "            padding-bottom: 0;\n" +
            "            box-shadow: 0 0px 0px 0 rgba(0, 0, 0, 0.2), 0 1px 9px 0 rgba(0, 0, 0, 0.19);\n" +
            "        }\n" +
            "\n" +
            "        .subSectionHeader {\n" +
            "            border: solid lightgrey 1px;\n" +
            "            padding: 6px;\n" +
            "            background-color: lightgrey;\n" +
            "            box-shadow: 0 0px 0px 0 rgba(0, 0, 0, 0.2), 0 2px 9px 0 rgba(0, 0, 0, 0.19);\n" +
            "        }\n" +
            "\n" +
            "        .sectionBody {\n" +
            "            margin: 20px;\n" +
            "        }" +
            "\n\n" + TAB_STYLE + "\n\n" +
            "    </style>\n" +
            "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Ubuntu\">\n" +
            "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Lato\">\n" +
            "    <script type=\"text/javascript\">\n" +
            "        function showHide(id) {\n" +
            "            var e = document.getElementById(id);\n" +
            "            e.style.display = (e.style.display == 'block') ? 'none' : 'block';\n" +
            "        }\n" +
            "        function showHideDisappear(id) {\n" +
            "            var e = document.getElementById(id);\n" +
            "            e.style.display = '';" +
            "            var eTrigger = document.getElementById(id + '_trigger');\n" +
            "            eTrigger.style.display = 'none';\n" +
            "        }\n\n" +
            "        function openTab(evt, tabName) {\n" +
            "          var i, tabcontent, tablinks;\n" +
            "          tabcontent = document.getElementsByClassName(\"tabcontent\");\n" +
            "          for (i = 0; i < tabcontent.length; i++) {\n" +
            "            tabcontent[i].style.display = \"none\";\n" +
            "          }\n" +
            "          tablinks = document.getElementsByClassName(\"tablinks\");\n" +
            "          for (i = 0; i < tablinks.length; i++) {\n" +
            "            tablinks[i].className = tablinks[i].className.replace(\" active\", \"\");\n" +
            "          }\n" +
            "          document.getElementById(tabName).style.display = \"block\";\n" +
            "          evt.currentTarget.className += \" active\";\n" +
            "        }\n" +
            "    </script>\n" +
            "</head>\n";
}
