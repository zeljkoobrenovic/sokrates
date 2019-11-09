package nl.obren.sokrates.codeexplorer.dependencies;

import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;

public class DependenciesMatrix extends BorderPane {
    public static final String ARROW = "->";
    protected WebView webView = new WebView();

    public DependenciesMatrix() {
        webView.setZoom(0.9);
        UXUtils.addCopyHandler(webView);
        setCenter(webView);
    }

    public void load(List<ComponentDependency> dependencies) {

        List<String> rows = getRows(dependencies);
        List<String> columns = getColumns(dependencies);
        Map<String, ComponentDependency> dependenciesMap = getDependencyMap(dependencies);

        int maxCount[] = {0};
        dependenciesMap.values().forEach(dependency -> maxCount[0] = Math.max(maxCount[0], dependency.getCount()));
        String html = getHtml(rows, columns, dependenciesMap, maxCount[0]);

        webView.getEngine().loadContent(html);
    }

    private String getHtml(List<String> rows, List<String> columns, Map<String, ComponentDependency> dependenciesMap, int maxCount) {
        StringBuilder html = new StringBuilder(UXUtils.DEFAULT_HTML_HEADER);

        html.append("<table>\n");
        html.append("<th></th>\n");
        columns.forEach(column -> {
            html.append("<th style='width: 102px'> " + StringEscapeUtils.escapeHtml4(column) + " -></th>\n");
        });
        rows.forEach(row -> {
            html.append("<tr>\n");
            html.append("<td><b>-> " + StringEscapeUtils.escapeHtml4(row) + "</b></td>\n");
            columns.forEach(column -> {
                String key = row + ARROW + column;
                html.append("<td>\n");
                if (dependenciesMap.containsKey(key)) {
                    int count = dependenciesMap.get(key).getCount();
                    html.append(getSvg(count, maxCount));
                } else {
                    html.append(getEmptySvg());
                }
                html.append("</td>\n");
            });
            html.append("</tr>\n");
        });
        html.append("</table>\n");

        return html.toString();
    }

    private String getSvg(int count, int maxCount) {
        int r = (int) (50.0 * Math.sqrt(Math.min(count, maxCount) / (double) maxCount));
        r = Math.max(r, 7);

        String svg = "<svg height=\"102\" width=\"102\">\n";
        svg += getCross();
        svg += "<circle cx=\"" + 51 + "\" cy=\"" + 51 + "\" r=\"" + r + "\" fill=\"skyblue\" />";
        svg += "<text x=\"50%\" y=\"53%\" text-anchor=\"middle\">" + count + "</text>";
        svg += "</svg>";

        return svg;
    }

    private String getEmptySvg() {
        String svg = "<svg height=\"102\" width=\"102\">\n";
        svg += getCross();
        svg += "</svg>";
        return svg;
    }

    private String getCross() {
        String svg = "<line x1=\"0%\" y1=\"50%\" x2=\"100%\" y2=\"50%\" style=\"stroke:rgb(200,200,200);stroke-width:2\"/>";
        svg += "<line x1=\"50%\" y1=\"0%\" x2=\"50%\" y2=\"100%\" style=\"stroke:rgb(200,200,200);stroke-width:2\"/>";
        return svg;
    }

    private Map<String, ComponentDependency> getDependencyMap(List<ComponentDependency> dependencies) {
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
        dependencies.forEach(dependency -> {
            String key = dependency.getToComponent() + ARROW + dependency.getFromComponent();
            dependenciesMap.put(key, dependency);
        });
        return dependenciesMap;
    }

    private List<String> getRows(List<ComponentDependency> dependencies) {
        List<String> rows = new ArrayList<>();
        Map<String, Integer> rowsCountMap = new HashMap<>();
        dependencies.forEach(dependency -> {
            String toComponent = dependency.getToComponent();
            if (!rows.contains(toComponent)) {
                rows.add(toComponent);
            }
            rowsCountMap.put(toComponent, rowsCountMap.containsKey(toComponent)
                    ? rowsCountMap.get(toComponent) + dependency.getCount()
                    : dependency.getCount());
        });
        Collections.sort(rows, (o1, o2) -> new Integer(rowsCountMap.get(o2)).compareTo(rowsCountMap.get(o1)));
        return rows;
    }

    private List<String> getColumns(List<ComponentDependency> dependencies) {
        List<String> columns = new ArrayList<>();
        dependencies.forEach(dependency -> {
            if (!columns.contains(dependency.getFromComponent())) {
                columns.add(dependency.getFromComponent());
            }
        });
        return columns;
    }
}