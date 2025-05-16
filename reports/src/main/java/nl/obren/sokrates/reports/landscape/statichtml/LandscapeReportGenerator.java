/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.landscape.data.LandscapeDataExport;
import nl.obren.sokrates.reports.landscape.statichtml.repositories.*;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.AnimalIcons;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.reports.utils.PromptsUtils;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResultsReadData;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LandscapeReportGenerator {
    public static final String DEPENDENCIES_ICON = "\n" +
            "<svg height='100px' width='100px'  fill=\"#000000\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\" x=\"0px\" y=\"0px\" viewBox=\"0 0 48 48\" enable-background=\"new 0 0 48 48\" xml:space=\"preserve\"><path d=\"M12,19.666v6.254l1.357-1.357c0.391-0.391,1.023-0.391,1.414,0s0.391,1.023,0,1.414l-3.064,3.064  c-0.195,0.195-0.451,0.293-0.707,0.293s-0.512-0.098-0.707-0.293l-3.064-3.064c-0.391-0.391-0.391-1.023,0-1.414  s1.023-0.391,1.414,0L10,25.92v-6.254c0-0.552,0.448-1,1-1S12,19.114,12,19.666z M28.334,36H22.08l1.357-1.357  c0.391-0.391,0.391-1.023,0-1.414s-1.023-0.391-1.414,0l-3.064,3.064c-0.391,0.391-0.391,1.023,0,1.414l3.064,3.064  c0.195,0.195,0.451,0.293,0.707,0.293s0.512-0.098,0.707-0.293c0.391-0.391,0.391-1.023,0-1.414L22.08,38h6.254c0.553,0,1-0.447,1-1  S28.887,36,28.334,36z M37,18.666c-0.553,0-1,0.448-1,1v6.254l-1.357-1.357c-0.391-0.391-1.023-0.391-1.414,0s-0.391,1.023,0,1.414  l3.064,3.064c0.195,0.195,0.451,0.293,0.707,0.293s0.512-0.098,0.707-0.293l3.064-3.064c0.391-0.391,0.391-1.023,0-1.414  s-1.023-0.391-1.414,0L38,25.92v-6.254C38,19.114,37.553,18.666,37,18.666z M31.58,16.421c-0.391-0.391-1.023-0.391-1.414,0  L18.127,28.458v-1.92c0-0.553-0.448-1-1-1s-1,0.447-1,1v4.334c0,0.13,0.027,0.26,0.077,0.382c0.101,0.245,0.296,0.439,0.541,0.541  c0.122,0.051,0.251,0.077,0.382,0.077h4.333c0.552,0,1-0.447,1-1s-0.448-1-1-1h-1.919L31.58,17.835  C31.971,17.444,31.971,16.812,31.58,16.421z M16.334,37c0,2.941-2.393,5.334-5.334,5.334S5.666,39.941,5.666,37  S8.059,31.666,11,31.666S16.334,34.059,16.334,37z M14.334,37c0-1.838-1.496-3.334-3.334-3.334S7.666,35.162,7.666,37  S9.162,40.334,11,40.334S14.334,38.838,14.334,37z M42.334,37c0,2.941-2.393,5.334-5.334,5.334S31.666,39.941,31.666,37  s2.393-5.334,5.334-5.334S42.334,34.059,42.334,37z M40.334,37c0-1.838-1.496-3.334-3.334-3.334S33.666,35.162,33.666,37  s1.496,3.334,3.334,3.334S40.334,38.838,40.334,37z M5.666,11c0-2.941,2.393-5.334,5.334-5.334S16.334,8.059,16.334,11  S13.941,16.334,11,16.334S5.666,13.941,5.666,11z M7.666,11c0,1.838,1.496,3.334,3.334,3.334s3.334-1.496,3.334-3.334  S12.838,7.666,11,7.666S7.666,9.162,7.666,11z M31.666,11c0-2.941,2.393-5.334,5.334-5.334S42.334,8.059,42.334,11  S39.941,16.334,37,16.334S31.666,13.941,31.666,11z M33.666,11c0,1.838,1.496,3.334,3.334,3.334s3.334-1.496,3.334-3.334  S38.838,7.666,37,7.666S33.666,9.162,33.666,11z\"></path></svg>";

    public static final int RECENT_THRESHOLD_DAYS = 30;
    public static final String OVERVIEW_TAB_ID = "overview";
    public static final String SUB_LANDSCAPES_TAB_ID = "sub-landscapes";
    public static final String REPOSITORIES_TAB_ID = "repositories";
    public static final String STATS_TAB_ID = "stats";

    public static final String TAGS_TAB_ID = "tags";
    public static final String CONTRIBUTORS_TAB_ID = "contributors";
    public static final String TOPOLOGIES_TAB_ID = "topologies";
    public static final String PROMPTS_TAB_ID = "prompts";
    public static final String TEAMS_TAB_ID = "teams";
    public static final String CUSTOM_TAB_ID_PREFIX = "custom_tab_";
    public static final String CONTRIBUTORS_30_D = "contributors_30d_";
    public static final String COMMITS_30_D = "commits_30d_";
    public static final String MAIN_LOC = "main_loc_";
    private static final Log LOG = LogFactory.getLog(LandscapeReportGenerator.class);
    public static final String DEVELOPER_SVG_ICON = "<svg width=\"16pt\" height=\"16pt\" version=\"1.1\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
            " <g>\n" +
            "  <path d=\"m82 61.801-14-14c-2.1016-2.1016-4.8008-3.1992-7.8008-3.1992h-20.398c-2.8984 0-5.6992 1.1016-7.8008 3.1992l-14 14c-1.3008 1.3008-2 3.1016-2 4.8984 0 1.8984 0.69922 3.6016 2 5l3.8984 3.8984c1.3008 1.3008 3.1016 2.1016 5 2.1016 1.8984 0 3.6016-0.69922 4.8984-2.1016l1.6016-1.6016v10c0 3.8984 3.1016 7 7 7h19.102c3.8984 0 7-3.1016 7-7v-9.9961l1.6016 1.6016c1.3008 1.3008 3.1016 2.1016 5 2.1016 1.8984 0 3.6016-0.69922 4.8984-2.1016l3.8984-3.8984c2.8008-2.8047 2.8008-7.2031 0.10156-9.9023zm-4.3008 5.5977-3.8984 3.8984c-0.39844 0.39844-1 0.39844-1.3984 0l-6.6992-6.6992c-0.89844-0.89844-2.1016-1.1016-3.3008-0.69922-1.1016 0.5-1.8984 1.6016-1.8984 2.8008l-0.003906 17.301c0 0.60156-0.39844 1-1 1h-19c-0.60156 0-1-0.39844-1-1v-17.301c0-1.1992-0.69922-2.3008-1.8984-2.8008-0.39844-0.19922-0.80078-0.19922-1.1016-0.19922-0.80078 0-1.6016 0.30078-2.1016 0.89844l-6.6992 6.6992c-0.39844 0.39844-1 0.39844-1.3984 0l-3.8984-3.8984c-0.39844-0.39844-0.39844-1 0-1.3984l14-14c0.89844-0.89844 2.1992-1.5 3.5-1.5h20.5c1.3008 0 2.6016 0.5 3.5 1.5l14 14c0.19922 0.19922 0.30078 0.39844 0.30078 0.69922-0.003906 0.30078-0.30469 0.5-0.50391 0.69922z\"></path>\n" +
            "  <path d=\"m50 42.102c9.1016 0 16.5-7.3984 16.5-16.5 0-9.2031-7.3984-16.602-16.5-16.602s-16.5 7.3984-16.5 16.5c0 9.1992 7.3984 16.602 16.5 16.602zm0-27.102c5.8008 0 10.5 4.6992 10.5 10.5s-4.6992 10.602-10.5 10.602-10.5-4.6992-10.5-10.5c0-5.8008 4.6992-10.602 10.5-10.602z\"></path>\n" +
            " </g>\n" +
            "</svg>";
    public static final String TEAM_SVG_ICON = "<svg width=\"14pt\" height=\"14pt\" version=\"1.1\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
            " <path d=\"m27.75 13.996c-0.33203-10.332-15.305-10.332-15.637 0 0.33594 10.324 15.293 10.328 15.637 0z\"/>\n" +
            " <path d=\"m28.844 21.113-0.48438-0.15625c-2.0703 2.5234-5.1641 3.9883-8.4297 3.9883-3.2656 0-6.3594-1.4648-8.4297-3.9883l-0.48438 0.15625c-2.7812 0.93359-4.6602 3.5391-4.6602 6.4727v2.7227c0.003906 1.6914 1.375 3.0625 3.0664 3.0625h21.016c1.6914 0 3.0625-1.3711 3.0664-3.0625v-2.7227c0-2.9336-1.8789-5.5391-4.6602-6.4727z\"/>\n" +
            " <path d=\"m27.75 74.141c-0.33203-10.332-15.305-10.332-15.637 0l-0.003906-0.003906c0.027344 4.3008 3.5195 7.7734 7.8203 7.7734 4.3008 0 7.793-3.4727 7.8203-7.7695z\"/>\n" +
            " <path d=\"m28.844 81.254-0.48438-0.15625c-2.082 2.5078-5.1719 3.9609-8.4297 3.9609-3.2578 0-6.3477-1.4531-8.4297-3.9609-3.0195 0.78516-5.1328 3.5078-5.1445 6.6289v2.7227c0.003906 1.6914 1.375 3.0625 3.0664 3.0625h21.016c1.6914 0 3.0625-1.3711 3.0664-3.0625v-2.7227c0-2.9336-1.8789-5.5391-4.6602-6.4727z\"/>\n" +
            " <path d=\"m72.25 74.137c0.027344 4.3008 3.5195 7.7734 7.8203 7.7734 4.3008 0 7.793-3.4727 7.8203-7.7695-0.33203-10.332-15.309-10.332-15.641-0.003906z\"/>\n" +
            " <path d=\"m88.984 81.254-0.48438-0.15625c-2.082 2.5078-5.1719 3.9609-8.4297 3.9609-3.2578 0-6.3477-1.4531-8.4297-3.9609-3.0195 0.78516-5.1328 3.5078-5.1445 6.6289v2.7227c0.003906 1.6914 1.375 3.0625 3.0664 3.0625h21.016c1.6914 0 3.0625-1.3711 3.0664-3.0625v-2.7227c0-2.9336-1.8789-5.5391-4.6602-6.4727z\"/>\n" +
            " <path d=\"m87.891 13.996c-0.33203-10.332-15.305-10.332-15.637 0 0.33594 10.324 15.293 10.328 15.637 0z\"/>\n" +
            " <path d=\"m88.984 21.113-0.48438-0.15625c-2.0703 2.5234-5.1641 3.9883-8.4297 3.9883-3.2656 0-6.3594-1.4648-8.4297-3.9883l-0.48438 0.15625c-2.7812 0.93359-4.6602 3.5391-4.6602 6.4727v2.7227c0.003906 1.6914 1.375 3.0625 3.0664 3.0625h21.016c1.6914 0 3.0625-1.3711 3.0664-3.0625v-2.7227c0-2.9336-1.8789-5.5391-4.6602-6.4727z\"/>\n" +
            " <path d=\"m16.973 37.285c-4.4219 7.8867-5.8906 17.094-4.1445 25.965 0.16797 0.84766 0.98828 1.3984 1.8359 1.2305 0.84766-0.16797 1.4023-0.98828 1.2344-1.8359-1.6055-8.1406-0.25781-16.586 3.7969-23.824 0.40625-0.75 0.13672-1.6875-0.60547-2.1055-0.74219-0.41797-1.6836-0.16406-2.1172 0.57031z\"/>\n" +
            " <path d=\"m61.293 88.742c-7.3203 2.5-15.266 2.5-22.586 0-0.80859-0.26563-1.6797 0.16797-1.9609 0.97266-0.27734 0.80469 0.13672 1.6836 0.93359 1.9805 7.9844 2.7383 16.656 2.7383 24.641 0 0.79688-0.29687 1.2109-1.1758 0.93359-1.9805-0.28125-0.80469-1.1523-1.2383-1.9609-0.97266z\"/>\n" +
            " <path d=\"m85.641 64.512c0.74609-0.003907 1.3867-0.53125 1.5312-1.2617 1.7461-8.8711 0.27734-18.078-4.1445-25.965-0.43359-0.73438-1.375-0.98828-2.1172-0.57031-0.74219 0.41797-1.0117 1.3555-0.60547 2.1055 4.0547 7.2383 5.4023 15.684 3.7969 23.824-0.085937 0.45703 0.035157 0.93359 0.33203 1.293 0.29688 0.36328 0.73828 0.57031 1.207 0.57422z\"/>\n" +
            " <path d=\"m63.82 20.586c-8.8867-3.4648-18.754-3.4648-27.641 0-0.78516 0.33203-1.1602 1.2266-0.84766 2.0195s1.2031 1.1875 2 0.88672c8.3516-3.1797 17.59-3.1406 25.914 0.11328 0.74219-0.015625 1.3711-0.54688 1.5117-1.2773 0.14063-0.72656-0.25-1.457-0.9375-1.7422z\"/>\n" +
            " <path d=\"m57.82 47.32c-0.32031-10.332-15.316-10.332-15.637 0 0.32422 10.328 15.305 10.332 15.637 0z\"/>\n" +
            " <path d=\"m58.914 54.438-0.48437-0.15625c-2.0859 2.5-5.1719 3.9453-8.4297 3.9453s-6.3438-1.4453-8.4297-3.9492c-3.0234 0.78516-5.1367 3.5078-5.1445 6.6328v2.707-0.003907c0 0.81641 0.32422 1.5938 0.89844 2.1719 0.57422 0.57422 1.3555 0.89453 2.168 0.89453h21.016c0.8125 0 1.5938-0.32031 2.168-0.89453 0.57812-0.57812 0.89844-1.3555 0.89844-2.1719v-2.7031c0.003906-2.9375-1.875-5.5469-4.6602-6.4727z\"/>\n" +
            "</svg>";
    public static final String OPEN_IN_NEW_TAB_SVG_ICON = "<svg width=\"14pt\" height=\"14pt\" version=\"1.1\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
            " <path d=\"m87.5 16.918-35.289 35.289c-1.2266 1.1836-3.1719 1.168-4.3789-0.039062s-1.2227-3.1523-0.039062-4.3789l35.289-35.289h-23.707c-1.7266 0-3.125-1.3984-3.125-3.125s1.3984-3.125 3.125-3.125h31.25c0.82812 0 1.625 0.32812 2.2109 0.91406 0.58594 0.58594 0.91406 1.3828 0.91406 2.2109v31.25c0 1.7266-1.3984 3.125-3.125 3.125s-3.125-1.3984-3.125-3.125zm-56.25 1.832h-15.633c-5.1719 0-9.3672 4.1797-9.3672 9.3516v56.305c0 5.1562 4.2422 9.3516 9.3867 9.3516h56.219c2.4922 0 4.8828-0.98437 6.6406-2.7461 1.7617-1.7617 2.75-4.1523 2.7461-6.6445v-15.613 0.003906c0-1.7266-1.3984-3.125-3.125-3.125-1.7227 0-3.125 1.3984-3.125 3.125v15.613-0.003906c0.003906 0.83594-0.32422 1.6328-0.91406 2.2227s-1.3906 0.91797-2.2227 0.91797h-56.219c-1.7148-0.007812-3.1094-1.3867-3.1367-3.1016v-56.305c0-1.7148 1.3945-3.1016 3.1172-3.1016h15.633c1.7266 0 3.125-1.3984 3.125-3.125s-1.3984-3.125-3.125-3.125z\"/>\n" +
            "</svg>";
    public static final String OPEN_IN_NEW_TAB_SVG_ICON_SMALL = "<svg width=\"14pt\" height=\"10pt\" version=\"1.1\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
            " <path d=\"m87.5 16.918-35.289 35.289c-1.2266 1.1836-3.1719 1.168-4.3789-0.039062s-1.2227-3.1523-0.039062-4.3789l35.289-35.289h-23.707c-1.7266 0-3.125-1.3984-3.125-3.125s1.3984-3.125 3.125-3.125h31.25c0.82812 0 1.625 0.32812 2.2109 0.91406 0.58594 0.58594 0.91406 1.3828 0.91406 2.2109v31.25c0 1.7266-1.3984 3.125-3.125 3.125s-3.125-1.3984-3.125-3.125zm-56.25 1.832h-15.633c-5.1719 0-9.3672 4.1797-9.3672 9.3516v56.305c0 5.1562 4.2422 9.3516 9.3867 9.3516h56.219c2.4922 0 4.8828-0.98437 6.6406-2.7461 1.7617-1.7617 2.75-4.1523 2.7461-6.6445v-15.613 0.003906c0-1.7266-1.3984-3.125-3.125-3.125-1.7227 0-3.125 1.3984-3.125 3.125v15.613-0.003906c0.003906 0.83594-0.32422 1.6328-0.91406 2.2227s-1.3906 0.91797-2.2227 0.91797h-56.219c-1.7148-0.007812-3.1094-1.3867-3.1367-3.1016v-56.305c0-1.7148 1.3945-3.1016 3.1172-3.1016h15.633c1.7266 0 3.125-1.3984 3.125-3.125s-1.3984-3.125-3.125-3.125z\"/>\n" +
            "</svg>";
    private static final int BAR_WIDTH = 800;
    private static final int BAR_HEIGHT = 42;
    public static final String REPOSITORIES_COLOR = "#EADDCA";
    public static final String MAIN_LOC_FRESH_COLOR = "#E0FFFF";
    public static final String MAIN_LOC_COLOR = "#D6E4E1";
    public static final String TEST_LOC_COLOR = "#f0f0f0";
    public static final String PEOPLE_COLOR = "#ADD8E6";
    private final TagMap customTagsMap;
    private final TeamsConfig teamsConfig;
    private final LandscapeReportContributorsTab landscapeReportContributorsTab;
    private final LandscapeReportPeopleTopologyTab contributorsTopologyTab;
    private final LandscapeReportPeopleTopologyTab teamsTopologyTab;
    private final LandscapeReportContributorsTab landscapeReportTeamsTab;
    private TagMap extensionsTagsMap;
    private List<TagGroup> extensionTagGroups;
    private RichTextReport landscapeReport = new RichTextReport("Landscape Report", "index.html");
    private RichTextReport landscapeRepositoriesReportShort = new RichTextReport("", "repositories-short.html");

    private RichTextReport landscapeRepositoriesTags = new RichTextReport("", "repositories-tags.html");
    private RichTextReport landscapeRepositoriesTagsMatrix = new RichTextReport("", "repositories-tags-matrix.html");

    private RichTextReport landscapeRepositoriesExtensionTags = new RichTextReport("", "repositories-extensions.html");
    private RichTextReport
            landscapeRepositoriesExtensionTagsMatrix = new RichTextReport("", "repositories-extensions-matrix.html");
    private RichTextReport landscapeRepositoriesReportLong = new RichTextReport("", "repositories.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<TagGroup> tagGroups;
    private File folder;
    private File reportsFolder;
    private Map<String, List<String>> contributorsPerWeekMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerWeekMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerDayMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerDayMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerMonthMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerMonthMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerYearMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerYearMap = new HashMap<>();
    private SourceFileAgeDistribution overallFileLastModifiedDistribution;
    private SourceFileAgeDistribution overallFileFirstModifiedDistribution;

    public LandscapeReportGenerator(LandscapeAnalysisResults analysisResults, List<TagGroup> tagGroups, File folder, File reportsFolder) {
        this.tagGroups = tagGroups;
        this.teamsConfig = analysisResults.getTeamsConfig();
        this.folder = folder;
        this.reportsFolder = reportsFolder;

        this.landscapeAnalysisResults = analysisResults;

        overallFileFirstModifiedDistribution = analysisResults.getOverallFileFirstModifiedDistribution();
        overallFileLastModifiedDistribution = analysisResults.getOverallFileLastModifiedDistribution();
        populateTimeSlotMaps();

        landscapeReportContributorsTab = new LandscapeReportContributorsTab(analysisResults, analysisResults.getContributors(), landscapeReport, folder, reportsFolder, LandscapeReportContributorsTab.Type.CONTRIBUTORS, teamsConfig);
        landscapeReportTeamsTab = new LandscapeReportContributorsTab(analysisResults, analysisResults.getTeams(), landscapeReport, folder, reportsFolder, LandscapeReportContributorsTab.Type.TEAMS, teamsConfig);

        contributorsTopologyTab = new LandscapeReportPeopleTopologyTab(analysisResults, analysisResults.getContributors(), landscapeReport, folder, reportsFolder, LandscapeReportContributorsTab.Type.CONTRIBUTORS, teamsConfig);
        teamsTopologyTab = new LandscapeReportPeopleTopologyTab(analysisResults, analysisResults.getTeams(), landscapeReport, folder, reportsFolder, LandscapeReportContributorsTab.Type.TEAMS, teamsConfig);

        landscapeRepositoriesReportShort.setEmbedded(true);
        landscapeRepositoriesReportLong.setEmbedded(true);

        LOG.info("Exporting repositories...");
        List<RepositoryAnalysisResults> repositories = getRepositories();

        customTagsMap = updateTagsData(analysisResults, tagGroups, repositories);

        exportData(analysisResults, folder);

        addReportHead();
        addDescription();
        addLinks();

        landscapeReport.addLineBreak();

        addTabsLine();


        addOverviewTab();
        addSublandscapesTab();
        addRepositoriesTab(repositories);
        addStatsTab();
        addRepositoryTab(repositories);
        addTagsTab(repositories);
        addPromptsTab();

        landscapeReportContributorsTab.addContributorsTabs(CONTRIBUTORS_TAB_ID);
        if (teamsConfig.getTeams().size() > 0) {
            landscapeReportTeamsTab.addContributorsTabs(TEAMS_TAB_ID);
        }

        addTopologyTag(TOPOLOGIES_TAB_ID);

        addCustomTabs();

        String generationDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        landscapeReport.addContentInDiv("generated by <a target='_blank' href='https://sokrates.dev/'>sokrates.dev</a> " +
                        " (<a href='config.json' target='_blank'>configuration</a> | <a href='config-tags.json' target='_blank'>tag definitions</a> | <a href='config-teams.json' target='_blank'>team definitions</a>) " +
                        " on " + generationDate,
                "color: grey; font-size: 80%; margin: 10px");
        LOG.info("Done report generation.");
    }

    void addTopologyTag(String tabId) {
        landscapeReport.startTabContentSection(tabId, false);

        ProcessingStopwatch.start("reporting/team topologies");
        LOG.info("Adding Contributor Dependencies...");

        if (teamsConfig.getTeams().size() > 0) {
            teamsTopologyTab.addPeopleInfoBlock();
        }
        contributorsTopologyTab.addPeopleInfoBlock();

        int repositoriesCount = landscapeAnalysisResults.getRepositoriesCount();
        addRepositoriesInfoBlockWithColor(FormattingUtils.getSmallTextForNumber(repositoriesCount), repositoriesCount == 1 ? "repository" : "repositories", "", "", REPOSITORIES_COLOR);

        contributorsTopologyTab.render30DaysTopology();
        if (teamsConfig.getTeams().size() > 0) {
            teamsTopologyTab.render30DaysTopology();
        }
        contributorsTopologyTab.renderRepoAndKnowlegeTopologies();
        contributorsTopologyTab.renderDetails();

        if (teamsConfig.getTeams().size() > 0) {
            // set dummy report to render graphs, but not add section in the HTML report
            // teamsTopologyTab.setLandscapeReport(new RichTextReport("dummy report", ""));

            teamsTopologyTab.renderDetails();

            // teamsTopologyTab.setLandscapeReport(landscapeReport);
        }

        ProcessingStopwatch.end("reporting/team topologies");

        landscapeReport.endTabContentSection();
    }


    private void addReportHead() {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        Metadata metadata = configuration.getMetadata();
        String landscapeName = metadata.getName();
        if (StringUtils.isNotBlank(landscapeName)) {
            landscapeReport.setDisplayName(landscapeName);
        }
        landscapeReport.setParentUrl(configuration.getParentUrl());
        String logoLink = metadata.getLogoLink();
        if (StringUtils.isBlank(logoLink)) {
            logoLink = "https://zeljkoobrenovic.github.io/sokrates-media/icons/landscape.png";
        }
        landscapeReport.setLogoLink(logoLink);
        landscapeReport.setBreadcrumbs(configuration.getBreadcrumbs());
    }

    private void exportData(LandscapeAnalysisResults landscapeAnalysisResults, File folder) {
        LandscapeDataExport dataExport = new LandscapeDataExport(landscapeAnalysisResults, folder);
        dataExport.exportRepositories(customTagsMap);
        LOG.info("Exporting contributors...");
        dataExport.exportContributors();
        LOG.info("Exporting teams...");
        dataExport.exportTeams(teamsConfig);
        LOG.info("Exporting analysis results...");
        dataExport.exportAnalysisResults();
    }

    private void addDescription() {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        Metadata metadata = configuration.getMetadata();
        String description = metadata.getDescription();
        String tooltip = metadata.getTooltip();
        if (StringUtils.isNotBlank(description)) {
            if (StringUtils.isBlank(tooltip)) {
                landscapeReport.addParagraph(description, "font-size: 90%; color: #787878; margin-top: 5px; margin-bottom: 12px;");
            }
            if (StringUtils.isNotBlank(tooltip)) {
                landscapeReport.addParagraphWithTooltip(description, tooltip, "font-size: 90%; color: #787878; margin-top: 8px; margin-bottom: 12px;");
            }
        }
    }

    private void addLinks() {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        Metadata metadata = configuration.getMetadata();
        if (metadata.getLinks().size() > 0) {
            landscapeReport.startDiv("font-size: 80%; margin-top: 6px;");
            boolean first[] = {true};
            metadata.getLinks().forEach(link -> {
                if (!first[0]) {
                    landscapeReport.addHtmlContent(" | ");
                }
                landscapeReport.addNewTabLink(link.getLabel(), link.getHref());
                first[0] = false;
            });
            landscapeReport.endDiv();
        }
    }

    private void addTabsLine() {
        int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount(landscapeAnalysisResults.getContributors());
        int recentTeamsCount = landscapeAnalysisResults.getRecentContributorsCount(landscapeAnalysisResults.getTeams());
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        List<SubLandscapeLink> subLandscapes = configuration.getSubLandscapes();

        List<SubLandscapeLink> level1SubLandscapes = configuration.getSubLandscapes().stream().filter(l -> getPathDepth(l.getIndexFilePath()) == 1).collect(Collectors.toList());

        landscapeReport.startTabGroup();
        landscapeReport.addTab(OVERVIEW_TAB_ID, "Overview", true);
        if (subLandscapes.size() > 0) {
            landscapeReport.addTab(SUB_LANDSCAPES_TAB_ID, "Sub-Landscapes (" + (level1SubLandscapes.size() == 0 ? subLandscapes.size() : level1SubLandscapes.size()) + ")", false);
        }
        landscapeReport.addTab(REPOSITORIES_TAB_ID, "Repositories (" + landscapeAnalysisResults.getFilteredRepositoryAnalysisResults().size() + ")", false);
        landscapeReport.addTab(STATS_TAB_ID, "Statistics", false);

        landscapeReport.addTab(TAGS_TAB_ID, "Tech Stats", false);
        landscapeReport.addTab(CONTRIBUTORS_TAB_ID, "Contributors" + (recentContributorsCount > 0 ? " (" + recentContributorsCount + ")" + "" : ""), false);
        if (teamsConfig.getTeams().size() > 0) {
            landscapeReport.addTab(TEAMS_TAB_ID, "Teams" + (recentContributorsCount > 0 ? " (" + recentTeamsCount + ")" + "" : ""), false);
        }
        landscapeReport.addTab(TOPOLOGIES_TAB_ID, "Team Topology", false);
        configuration.getCustomTabs().forEach(tab -> {
            int index = configuration.getCustomTabs().indexOf(tab);
            landscapeReport.addTab(CUSTOM_TAB_ID_PREFIX + index, tab.getName(), false);
        });
        landscapeReport.addTab(PROMPTS_TAB_ID, "AI Prompts", false);
        landscapeReport.endTabGroup();
    }

    private void addCustomTabs() {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        configuration.getCustomTabs().forEach(tab -> {
            int index = configuration.getCustomTabs().indexOf(tab);
            landscapeReport.startTabContentSection(CUSTOM_TAB_ID_PREFIX + index, false);
            landscapeReport.addLineBreak();
            addIFrames(tab.getiFrames());
            landscapeReport.endTabContentSection();
        });
    }

    private void addTagsTab(List<RepositoryAnalysisResults> repositories) {
        landscapeReport.startTabContentSection(TAGS_TAB_ID, false);

        landscapeReport.addLineBreak();
        landscapeReport.startSubSection("<a href='repositories-extensions.html' target='_blank' style='text-decoration: none'>" +
                "File Extension Stats</a>&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "");
        landscapeReport.startDiv("margin-bottom: 18px;");
        landscapeReport.addNewTabLink("<b>Open expanded view</b> (stats per sub-folder)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-extensions-matrix.html");
        landscapeReport.endDiv();
        landscapeReport.addHtmlContent("<iframe src='repositories-extensions.html' frameborder=0 style='height: 600px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");
        landscapeReport.endSection();

        ProcessingStopwatch.start("reporting/tags");
        landscapeReport.addLineBreak();
        addTagsSection(repositories);


        ProcessingStopwatch.end("reporting/tags");
        landscapeReport.endTabContentSection();
    }

    private void addPromptsTab() {
        ProcessingStopwatch.start("reporting/prompts");

        landscapeReport.startTabContentSection(PROMPTS_TAB_ID, false);
        landscapeReport.startDiv("margin: 20px;");
        landscapeReport.addParagraph("Generative AI tools, like ChatGPT or Gemini, can help you explore and discuss various aspects of source code repositories using simple prompts and file uploads. Sokrates provides you with curated data that you can use to analyze your source code further.", "");

        PromptsUtils.addLandscapePromptSection("landscape-repository-insights", landscapeReport, landscapeAnalysisResults, "Prompt 1: Simple Repository Insights (based on repository names and basic stats)", "", Arrays.asList(new Link("repositories.txt", "data/repositories.txt"), new Link("repositories.json", "data/repositories.json")));

        PromptsUtils.addLandscapePromptSection("landscape-commits-analyzer", landscapeReport, landscapeAnalysisResults, "Prompt 2: Simple Commits & Contributor Insights", "", Arrays.asList(new Link("contributors.txt", "data/contributors.txt"), new Link("contributors.json", "data/contributors.json")));

        ProcessingStopwatch.end("reporting/prompts");

        landscapeReport.endDiv();
        landscapeReport.endTabContentSection();
    }

    private void addRepositoryTab(List<RepositoryAnalysisResults> repositories) {
        LOG.info("Adding repository section...");
        ProcessingStopwatch.start("reporting/repositories");
        addRepositoriesStatisticsSection(repositories);
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesRepositories());
        ProcessingStopwatch.end("reporting/repositories");
        landscapeReport.endTabContentSection();
    }

    private void addStatsTab() {
        landscapeReport.startTabContentSection(STATS_TAB_ID, false);
        LOG.info("Adding stats...");
        addBigRepositoriesSummary(landscapeAnalysisResults);
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesRepositoriesAtStart());
        if (!landscapeAnalysisResults.getConfiguration().isShowExtensionsOnFirstTab()) {
            LOG.info("Adding extensions...");
            addExtensions();
        }
    }

    private void addRepositoriesTab(List<RepositoryAnalysisResults> repositories) {
        landscapeReport.startTabContentSection(REPOSITORIES_TAB_ID, false);
        LOG.info("Adding repository section...");
        ProcessingStopwatch.start("reporting/repositories");
        addRepositoriesSection(repositories);
        ProcessingStopwatch.end("reporting/repositories");
        landscapeReport.endTabContentSection();
    }

    private void addSublandscapesTab() {
        List<SubLandscapeLink> subLandscapes = landscapeAnalysisResults.getConfiguration().getSubLandscapes();
        if (subLandscapes.size() > 0) {
            landscapeReport.startTabContentSection(SUB_LANDSCAPES_TAB_ID, false);
            ProcessingStopwatch.start("reporting/sub-landscapes");
            LOG.info("Adding sub landscape section...");
            addSubLandscapeSection(subLandscapes);
            WebFrameLink iframe = new WebFrameLink();
            iframe.setSrc("visuals/sub_landscapes_zoomable_circles_main_loc_.html");
            iframe.setMoreInfoLink("visuals/sub_landscapes_zoomable_circles_main_loc_.html");
            iframe.setTitle("Sub-Landscape repositories (by size)");
            iframe.setStyle("width: 100%; height: 970px;");
            iframe.setScrolling(false);
            addIFrame(iframe);
            ProcessingStopwatch.end("reporting/sub-landscapes");

            landscapeReport.startSubSection("Level 1 Sub-Landscape Dependencies", "");
            landscapeReport.startSubSection("Via Recent Contributors (30 days)", "");
            renderSubLandscapeDependenciesViaContributors();
            landscapeReport.endSection();
            landscapeReport.startSubSection("Via Same Repository Names", "");
            renderSubLandscapeDependenciesViaRepoName();
            landscapeReport.endSection();
            landscapeReport.endSection();

            landscapeReport.endTabContentSection();
        }
    }

    private void addOverviewTab() {
        ProcessingStopwatch.start("reporting/big summary");
        landscapeReport.startTabContentSection(OVERVIEW_TAB_ID, true);
        ProcessingStopwatch.start("reporting/overview");
        addBigSummary(landscapeAnalysisResults);
        if (landscapeAnalysisResults.getConfiguration().isShowExtensionsOnFirstTab()) {
            addExtensions();
        }
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFrames());
        ProcessingStopwatch.end("reporting/overview");
        landscapeReport.endTabContentSection();
        ProcessingStopwatch.end("reporting/big summary");
    }

    private void renderSubLandscapeDependenciesViaContributors() {
        GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
        renderer.setMaxNumberOfDependencies(100);
        renderer.setDefaultNodeFillColor("deepskyblue2");
        renderer.setOrientation("LR");
        renderer.setTypeGraph();
        List<ComponentDependency> dependencies = landscapeAnalysisResults.getSubLandscapeDependenciesViaRepositoriesWithSameContributors();
        String graphvizContent = renderer.getGraphvizContent(landscapeAnalysisResults.getLevel1SubLandscapes().stream().map(s -> "[" + s + "]").collect(Collectors.toCollection(ArrayList::new)), landscapeAnalysisResults.getSubLandscapeIndirectDependenciesViaRepositoriesWithSameContributors());

        landscapeReport.startShowMoreBlock("show sub-landscape/repository dependencies...");
        landscapeReport.addGraphvizFigure("sub_landscape_dependencies_same_contributors", "Extension dependencies", graphvizContent);
        addDownloadLinks("sub_landscape_dependencies_same_contributors");
        landscapeReport.endShowMoreBlock();
        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink(" - show dependencies as 2D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/sub_landscape_dependencies_same_contributors_force_2d.html");
        landscapeReport.addNewTabLink(" - show dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/sub_landscape_dependencies_same_contributors_force_3d.html");
        new Force3DGraphExporter().export2D3DForceGraph(dependencies, reportsFolder, "sub_landscape_dependencies_same_contributors");

    }

    private void renderSubLandscapeDependenciesViaRepoName() {
        GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
        renderer.setMaxNumberOfDependencies(100);
        renderer.setDefaultNodeFillColor("deepskyblue2");
        renderer.setOrientation("LR");
        renderer.setTypeGraph();
        List<ComponentDependency> dependencies = landscapeAnalysisResults.getSubLandscapeDependenciesViaRepositoriesWithSameName();
        String graphvizContent = renderer.getGraphvizContent(landscapeAnalysisResults.getLevel1SubLandscapes().stream().map(s -> "[" + s + "]").collect(Collectors.toCollection(ArrayList::new)), landscapeAnalysisResults.getSubLandscapeIndirectDependenciesViaRepositoriesWithSameName());

        landscapeReport.startShowMoreBlock("show sub-landscape/repository dependencies...");
        landscapeReport.addGraphvizFigure("sub_landscape_dependencies_same_name_repos", "Extension dependencies", graphvizContent);
        addDownloadLinks("sub_landscape_dependencies_same_name_repos");
        landscapeReport.endShowMoreBlock();
        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink(" - show dependencies as 2D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/sub_landscape_dependencies_same_name_repos_force_2d.html");
        landscapeReport.addNewTabLink(" - show dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/sub_landscape_dependencies_same_name_repos_force_3d.html");
        new Force3DGraphExporter().export2D3DForceGraph(dependencies, reportsFolder, "sub_landscape_dependencies_same_name_repos");

    }

    private void getHiddenFilesTagGroup(List<RepositoryAnalysisResults> repositories, List<TagGroup> extensionTagGroups) {
        Set<String> hiddenFiles = new HashSet<>();
        Set<String> hiddenFolders = new HashSet<>();
        repositories.forEach(repository -> {
            repository.getFiles().forEach(path -> {
                File file = new File(path.getPath());
                String name = file.getName();
                if (name.startsWith(".")) {
                    hiddenFiles.add(name);
                }
                File parentFile = file.getParentFile();
                while (parentFile != null) {
                    if (parentFile.getName().startsWith(".")) {
                        hiddenFolders.add(parentFile.getName());
                    }
                    parentFile = parentFile.getParentFile();
                }
            });
        });

        TagGroup hiddenFoldersTags = new TagGroup("hidden folders");
        hiddenFoldersTags.setDescription("folders with \".*\" like names");
        hiddenFoldersTags.setColor("lightgrey");

        hiddenFolders.forEach(hiddenFolder -> {
            RepositoryTag tag = new RepositoryTag();
            tag.setGroup(hiddenFoldersTags);
            tag.setTag(hiddenFolder);
            tag.getPathPatterns().add("(|\\/)" + hiddenFolder.replaceAll("\\.", "[.]").replaceAll("\\-", "[-]") + "/.*");
            hiddenFoldersTags.getRepositoryTags().add(tag);
        });

        TagGroup hiddenFileTags = new TagGroup("hidden files");
        hiddenFileTags.setDescription("files with \".*\" like names");
        hiddenFileTags.setColor("lightgrey");

        hiddenFiles.forEach(hiddenFile -> {
            RepositoryTag tag = new RepositoryTag();
            tag.setGroup(hiddenFileTags);
            tag.setTag(hiddenFile);
            tag.getPathPatterns().add("(|\\/)" + hiddenFile.replaceAll("\\.", "[.]").replaceAll("\\-", "[-]"));
            hiddenFileTags.getRepositoryTags().add(tag);
        });

        extensionTagGroups.add(hiddenFoldersTags);
        extensionTagGroups.add(hiddenFileTags);
    }

    private TagMap updateTagsData(LandscapeAnalysisResults landscapeAnalysisResults, List<TagGroup> tagGroups, List<RepositoryAnalysisResults> repositories) {
        final TagMap customTagsMap;
        ProcessingStopwatch.start("reporting/tags/custom tags map");
        customTagsMap = new TagMap(landscapeAnalysisResults, tagGroups);
        customTagsMap.updateTagMap(repositories);
        ProcessingStopwatch.end("reporting/tags/custom tags map");

        ProcessingStopwatch.start("reporting/tags/extensions tags map");
        extensionTagGroups = getExtensionTagGroups();
        getHiddenFilesTagGroup(repositories, extensionTagGroups);
        extensionsTagsMap = new TagMap(landscapeAnalysisResults, extensionTagGroups);
        extensionsTagsMap.updateTagMap(repositories);
        ProcessingStopwatch.end("reporting/tags/extensions tags map");


        return customTagsMap;
    }

    public static List<ContributionTimeSlot> getContributionDays(List<ContributionTimeSlot> contributorsPerDayOriginal, int pastDays, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>(contributorsPerDayOriginal);
        List<String> slots = contributorsPerDay.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastDays(pastDays, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerDay.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerDay;
    }

    public static List<ContributionTimeSlot> getContributionWeeks(List<ContributionTimeSlot> contributorsPerWeekOriginal, int pastWeeks, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>(contributorsPerWeekOriginal);
        List<String> slots = contributorsPerWeek.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerWeek;
    }

    public static List<ContributionTimeSlot> getContributionYears(List<ContributionTimeSlot> contributorsPerWeekOriginal, int pastYears, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>(contributorsPerWeekOriginal);
        List<String> slots = contributorsPerWeek.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastYears(pastYears, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerWeek;
    }

    private List<RepositoryAnalysisResults> getRepositories() {
        return landscapeAnalysisResults.getFilteredRepositoryAnalysisResults();
    }

    private int getPathDepth(String path) {
        return path.replace("\\", "/")
                .replace("/_sokrates_landscape/index.html", "")
                .split("/").length;
    }

    private void addSubLandscapeSection(List<SubLandscapeLink> subLandscapes) {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        final int maxDepth = configuration.getMaxSublandscapeDepth();
        List<SubLandscapeLink> links = subLandscapes.stream().filter(l -> maxDepth == 0 || getPathDepth(l.getIndexFilePath()) <= maxDepth).collect(Collectors.toList());
        if (links.size() > 0) {
            Collections.sort(links, Comparator.comparing(a -> getLabel(a).toLowerCase()));
            landscapeReport.startDiv("margin: 12px; margin-bottom: 22px");

            landscapeReport.addHtmlContent("zoomable circles: ");
            landscapeReport.addNewTabLink("contributors (30d)", "visuals/sub_landscapes_zoomable_circles_" + CONTRIBUTORS_30_D + ".html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("commits (30d)", "visuals/sub_landscapes_zoomable_circles_" + COMMITS_30_D + ".html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("lines of code (main)", "visuals/sub_landscapes_zoomable_circles_" + MAIN_LOC + ".html");
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("zoomable sunburst: ");
            landscapeReport.addNewTabLink("contributors (30d)", "visuals/sub_landscapes_zoomable_sunburst_" + CONTRIBUTORS_30_D + ".html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("commits (30d)", "visuals/sub_landscapes_zoomable_sunburst_" + COMMITS_30_D + ".html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("lines of code (main)", "visuals/sub_landscapes_zoomable_sunburst_" + MAIN_LOC + ".html");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();

            landscapeReport.startTable();
            landscapeReport.addTableHeader("", "", "repositories", "main loc", "test loc", "other loc", "commits<br>(all time)", "contributors<br>(30 days)", "commits<br>(30 days)", "commit period");
            String prevRoot[] = {""};
            List<LandscapeAnalysisResultsReadData> loadedSubLandscapes = new ArrayList<>();
            links.stream().sorted((a, b) -> compareSubLandscapeLinks(a, b)).forEach(subLandscape -> {
                LOG.info("Adding " + subLandscape.getIndexFilePath());
                String labelText = StringUtils.removeEnd(getLabel(subLandscape), "/");
                String label = labelText;
                String style = "";
                String root = label.replaceAll("/.*", "");
                boolean isRoot;
                if (!prevRoot[0].equals(root)) {
                    isRoot = true;
                    label = "<b>" + label + "</b>";
                    style = "color: black; font-weight: bold;";
                } else {
                    isRoot = false;
                    int lastIndex = label.lastIndexOf("/");
                    label = "<span style='color: lightgrey'>" + label.substring(0, lastIndex + 1) + "</span>" + label.substring(lastIndex + 1) + "";
                    style = "color: grey; font-size: 90%";
                }
                String href = configuration.getRepositoryReportsUrlPrefix() + subLandscape.getIndexFilePath();
                LandscapeAnalysisResultsReadData subLandscapeAnalysisResults = getSubLandscapeAnalysisResults(subLandscape);
                landscapeReport.startTableRow(style);
                LandscapeConfiguration subLandscapeConfig = getSubLandscapeConfig(subLandscape);
                Metadata metadata = subLandscapeConfig.getMetadata();
                landscapeReport.addTableCell(!labelText.contains("/") ? ("<a href='" + href + "' target='_blank'>" +
                        (StringUtils.isNotBlank(metadata.getLogoLink())
                                ? "<img src='" + getLogoLink(configuration.getRepositoryReportsUrlPrefix() + subLandscape.getIndexFilePath().replace("/index.html", ""), metadata.getLogoLink()) + "' " +
                                "style='vertical-align: middle; width: 24px' " +
                                "onerror=\"this.onerror=null;this.src='https://zeljkoobrenovic.github.io/sokrates-media/icons/landscape.png'\">"
                                : "<img src='https://zeljkoobrenovic.github.io/sokrates-media/icons/landscape.png' style='vertical-align: middle; width: 24px'>") +
                        "</a>") : "", "text-align: center;");

                landscapeReport.startTableCell();
                landscapeReport.addNewTabLink(label, href);
                loadedSubLandscapes.add(subLandscapeAnalysisResults);
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getRepositoriesCount()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getMainLoc()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getTestLoc()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    int other = subLandscapeAnalysisResults.getBuildAndDeploymentLoc()
                            + subLandscapeAnalysisResults.getGeneratedLoc() + subLandscapeAnalysisResults.getOtherLoc();
                    landscapeReport.addHtmlContent("<span style='color: lightgrey'>" + FormattingUtils.formatCount(other) + "</span>");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getCommitsCount()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getRecentContributorsCount()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getCommitsCount30Days()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right; font-size: 70%");
                if (subLandscapeAnalysisResults != null) {
                    String firstYear = DateUtils.getYear(subLandscapeAnalysisResults.getFirstCommitDate());
                    String lastYear = DateUtils.getYear(subLandscapeAnalysisResults.getLatestCommitDate());
                    landscapeReport.addHtmlContent(firstYear);
                    landscapeReport.addHtmlContent("-");
                    landscapeReport.addHtmlContent(lastYear);
                    try {
                        int first = Integer.parseInt(firstYear);
                        int last = Integer.parseInt(lastYear);
                        if (last >= first) {
                            int width = Math.min(20, last - first + 1) * 7;
                            String periodStyle = "margin-left: auto; margin-right: 0; margin-top: 2px; padding: 0; width: " + width + "px;";
                            if (!isRoot) {
                                periodStyle += "background-color: lightgrey; height: 4px;";
                            } else {
                                periodStyle += "height: 7px; background-color: green;";
                            }
                            landscapeReport.addContentInDiv("", periodStyle);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
                landscapeReport.endTableCell();
                landscapeReport.endTableRow();

                prevRoot[0] = root;
            });
            landscapeReport.endTable();

            landscapeReport.endDiv();
        }

    }

    private int compareSubLandscapeLinks(SubLandscapeLink a, SubLandscapeLink b) {
        if (a.getLandscapeAnalysisResults() != null && b.getLandscapeAnalysisResults() != null) {
            return b.getLandscapeAnalysisResults().getFirstCommitDate().compareTo(a.getLandscapeAnalysisResults().getFirstCommitDate());
        }

        return 0;
    }

    private String getLogoLink(String repositoryLinkPrefix, String link) {
        return link.startsWith("/") || link.contains("://") || link.startsWith("data:image")
                ? link
                : StringUtils.appendIfMissing(repositoryLinkPrefix, "/") + link;
    }

    private VisualizationItem getParent(Map<String, VisualizationItem> parents, List<String> pathElements) {
        String parentName = "";
        for (int i = 0; i < pathElements.size() - 1; i++) {
            if (parentName.length() > 0) {
                parentName += "/";
            }
            parentName += pathElements.get(i);
        }

        if (parents.containsKey(parentName)) {
            return parents.get(parentName);
        }

        VisualizationItem newParent = new VisualizationItem(parentName, 0);
        parents.put(parentName, newParent);

        if (parentName.length() > 0) {
            getParent(parents, pathElements.subList(0, pathElements.size() - 1)).getChildren().add(newParent);
        }

        return newParent;
    }

    private void exportZoomableCircles(String type, List<RepositoryAnalysisResults> repositoryAnalysisResults, ZommableCircleCountExtractors zommableCircleCountExtractors) {
        Map<String, VisualizationItem> parents = new HashMap<>();
        VisualizationItem root = new VisualizationItem("", 0);
        parents.put("", root);

        repositoryAnalysisResults.forEach(analysisResults -> {
            String name = getRepositoryCircleName(analysisResults);
            String[] elements = name.split("/");
            LOG.info(name);
            if (elements.length > 1) {
                name = name.substring(elements[0].length() + 1);
            }
            int count = zommableCircleCountExtractors.getCount(analysisResults);
            if (count > 0) {
                VisualizationItem item = new VisualizationItem(name + " (" + FormattingUtils.getPlainTextForNumber(count) + ")", count);
                getParent(parents, Arrays.asList(elements)).getChildren().add(item);
            }
        });
        try {
            File folder = new File(reportsFolder, "visuals");
            folder.mkdirs();
            FileUtils.write(new File(folder, "sub_landscapes_zoomable_circles_" + type + ".html"), new VisualizationTemplate().renderZoomableCircles(root.getChildren()), UTF_8);
            FileUtils.write(new File(folder, "sub_landscapes_zoomable_sunburst_" + type + ".html"), new VisualizationTemplate().renderZoomableSunburst(root.getChildren()), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRepositoryCircleName(RepositoryAnalysisResults analysisResults) {
        String name = analysisResults.getSokratesRepositoryLink().getAnalysisResultsPath().replace("\\", "/");
        name = name.replace("/data/analysisResults.json", "");
        return name;
    }

    private LandscapeAnalysisResultsReadData getSubLandscapeAnalysisResults(SubLandscapeLink subLandscape) {
        try {
            String prefix = landscapeAnalysisResults.getConfiguration().getRepositoryReportsUrlPrefix();
            File resultsFile = new File(new File(folder, prefix + subLandscape.getIndexFilePath()).getParentFile(), "data/landscapeAnalysisResults.json");
            LOG.info(resultsFile.getPath());
            String json = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
            return (LandscapeAnalysisResultsReadData) new JsonMapper().getObject(json, LandscapeAnalysisResultsReadData.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private LandscapeConfiguration getSubLandscapeConfig(SubLandscapeLink subLandscape) {
        try {
            String prefix = landscapeAnalysisResults.getConfiguration().getRepositoryReportsUrlPrefix();
            File resultsFile = new File(new File(folder, prefix + subLandscape.getIndexFilePath()).getParentFile(), "config.json");
            LOG.info(resultsFile.getPath());
            String json = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
            return (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getLabel(SubLandscapeLink subLandscape) {
        return subLandscape.getIndexFilePath()
                .replaceAll("(/|\\\\)_sokrates_landscape(/|\\\\).*", "");
    }

    private void addBigSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        landscapeReport.startDiv("margin-top: 0px;");
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int size = getRepositories().size();
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(size), (size == 1 ? "repository" : "repositories"),
                "", "all repositories updated after " + configuration.getIgnoreRepositoriesLastUpdatedBefore() + " with at least " + FormattingUtils.formatCountPlural(configuration.getRepositoryThresholdContributors(), "contributor", "contributors"), REPOSITORIES_COLOR, "repository");
        addLocInfoBlock(landscapeAnalysisResults);
        int mainLoc1YearActive = landscapeAnalysisResults.getMainLoc1YearActive();
        int totalValue = getSumOfValues(overallFileLastModifiedDistribution);
        addActiveCodeBlock(landscapeAnalysisResults, totalValue);

        List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
        long contributorsCount = contributors.size();
        if (contributorsCount > 0) {
            int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount(contributors);
            int locPerRecentContributor = 0;
            if (recentContributorsCount > 0) {
                locPerRecentContributor = (int) Math.round((double) mainLoc1YearActive / recentContributorsCount);
            }
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "recent contributors",
                    "(past 30 days)", getExtraPeopleInfo(contributors, contributorsCount) + "\n" + FormattingUtils.formatCount(locPerRecentContributor) + " active lines of code per recent contributor");
            int rookiesContributorsCount = landscapeAnalysisResults.getRookiesContributorsCount(contributors);
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(rookiesContributorsCount),
                    rookiesContributorsCount == 1 ? "active rookie" : "active rookies",
                    "(started in past year)", "active contributors with the first commit in past year");
        }

        addContributorsPerYear(configuration.isShowContributorsTrendsOnFirstTab());

        landscapeReport.endDiv();
        landscapeReport.addLineBreak();
    }

    private void addBigRepositoriesSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        landscapeAnalysisResults.getRecentContributorsCount(landscapeAnalysisResults.getContributors());
        List<RepositoryAnalysisResults> repositories = getRepositories();
        int recentSize = (int) repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() > 0).count();
        int recentLoc = repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() > 0).map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);

        String style = "border-top: 2px solid lightgrey; border-right: 2px solid lightgrey; display: inline-block; margin-right: 8px";
        landscapeReport.startDiv(style);
        landscapeReport.addContentInDiv("active repositories", "text-align: center; margin-bottom: -7px; margin-top: 2px; margin-left: 4px; color: grey; font-size: 70%;");

        int size = repositories.size();
        int locAll = landscapeAnalysisResults.getMainLoc();
        int size90Days = (int) repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days() > 0).count();
        int loc90Days = repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days() > 0).map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        int size180Days = (int) repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days() > 0).count();
        int loc180Days = repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days() > 0).map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        int size365Days = (int) repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount365Days() > 0).count();
        int loc365Days = repositories.stream().filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount365Days() > 0).map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        String contributorConstraint = " with at least " + FormattingUtils.formatCountPlural(configuration.getRepositoryThresholdContributors(), "contributor", "repository");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size), "all time",
                FormattingUtils.getSmallTextForNumber(locAll) + " LOC",
                "all repositories updated after " + configuration.getIgnoreRepositoriesLastUpdatedBefore() + " with at least " + contributorConstraint, REPOSITORIES_COLOR, "repository");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size365Days), "past 365d",
                FormattingUtils.getSmallTextForNumber(loc365Days) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * loc365Days / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 365 days with at least " + contributorConstraint, REPOSITORIES_COLOR, "repository");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size180Days), "past 180d",
                FormattingUtils.getSmallTextForNumber(loc180Days) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * loc180Days / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 180 days with at least " + contributorConstraint, REPOSITORIES_COLOR, "repository");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size90Days), "past 90d",
                FormattingUtils.getSmallTextForNumber(loc180Days) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * loc90Days / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 90 days with at least " + contributorConstraint, REPOSITORIES_COLOR, "repository");
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(recentSize), "past 30d",
                FormattingUtils.getSmallTextForNumber(recentLoc) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * recentLoc / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 30 days with at least " + contributorConstraint, REPOSITORIES_COLOR, "repository");
        landscapeReport.endDiv();
        landscapeReport.startDiv(style);
        landscapeReport.addContentInDiv("size (LOC)", "text-align: center; margin-bottom: -7px; margin-top: 2px; margin-left: 4px; color: grey; font-size: 70%;");
        addLocInfoBlock(landscapeAnalysisResults);
        landscapeReport.endDiv();
        landscapeReport.startDiv(style);
        landscapeReport.addContentInDiv("1y code activity", "text-align: center; margin-bottom: -7px; margin-top: 2px; margin-left: 4px; color: grey; font-size: 70%;");
        int totalValue = getSumOfValues(overallFileLastModifiedDistribution);
        addActiveCodeBlock(landscapeAnalysisResults, totalValue);

        landscapeReport.endDiv();
    }

    private void addCorrelations() {
        List<RepositoryAnalysisResults> repositories = landscapeAnalysisResults.getRepositoryAnalysisResults();
        CorrelationDiagramGenerator<RepositoryAnalysisResults> correlationDiagramGenerator = new CorrelationDiagramGenerator<>(landscapeReport, repositories);

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Commits (30 days)", "commits (30d)", "recent contributors (30d)",
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Repository Main LOC", "main LOC", "recent contributors (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Recent Commits (30 days) vs. Repository Main LOC", "main LOC", "commits (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Age in Years vs. Repository Main LOC", "main LOC", "age (years)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> Math.round(10 * p.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays() / 365.0) / 10,
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Number of Files vs. Repository Main LOC", "main LOC", "# main files",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getFilesCount(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Duplication vs. Repository Main LOC", "main LOC", "% duplication",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> Math.round(10 * p.getAnalysisResults().getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue()) / 10,
                p -> p.getAnalysisResults().getMetadata().getName());
    }

    private void addActiveCodeBlock(LandscapeAnalysisResults landscapeAnalysisResults, int locAll) {
        int mainLocActive = landscapeAnalysisResults.getMainLoc1YearActive();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocActive), "main code touched", "1 year (" + FormattingUtils.getFormattedPercentage(100.0 * mainLocActive / Math.max(1, locAll)) + "%)",
                "files updated in past year", MAIN_LOC_FRESH_COLOR, "touch");
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocNew),
                "new main code", "1 year (+" + FormattingUtils.getFormattedPercentage(100.0 * mainLocNew / Math.max(1, locAll)) + "%)",
                "files created in past year", MAIN_LOC_FRESH_COLOR, "new");
    }

    private void addLocInfoBlock(LandscapeAnalysisResults landscapeAnalysisResults) {
        int mainLoc = landscapeAnalysisResults.getMainLoc();
        int secondaryLoc = landscapeAnalysisResults.getSecondaryLoc();
        int mainFilesCount = landscapeAnalysisResults.getMainFilesCount();
        int secondaryFilesCount = landscapeAnalysisResults.getSecondaryFilesCount();
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(mainLoc), "lines of main code", FormattingUtils.getSmallTextForNumber(mainFilesCount) + " files", "main lines of code", MAIN_LOC_COLOR, "main");
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(secondaryLoc), "lines of other code", FormattingUtils.getSmallTextForNumber(secondaryFilesCount) + " files", "test, build & deployment, generated, all other code in scope", TEST_LOC_COLOR, "build");
    }

    private void addIFrames(List<WebFrameLink> iframes) {
        if (iframes.size() > 0) {
            iframes.forEach(iframe -> {
                addIFrame(iframe);
            });
        }
    }

    private void addIFrame(WebFrameLink iframe) {
        if (StringUtils.isNotBlank(iframe.getTitle())) {
            String title;
            if (StringUtils.isNotBlank(iframe.getMoreInfoLink())) {
                title = "<a href='" + iframe.getMoreInfoLink() + "' target='_blank' style='text-decoration: none'>" + iframe.getTitle() + "</a>";
                title += "&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON;
            } else {
                title = iframe.getTitle();
            }
            landscapeReport.startSubSection(title, "");
        }
        String style = StringUtils.defaultIfBlank(iframe.getStyle(), "width: 100%; height: 200px; border: 1px solid lightgrey;");
        landscapeReport.addHtmlContent("<iframe src='" + iframe.getSrc()
                + "' frameborder='0' style='" + style + "'"
                + (iframe.getScrolling() ? "" : " scrolling='no' ")
                + "></iframe>");
        if (StringUtils.isNotBlank(iframe.getTitle())) {
            landscapeReport.endSection();
        }
    }

    private void addExtensions() {
        List<NumericMetric> linesOfCodePerExtensionMain = LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getMainLinesOfCodePerExtension());
        addMainExtensions("Main", linesOfCodePerExtensionMain, true);
        if (landscapeAnalysisResults.getRecentContributorsCount(landscapeAnalysisResults.getContributors()) > 0) {
            addContributorsPerExtension(true);
        }
        landscapeReport.startShowMoreBlockDisappear("", "&nbsp;&nbsp;>&nbsp;Show test and other code...");
        addMainExtensions("Test", LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getTestLinesOfCodePerExtension()), false);
        addMainExtensions("Other", LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getOtherLinesOfCodePerExtension()), false);
        landscapeReport.endShowMoreBlock();
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
        landscapeReport.startShowMoreBlockDisappear("", "&nbsp;&nbsp;>&nbsp;Show commit history per extension...");

        landscapeReport.startSubSection("Commit history per file extension", "");
        landscapeReport.startDiv("max-height: 600px; overflow-y: auto;");
        landscapeReport.startDiv("margin-bottom: 16px; vertical-align: middle;");
        landscapeReport.addContentInDiv(ReportConstants.ANIMATION_SVG_ICON, "display: inline-block; vertical-align: middle; margin: 4px;");
        landscapeReport.addHtmlContent("animated commit history: ");
        landscapeReport.addNewTabLink("all time cumulative", "visuals/racing_charts_extensions_commits.html?tickDuration=600");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("12 months window", "visuals/racing_charts_extensions_commits_window.html?tickDuration=600");
        landscapeReport.endDiv();
        List<String> extensions = linesOfCodePerExtensionMain.stream().map(loc -> loc.getName().replaceAll(".*[.]", "").trim()).collect(Collectors.toList());
        List<HistoryPerExtension> yearlyCommitHistoryPerExtension = landscapeAnalysisResults.getYearlyCommitHistoryPerExtension();
        HistoryPerLanguageGenerator.getInstanceCommits(yearlyCommitHistoryPerExtension, extensions).addHistoryPerLanguage(landscapeReport);
        new RacingLanguagesBarChartsExporter(landscapeAnalysisResults, yearlyCommitHistoryPerExtension, extensions).exportRacingChart(reportsFolder);
        landscapeReport.endDiv();
        landscapeReport.endSection();

        landscapeReport.endShowMoreBlock();

        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();

        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesAtStart());

        landscapeReport.startSubSection("Tags (" + customTagsMap.tagsCount() + ")", "");
        new LandscapeRepositoriesTagsLine(tagGroups, customTagsMap).addTagsLine(landscapeReport);
        landscapeReport.endSection();

        landscapeReport.addLineBreak();
    }

    private void addMainExtensions(String type, List<NumericMetric> linesOfCodePerExtension, boolean linkCharts) {
        int threshold = landscapeAnalysisResults.getConfiguration().getExtensionThresholdLoc();
        landscapeReport.startSubSection("Lines of Code in " + type + " Code (" + linesOfCodePerExtension.size() + ")",
                threshold >= 1 ? threshold + "+ lines of code" : "");
        if (linkCharts) {
            landscapeReport.startDiv("");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_extensions.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_extensions.html");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.endDiv();
        }
        landscapeReport.startDiv("");
        boolean tooLong = linesOfCodePerExtension.size() > 25;
        List<NumericMetric> linesOfCodePerExtensionDisplay = tooLong ? linesOfCodePerExtension.subList(0, 25) : linesOfCodePerExtension;
        List<NumericMetric> linesOfCodePerExtensionHide = tooLong ? linesOfCodePerExtension.subList(25, linesOfCodePerExtension.size()) : new ArrayList<>();
        linesOfCodePerExtensionDisplay.forEach(extension -> {
            addLangInfo(extension);
        });
        if (linesOfCodePerExtensionHide.size() > 0) {
            landscapeReport.startShowMoreBlockDisappear("", "show all...");
            linesOfCodePerExtensionHide.forEach(extension -> {
                addLangInfo(extension);
            });
            landscapeReport.endShowMoreBlock();
        }
        landscapeReport.endDiv();
        String excludedExtensions = landscapeAnalysisResults.getConfiguration().getIgnoreExtensions().stream().collect(Collectors.joining(", "));
        if (StringUtils.isNotBlank(excludedExtensions)) {
            landscapeReport.addParagraph("The following extensions are configured not to be displayed: [" + excludedExtensions + "]", "margin-top: 12px; font-size: 70%; color: grey;");
        }
        landscapeReport.endSection();
    }

    private void addContributorsPerExtension(boolean linkCharts) {
        landscapeReport.startSubSection("Contributors per File Extensions (past 30 days)", "");
        if (linkCharts) {
            landscapeReport.startDiv("");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_extensions_contributors_30d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_extensions_contributors_30d.html");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.endDiv();
        }
        landscapeReport.startDiv("");
        List<String> mainExtensions = getMainExtensions();
        List<CommitsPerExtension> contributorsPerExtension = landscapeAnalysisResults.getContributorsPerExtension()
                .stream().filter(c -> mainExtensions.contains(c.getExtension())).collect(Collectors.toList());
        Collections.sort(contributorsPerExtension, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());
        boolean tooLong = contributorsPerExtension.size() > 25;
        List<CommitsPerExtension> contributorsPerExtensionDisplay = tooLong ? contributorsPerExtension.subList(0, 25) : contributorsPerExtension;
        List<CommitsPerExtension> linesOfCodePerExtensionHide = tooLong ? contributorsPerExtension.subList(25, contributorsPerExtension.size()) : new ArrayList<>();
        contributorsPerExtensionDisplay.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(extension -> {
            addLangInfo(extension, (e) -> e.getCommitters30Days(), extension.getCommitsCount30Days(), DEVELOPER_SVG_ICON);
        });
        if (linesOfCodePerExtensionHide.stream().filter(e -> e.getCommitters30Days().size() > 0).count() > 0) {
            landscapeReport.startShowMoreBlockDisappear("", "show all...");
            linesOfCodePerExtensionHide.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(extension -> {
                addLangInfo(extension, (e) -> e.getCommitters30Days(), extension.getCommitsCount30Days(), DEVELOPER_SVG_ICON);
            });
            landscapeReport.endShowMoreBlock();
        }
        landscapeReport.endDiv();
        addContributorDependencies(contributorsPerExtension);
        landscapeReport.endSection();
    }

    private void addContributorDependencies(List<CommitsPerExtension> contributorsPerExtension) {
        Map<String, List<String>> contrExtMap = new HashMap<>();
        Set<String> extensionsNames = new HashSet<>();
        contributorsPerExtension.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(commitsPerExtension -> {
            String extensionDisplayLabel = commitsPerExtension.getExtension() + " (" + commitsPerExtension.getCommitters30Days().size() + ")";
            extensionsNames.add(extensionDisplayLabel);
            commitsPerExtension.getCommitters30Days().forEach(contributor -> {
                if (contrExtMap.containsKey(contributor)) {
                    contrExtMap.get(contributor).add(extensionDisplayLabel);
                } else {
                    contrExtMap.put(contributor, new ArrayList<>(Arrays.asList(extensionDisplayLabel)));
                }
            });
        });
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependencyMap = new HashMap<>();

        List<String> mainExtensions = getMainExtensions();
        contrExtMap.values().stream().filter(v -> v.size() > 1).forEach(extensions -> {
            extensions.stream().filter(extension1 -> mainExtensions.contains(extension1.replaceAll("\\(.*\\)", "").trim())).forEach(extension1 -> {
                extensions.stream().filter(extension2 -> mainExtensions.contains(extension2.replaceAll("\\(.*\\)", "").trim())).filter(extension2 -> !extension1.equalsIgnoreCase(extension2)).forEach(extension2 -> {
                    String key1 = extension1 + "::" + extension2;
                    String key2 = extension2 + "::" + extension1;

                    if (dependencyMap.containsKey(key1)) {
                        dependencyMap.get(key1).increment(1);
                    } else if (dependencyMap.containsKey(key2)) {
                        dependencyMap.get(key2).increment(1);
                    } else {
                        ComponentDependency dependency = new ComponentDependency(extension1, extension2);
                        dependencyMap.put(key1, dependency);
                        dependencies.add(dependency);
                    }
                });
            });
        });

        dependencies.forEach(dependency -> dependency.setCount(dependency.getCount() / 2));

        GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
        renderer.setMaxNumberOfDependencies(100);
        renderer.setDefaultNodeFillColor("deepskyblue2");
        renderer.setTypeGraph();
        String graphvizContent = renderer.getGraphvizContent(new ArrayList<>(extensionsNames), dependencies);

        landscapeReport.startShowMoreBlock("show extension dependencies...");
        landscapeReport.addGraphvizFigure("extension_dependencies_30d", "Extension dependencies", graphvizContent);
        addDownloadLinks("extension_dependencies_30d");
        landscapeReport.endShowMoreBlock();
        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink(" - show extension dependencies as 2D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_2d.html");
        landscapeReport.addNewTabLink(" - show extension dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_3d.html");
        new Force3DGraphExporter().export2D3DForceGraph(dependencies, reportsFolder, "extension_dependencies_30d");
    }

    private List<String> getMainExtensions() {
        return landscapeAnalysisResults.getMainLinesOfCodePerExtension().stream()
                .map(l -> l.getName().replace("*.", "").trim()).collect(Collectors.toList());
    }

    private void addLangInfo(CommitsPerExtension extension, ExtractStringListValue<CommitsPerExtension> extractor, int commitsCount, String suffix) {
        int size = extractor.getValue(extension).size();
        String smallTextForNumber = FormattingUtils.getSmallTextForNumber(size) + suffix;
        addLangInfoBlockExtra(smallTextForNumber, extension.getExtension().replace("*.", "").trim(),
                size + " " + (size == 1 ? "contributor" : "contributors (" + commitsCount + " commits)") + ":\n" +
                        extractor.getValue(extension).stream().limit(100)
                                .collect(Collectors.joining(", ")), FormattingUtils.getSmallTextForNumber(commitsCount) + " commits");
    }

    private void addLangInfo(NumericMetric extension) {
        String smallTextForNumber = FormattingUtils.getSmallTextForNumber(extension.getValue().intValue());
        int size = extension.getDescription().size();
        Collections.sort(extension.getDescription(), (a, b) -> b.getValue().intValue() - a.getValue().intValue());
        addLangInfoBlock(smallTextForNumber, extension.getName().replace("*.", "").trim(),
                size + " " + (size == 1 ? "repository" : "repositories") + ":\n  " +
                        extension.getDescription().stream()
                                .map(a -> a.getName() + " (" + FormattingUtils.formatCount(a.getValue().intValue()) + " LOC)")
                                .collect(Collectors.joining("\n  ")));
    }

    private void addRepositoriesSection(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        Collections.sort(repositoryAnalysisResults, (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();

        if (repositoryAnalysisResults.size() > 0) {
            int shortLimit = configuration.getRepositoriesShortListLimit();
            ProcessingStopwatch.start("reporting/repositories/short report");
            new LandscapeRepositoriesReport(landscapeAnalysisResults, shortLimit, "See the full list of repositories...", "repositories.html", customTagsMap)
                    .saveRepositoriesReport(landscapeRepositoriesReportShort, reportsFolder, repositoryAnalysisResults, tagGroups);
            ProcessingStopwatch.end("reporting/repositories/short report");

            List<NumericMetric> repositorySizes = new ArrayList<>();
            repositoryAnalysisResults.forEach(repository -> {
                LOG.info("Adding " + repository.getSokratesRepositoryLink().getAnalysisResultsPath());
                CodeAnalysisResults analysisResults = repository.getAnalysisResults();
                repositorySizes.add(new NumericMetric(analysisResults.getMetadata().getName(), analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            });

            landscapeReport.addHtmlContent("<iframe src='repositories-short.html' frameborder=0 style='height: calc(100vh - 300px); width: 100%; margin-left: 0; margin-bottom: 0px; padding: 0;'></iframe>");

            if (repositoryAnalysisResults.size() > shortLimit) {
                ProcessingStopwatch.start("reporting/repositories/long report");
                new LandscapeRepositoriesReport(landscapeAnalysisResults, configuration.getRepositoriesListLimit(), customTagsMap)
                        .saveRepositoriesReport(landscapeRepositoriesReportLong, reportsFolder, repositoryAnalysisResults, tagGroups);
                ProcessingStopwatch.end("reporting/repositories/long report");
            }
        }

        List<RepositoryAnalysisResults> ignoredRepositoriess = landscapeAnalysisResults.getIgnoredRepositoryAnalysisResults();
        if (ignoredRepositoriess.size() > 0) {
            String lastUpdatedBefore = configuration.getIgnoreRepositoriesLastUpdatedBefore();
            int thresholdContributors = configuration.getRepositoryThresholdContributors();
            int thresholdLocMain = configuration.getRepositoryThresholdLocMain();
            int ignoredLocMain = ignoredRepositoriess.stream().mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
            landscapeReport.addContentInDiv("<a href='data/ignoredRepositories.txt' target='_blank'>" + ignoredRepositoriess.size() +
                            " repositories (" + FormattingUtils.getSmallTextForNumber(ignoredLocMain) + " lines of main code) are ignored</a> based on any of the following criteria: " +
                            (StringUtils.isNoneBlank(lastUpdatedBefore) ? "not updated after " + lastUpdatedBefore + "; " : "") +
                            ((thresholdContributors > 0) ? "have < " + FormattingUtils.formatCountPlural(thresholdContributors, "contributor", "contributors") + "; " : "") +
                            (thresholdLocMain > 0 ? "have less than " + thresholdLocMain + " lines of main code" : ""),
                    "color: grey; margin: 10px; font-size: 80%");
        }

    }

    private void addRepositoriesStatisticsSection(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        Collections.sort(repositoryAnalysisResults, (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        ProcessingStopwatch.end("reporting/repositories/preparing");

        ProcessingStopwatch.start("reporting/repositories/export visuals");
        exportZoomableCircles(CONTRIBUTORS_30_D, repositoryAnalysisResults, new ZommableCircleCountExtractors() {
            @Override
            public int getCount(RepositoryAnalysisResults repositoryAnalysisResults) {
                List<ContributionTimeSlot> contributorsPerMonth = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults().getContributorsPerMonth();
                if (contributorsPerMonth.size() > 0) {
                    return contributorsPerMonth.get(0).getContributorsCount();
                }
                return 0;
            }
        });
        exportZoomableCircles(COMMITS_30_D, repositoryAnalysisResults, new ZommableCircleCountExtractors() {
            @Override
            public int getCount(RepositoryAnalysisResults repositoryAnalysisResults) {
                return repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
            }
        });
        exportZoomableCircles(MAIN_LOC, repositoryAnalysisResults, new ZommableCircleCountExtractors() {
            @Override
            public int getCount(RepositoryAnalysisResults repositoryAnalysisResults) {
                return repositoryAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            }
        });
        ProcessingStopwatch.end("reporting/repositories/export visuals");

        ProcessingStopwatch.start("reporting/repositories/file age & freshness");
        addFileAgeAndFreshnessSection();
        addZooSection();
        ProcessingStopwatch.end("reporting/repositories/file age & freshness");

        landscapeReport.startSubSection("Correlations", "");
        addCorrelations();
        landscapeReport.endSection();

    }

    private void addTagsSection(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        landscapeReport.startSubSection("Custom Tags (" + customTagsMap.tagsCount() + ")", "");

        if (repositoryAnalysisResults.size() > 0) {
            landscapeReport.startDiv("margin-top: 14px; max-height: 400px");
            landscapeReport.addNewTabLink("<b>Open in a new tab</b>&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-tags.html");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;");
            landscapeReport.addNewTabLink("<b>Open expanded view</b> (stats per sub-folder)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-tags-matrix.html");
            landscapeReport.endDiv();

            ProcessingStopwatch.start("reporting/repositories/tags");

            ProcessingStopwatch.start("reporting/tags/custom");
            new LandscapeRepositoriesTagsReport(landscapeAnalysisResults, tagGroups, customTagsMap, "custom", "repositories-tags-matrix.html", false)
                    .saveRepositoriesReport(landscapeRepositoriesTags, reportsFolder);
            ProcessingStopwatch.end("reporting/tags/custom");

            ProcessingStopwatch.start("reporting/tags/custom-matrix");
            new LandscapeRepositoriesTagsMatrixReport(landscapeAnalysisResults, tagGroups, customTagsMap, "custom-matrix", false)
                    .saveRepositoriesReport(landscapeRepositoriesTagsMatrix, "Custom Tags / Expanded View");
            ProcessingStopwatch.end("reporting/tags/custom-matrix");

            ProcessingStopwatch.start("reporting/tags/extensions");
            new LandscapeRepositoriesTagsReport(landscapeAnalysisResults, extensionTagGroups, extensionsTagsMap, "extension", "repositories-extensions-matrix.html", true)
                    .saveRepositoriesReport(landscapeRepositoriesExtensionTags, reportsFolder);
            ProcessingStopwatch.end("reporting/tags/extensions");

            ProcessingStopwatch.start("reporting/tags/extensions-matrix");
            new LandscapeRepositoriesTagsMatrixReport(landscapeAnalysisResults, extensionTagGroups, extensionsTagsMap, "extension-matrix", true)
                    .saveRepositoriesReport(landscapeRepositoriesExtensionTagsMatrix, "Extensions Tags / Expanded View");
            ProcessingStopwatch.end("reporting/tags/extensions-matrix");
            ProcessingStopwatch.end("reporting/repositories/tags");
        }

        landscapeReport.addLineBreak();
        landscapeReport.addHtmlContent("<iframe src='repositories-tags.html' frameborder=0 style='height: calc(100vh - 290px); width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

        landscapeReport.endSection();
    }

    private List<TagGroup> getExtensionTagGroups() {
        List<TagGroup> groups = new ArrayList<>();
        TagGroup mainProgrammingLanguages = new TagGroup("main file extensions", "#ffefd5");
        mainProgrammingLanguages.setDescription("file extensions with most lines of code in a repository");
        LandscapeGeneratorUtils.getLinesOfCodePerExtension(this.landscapeAnalysisResults, this.landscapeAnalysisResults.getMainLinesOfCodePerExtension()).forEach(extension -> {
            String lang = extension.getName().replaceAll(".*[.]", "").trim();
            RepositoryTag langTag = new RepositoryTag();
            langTag.setTag(lang);
            langTag.setMainExtensions(Arrays.asList(lang));
            langTag.setGroup(mainProgrammingLanguages);
            mainProgrammingLanguages.getRepositoryTags().add(langTag);
        });
        TagGroup programmingLanguages = new TagGroup("all file extensions", "#f0f0f0");
        programmingLanguages.setDescription("file extensions with at least one file in a repository");
        LandscapeGeneratorUtils.getLinesOfCodePerExtension(this.landscapeAnalysisResults, this.landscapeAnalysisResults.getMainLinesOfCodePerExtension()).forEach(extension -> {
            String lang = extension.getName().replaceAll(".*[.]", "").trim();
            RepositoryTag langTag = new RepositoryTag();
            langTag.setTag(lang);
            langTag.setAnyExtensions(Arrays.asList(lang));
            langTag.setGroup(programmingLanguages);
            programmingLanguages.getRepositoryTags().add(langTag);
        });

        groups.add(mainProgrammingLanguages);
        groups.add(programmingLanguages);

        return groups;
    }

    private void addFileAgeAndFreshnessSection() {
        landscapeReport.startSubSection("File Age and Freshness", "Lines of code in files first/last updated more than a year ago | 6 to 12 months ago | 3 to 6 months ago | 1 to 3 months ago | month or less ago");

        landscapeReport.startTable();
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("old", "border: none");
        landscapeReport.startTableCell("border: none");
        landscapeReport.startDivWithLabel("file age:\n" + overallFileFirstModifiedDistribution.getDescription(), "");
        landscapeReport.addHtmlContent(getRiskProfileVisual(overallFileFirstModifiedDistribution, Palette.getAgePalette()));
        landscapeReport.endDiv();
        landscapeReport.endTableCell();
        landscapeReport.addTableCell("new", "border: none");
        landscapeReport.endTableRow();

        landscapeReport.startTableRow();
        landscapeReport.addTableCell("stale", "border: none");
        landscapeReport.startTableCell("border: none");
        landscapeReport.startDivWithLabel("file freshness:\n" + overallFileLastModifiedDistribution.getDescription(), "");
        landscapeReport.addHtmlContent(getRiskProfileVisual(overallFileLastModifiedDistribution, Palette.getFreshnessPalette()));
        landscapeReport.endDiv();
        landscapeReport.endTableCell();
        landscapeReport.addTableCell("fresh", "border: none");
        landscapeReport.endTableRow();

        addNoHistoryRow();

        landscapeReport.endTable();

        landscapeReport.endSection();
    }

    public void addZooSection() {
        AnimalIcons icons = new AnimalIcons(64);
        List<String> animals = icons.getAnimals();
        List<String> animalsLocInfo = icons.getAnimalsLOCInfo();
        // Collections.reverse(animals);
        // Collections.reverse(animalsLocInfo);

        int totalLoc = landscapeAnalysisResults.getMainLoc();

        Map<String, List<RepositoryAnalysisResults>> animalCounts = new HashMap<>();
        List<RepositoryAnalysisResults> repositories = landscapeAnalysisResults.getRepositoryAnalysisResults();
        repositories.forEach(repositoryAnalysis -> {
            String animal = icons.getAnimalForMainLoc(repositoryAnalysis.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
            if (!animalCounts.containsKey(animal)) {
                animalCounts.put(animal, new ArrayList<>());
            }
            animalCounts.get(animal).add(repositoryAnalysis);
        });
        int maxCount[] = {0};
        animals.forEach(animal -> {
            int count = animalCounts.containsKey(animal) ? animalCounts.get(animal).size() : 0;
            maxCount[0] = Math.max(count, maxCount[0]);
        });

        landscapeReport.startSubSection("Repositories Size Distribution", "Size of repositories (main lines of code)");
        landscapeReport.startTable("font-size: 100%; margin-bottom: 6px;");

        landscapeReport.startTableRow();
        animalsLocInfo.forEach(animalInfo -> {
            landscapeReport.startTableCell("text-align: center; border: none; color: black;");
            landscapeReport.addContentInDiv(animalInfo);
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();

        landscapeReport.startTableRow();
        animals.forEach(animal -> {
            int count = animalCounts.containsKey(animal) ? animalCounts.get(animal).size() : 0;
            landscapeReport.startTableCell("vertical-align: bottom; text-align: center; border: none;" + (count > 0 ? "" : "color: grey; opacity: 0.4"));
            int loc = 0;
            if (count > 0) {
                loc += animalCounts.get(animal).stream()
                        .mapToInt(a -> a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                        .sum();
            }
            double percentage = totalLoc > 0 ? 100.0 * loc / totalLoc : 0;
            int width = totalLoc > 0 ? (200 * loc / totalLoc) : 0;
            if (count > 0 && width == 0) {
                width = 1;
            }
            landscapeReport.addContentInDiv(FormattingUtils.getFormattedPercentage(percentage) + "%", "font-size: 13px; ");
            landscapeReport.addContentInDiv(FormattingUtils.getSmallTextForNumber(loc) + "LOC", "font-size: 11px;");
            landscapeReport.startDiv("border: 1px solid #d0d0d0; width: 64px; margin-bottom: 4px; ");
            landscapeReport.addContentInDiv("", "background-color: blue; width: 100%; height: " + width + "px");
            landscapeReport.endDiv();
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();


        landscapeReport.startTableRow();
        animals.forEach(animal -> {
            int count = animalCounts.containsKey(animal) ? animalCounts.get(animal).size() : 0;
            landscapeReport.startTableCell("vertical-align: top; border: none;" + (count > 0 ? "" : "color: grey; opacity: 0.2"));
            int height = maxCount[0] > 0 ? (int) Math.round(64.0 * count / maxCount[0]) + 1 : 1;
            landscapeReport.addContentInDiv("", "background-color: lightgrey; width: 100%; height: " + height + "px");
            String info = "";
            if (count > 0) {
                info += animalCounts.get(animal).stream()
                        .map(a -> a.getAnalysisResults().getMetadata().getName() + " " + a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                        .collect(Collectors.joining("\n"));
            }

            String perc = "";

            if (repositories.size() > 0) {
                long percValue = Math.round(100.0 * count / repositories.size());
                if (percValue == 0 && count > 0) {
                    perc = "<1%<br>";
                } else {
                    perc = percValue + "%<br>";
                }
            }

            landscapeReport.addContentInDivWithTooltip(perc + count + (count == 0 ? " repo" : " repos"), info, "font-size: 80%; width: 100%; text-align: center");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();

        landscapeReport.endTable();

        landscapeReport.endSection();
    }

    private void addNoHistoryRow() {
        int mainLoc = landscapeAnalysisResults.getMainLoc();
        int mainFilesCount = landscapeAnalysisResults.getMainFilesCount();
        int values = getSumOfValues(overallFileLastModifiedDistribution);
        int counts = getSumOfCounts(overallFileLastModifiedDistribution);
        int filesWithoutHistoryCount = mainFilesCount - counts;
        int locWithoutHistory = mainLoc - values;
        if (filesWithoutHistoryCount > 0 && locWithoutHistory > 0) {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell("no<br>history", "border: none");
            landscapeReport.startTableCell("border: none; padding-top: 3px;");
            landscapeReport.startDivWithLabel(FormattingUtils.formatCount(filesWithoutHistoryCount) + " files without commit history, " + FormattingUtils.formatCount(locWithoutHistory) + " lines of code (" + FormattingUtils.getFormattedPercentage(100.0 * locWithoutHistory / mainLoc) + "%)", "");
            landscapeReport.addHtmlContent(addFilesWithoutHistoryBar(locWithoutHistory, mainLoc));
            landscapeReport.endDiv();
            landscapeReport.endTableCell();
            landscapeReport.addTableCell("", "border: none");
            landscapeReport.endTableRow();
        }
    }

    private int getSumOfValues(SourceFileAgeDistribution distribution) {
        return distribution.getNegligibleRiskValue() + distribution.getLowRiskValue() + distribution.getMediumRiskValue() + distribution.getHighRiskValue() + distribution.getVeryHighRiskValue();
    }

    private int getSumOfCounts(SourceFileAgeDistribution distribution) {
        return distribution.getNegligibleRiskCount() + distribution.getLowRiskCount() + distribution.getMediumRiskCount() + distribution.getHighRiskCount() + distribution.getVeryHighRiskCount();
    }

    private String addFilesWithoutHistoryBar(int value, int total) {
        int width = (int) (BAR_WIDTH * (double) value / total);
        return "<svg width='" + (width + 2) + "' height='" + (BAR_HEIGHT) + "'>" +
                "<rect width='" + width + "' height='" + (BAR_HEIGHT - 2) + "' style='fill:rgb(200,200,200);stroke-width:1;stroke:rgb(150,150,150)'/>\n" +
                "</svg>";
    }

    private void addInfoBlock(String mainValue, String subtitle, String description, String tooltip, String color, String icon) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color + "; opacity: 0.8", tooltip, icon);
    }

    private void addFreshInfoBlock(String mainValue, String subtitle, String description, String tooltip, String color, String icon) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, icon);
    }

    private String getExtraPeopleInfo(List<ContributorRepositories> contributors, long contributorsCount) {
        String info = "";

        int recentContributorsCount6Months = landscapeAnalysisResults.getRecentContributorsCount6Months(contributors);
        int recentContributorsCount3Months = landscapeAnalysisResults.getRecentContributorsCount3Months(contributors);
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getRecentContributorsCount(landscapeAnalysisResults.getContributors())) + " contributors (30 days)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount3Months) + " contributors (3 months)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount6Months) + " contributors (6 months)\n";

        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdCommits = configuration.getContributorThresholdCommits();
        info += FormattingUtils.getPlainTextForNumber((int) contributorsCount) + " contributors (all time)\n";
        info += "\nOnly the contributors with " + (thresholdCommits > 1 ? "(" + thresholdCommits + "+&nbsp;commits)" : "") + " included";

        return info;
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        addPeopleInfoBlockWithColor(mainValue, subtitle, description, tooltip, PEOPLE_COLOR);
    }

    private void addPeopleInfoBlockWithColor(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: #707070; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, "contributors");
    }

    private void addWorkloadInfoBlockWithColor(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, "workload");
    }

    private void addRepositoriesInfoBlockWithColor(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, "repository");
    }

    private void addInfoBlockWithColor(String mainValue, String subtitle, String color, String tooltip, String icon) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";
        style += "box-shadow: rgb(0 0 0 / 12%) 0px 1px 3px, rgb(0 0 0 / 24%) 0px 1px 2px;";

        landscapeReport.startDiv("display: inline-block; text-align: center", tooltip);
        landscapeReport.addContentInDiv(ReportFileExporter.getIconSvg(icon, 48), "margin-top: 18px; margin-bottom: -12px");
        landscapeReport.startDiv(style, tooltip);
        String specialColor = mainValue.equals("<b>0</b>") ? " color: grey;" : "";
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px;" + specialColor + "'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 15px;" + specialColor + "'>" + subtitle + "</div>");
        landscapeReport.endDiv();
        landscapeReport.endDiv();
    }

    private String getRiskProfileVisual(RiskDistributionStats distributionStats, Palette palette) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(BAR_WIDTH + 20);
        chart.setBarHeight(BAR_HEIGHT);
        chart.setMaxBarWidth(BAR_WIDTH);
        chart.setBarStartXOffset(0);

        List<Integer> values = Arrays.asList(
                distributionStats.getVeryHighRiskValue(),
                distributionStats.getHighRiskValue(),
                distributionStats.getMediumRiskValue(),
                distributionStats.getLowRiskValue(),
                distributionStats.getNegligibleRiskValue());

        return chart.getStackedBarSvg(values, palette, "", "");
    }


    private void addLangInfoBlock(String value, String lang, String description) {
        String style = "border-radius: 8px; margin: 4px 4px 4px 0px; display: inline-block; " +
                "width: 80px; height: 114px;background-color: #dedede; " +
                "text-align: center; vertical-align: middle; margin-bottom: 16px;";

        landscapeReport.startDivWithLabel(description, style);

        landscapeReport.addContentInDiv("", "margin-top: 8px");
        landscapeReport.addHtmlContent(DataImageUtils.getLangDataImageDiv42(lang));
        landscapeReport.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 13px'>" + lang + "</div>");
        landscapeReport.endDiv();
    }

    private void addLangInfoBlockExtra(String value, String lang, String description, String extra) {
        String style = "border-radius: 8px; margin: 4px 4px 4px 0px; display: inline-block; " +
                "width: 80px; height: 114px;background-color: #dedede; " +
                "text-align: center; vertical-align: middle; margin-bottom: 16px;";

        landscapeReport.startDivWithLabel(description, style);

        landscapeReport.addContentInDiv("", "margin-top: 8px");
        landscapeReport.addHtmlContent(DataImageUtils.getLangDataImageDiv42(lang));
        landscapeReport.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 13px'>" + lang + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #767676; font-size: 9px; margin-top: 1px;'>" + extra + "</div>");
        landscapeReport.endDiv();
    }

    private void addSmallInfoBlock(String value, String subtitle, String color, String link) {
        String style = "border-radius: 8px;";

        style += "margin: 4px 4px 4px 0px;";
        style += "display: inline-block; width: 80px; height: 76px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 16px;";

        landscapeReport.startDiv(style);
        if (StringUtils.isNotBlank(link)) {
            landscapeReport.startNewTabLink(link, "text-decoration: none");
        }
        landscapeReport.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 13px'>" + subtitle + "</div>");
        if (StringUtils.isNotBlank(link)) {
            landscapeReport.endNewTabLink();
        }
        landscapeReport.endDiv();
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

        reports.add(this.landscapeReport);
        reports.add(this.landscapeRepositoriesReportShort);
        reports.add(this.landscapeRepositoriesTags);
        reports.add(this.landscapeRepositoriesExtensionTags);
        reports.add(this.landscapeRepositoriesTagsMatrix);
        reports.add(this.landscapeRepositoriesExtensionTagsMatrix);
        if (landscapeAnalysisResults.getRepositoryAnalysisResults().size() > landscapeAnalysisResults.getConfiguration().getRepositoriesShortListLimit()) {
            reports.add(this.landscapeRepositoriesReportLong);
        }
        reports.add(landscapeReportContributorsTab.getLandscapeContributorsReport());
        reports.add(landscapeReportContributorsTab.getLandscapeBotsReport());
        reports.add(landscapeReportContributorsTab.getLandscapeRecentContributorsReport());
        if (teamsConfig.getTeams().size() > 0) {
            reports.add(landscapeReportTeamsTab.getLandscapeContributorsReport());
            reports.add(landscapeReportTeamsTab.getLandscapeRecentContributorsReport());
        }

        return reports;
    }

    private void addContributorsPerYear(boolean showContributorsCount) {
        List<ContributionTimeSlot> contributorsPerYear = landscapeAnalysisResults.getContributorsPerYear();
        if (contributorsPerYear.size() > 0) {
            int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(0, limit);
            }

            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            landscapeReport.startDiv("overflow-y: none;");
            landscapeReport.startTable();

            landscapeReport.startTableRow();
            landscapeReport.startTableCell("border: none; height: 100px");
            int commitsCount = landscapeAnalysisResults.getCommitsCount();
            if (commitsCount > 0) {
                landscapeReport.startDiv("max-height: 105px");
                addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(commitsCount), "commits", "white", "");
                landscapeReport.endDiv();
            }
            landscapeReport.endTableCell();
            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px";
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);
            contributorsPerYear.forEach(year -> {
                landscapeReport.startTableCell(style);
                int count = year.getCommitsCount();
                String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color);
                int height = 1 + (int) (64.0 * count / maxCommits);
                String bgColor = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "lightgrey";
                landscapeReport.addHtmlContent("<div style='width: 100%; background-color: " + bgColor + "; height:" + height + "px'></div>");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();

            if (showContributorsCount) {
                int maxContributors[] = {1};
                contributorsPerYear.forEach(year -> {
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    maxContributors[0] = Math.max(maxContributors[0], count);
                });
                landscapeReport.startTableRow();
                landscapeReport.startTableCell("border: none; height: 100px");
                int contributorsCount = landscapeAnalysisResults.getContributors().size();
                if (contributorsCount > 0) {
                    landscapeReport.startDiv("max-height: 105px");
                    addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(contributorsCount), "contributors", "white", "");
                    landscapeReport.endDiv();
                }
                landscapeReport.endTableCell();
                contributorsPerYear.forEach(year -> {
                    landscapeReport.startTableCell(style);
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                    landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color + ";");
                    int height = 1 + (int) (64.0 * count / maxContributors[0]);
                    landscapeReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                    landscapeReport.endTableCell();
                });
                landscapeReport.endTableRow();
            }

            landscapeReport.startTableRow();
            landscapeReport.addTableCell("", "border: none; ");
            var ref = new Object() {
                String latestCommitDate = landscapeAnalysisResults.getLatestCommitDate();
            };
            if (ref.latestCommitDate.length() > 5) {
                ref.latestCommitDate = ref.latestCommitDate.substring(5);
            }
            contributorsPerYear.forEach(year -> {
                String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                landscapeReport.startTableCell("vertical-align: top; border: none; text-align: center; font-size: 90%; color: " + color);
                landscapeReport.addHtmlContent(year.getTimeSlot());
                if (landscapeAnalysisResults.getLatestCommitDate().startsWith(year.getTimeSlot() + "-")) {
                    landscapeReport.addContentInDiv(ref.latestCommitDate, "text-align: center; color: grey; font-size: 9px");
                }
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private int getContributorsCountPerYear(String year) {
        return this.contributorsPerYearMap.containsKey(year) ? contributorsPerYearMap.get(year).size() : 0;
    }

    private void populateTimeSlotMaps() {
        landscapeAnalysisResults.getContributors().forEach(contributorRepositories -> {
            List<String> commitDates = contributorRepositories.getContributor().getCommitDates();
            commitDates.forEach(day -> {
                String week = DateUtils.getWeekMonday(day);
                String month = DateUtils.getMonth(day);
                String year = DateUtils.getYear(day);

                updateTimeSlotMap(contributorRepositories, contributorsPerDayMap, rookiesPerDayMap, day, day);
                updateTimeSlotMap(contributorRepositories, contributorsPerWeekMap, rookiesPerWeekMap, week, week);
                updateTimeSlotMap(contributorRepositories, contributorsPerMonthMap, rookiesPerMonthMap, month, month + "-01");
                updateTimeSlotMap(contributorRepositories, contributorsPerYearMap, rookiesPerYearMap, year, year + "-01-01");
            });
        });

    }

    private void updateTimeSlotMap(ContributorRepositories contributorRepositories,
                                   Map<String, List<String>> map, Map<String, List<String>> rookiesMap, String key, String rookieDate) {
        boolean rookie = contributorRepositories.getContributor().isRookieAtDate(rookieDate);

        String email = contributorRepositories.getContributor().getEmail();
        if (map.containsKey(key)) {
            if (!map.get(key).contains(email)) {
                map.get(key).add(email);
            }
        } else {
            map.put(key, new ArrayList<>(Arrays.asList(email)));
        }
        if (rookie) {
            if (rookiesMap.containsKey(key)) {
                if (!rookiesMap.get(key).contains(email)) {
                    rookiesMap.get(key).add(email);
                }
            } else {
                rookiesMap.put(key, new ArrayList<>(Arrays.asList(email)));
            }
        }
    }

    private void addDownloadLinks(String graphId) {
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent("Download: ");
        landscapeReport.addNewTabLink("SVG", "visuals/" + graphId + ".svg");
        landscapeReport.addHtmlContent(" ");
        landscapeReport.addNewTabLink("DOT", "visuals/" + graphId + ".dot.txt");
        landscapeReport.addHtmlContent(" ");
        landscapeReport.addNewTabLink("(open online Graphviz editor)", "https://obren.io/tools/graphviz/");
        landscapeReport.endDiv();
    }

    abstract class ZommableCircleCountExtractors {
        public abstract int getCount(RepositoryAnalysisResults repositoryAnalysisResults);
    }

    public List<RichTextReport> getIndividualContributorReports() {
        return landscapeReportContributorsTab.getIndividualReports();
    }

    public List<RichTextReport> getIndividualTeamReports() {
        return landscapeReportTeamsTab.getIndividualReports();
    }

    public List<RichTextReport> getIndividualBotReports() {
        return landscapeReportContributorsTab.getBotReports();
    }
}