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
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.landscape.data.LandscapeDataExport;
import nl.obren.sokrates.reports.landscape.statichtml.repositories.*;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.*;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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

    public static final String TAGS_TAB_ID = "tags";
    public static final String CONTRIBUTORS_TAB_ID = "contributors";
    public static final String TOPOLOGIES_TAB_ID = "topologies";
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
    public static final String OPEN_IN_NEW_TAB_SVG_ICON = "<svg width=\"14pt\" height=\"14pt\" version=\"1.1\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
            " <path d=\"m87.5 16.918-35.289 35.289c-1.2266 1.1836-3.1719 1.168-4.3789-0.039062s-1.2227-3.1523-0.039062-4.3789l35.289-35.289h-23.707c-1.7266 0-3.125-1.3984-3.125-3.125s1.3984-3.125 3.125-3.125h31.25c0.82812 0 1.625 0.32812 2.2109 0.91406 0.58594 0.58594 0.91406 1.3828 0.91406 2.2109v31.25c0 1.7266-1.3984 3.125-3.125 3.125s-3.125-1.3984-3.125-3.125zm-56.25 1.832h-15.633c-5.1719 0-9.3672 4.1797-9.3672 9.3516v56.305c0 5.1562 4.2422 9.3516 9.3867 9.3516h56.219c2.4922 0 4.8828-0.98437 6.6406-2.7461 1.7617-1.7617 2.75-4.1523 2.7461-6.6445v-15.613 0.003906c0-1.7266-1.3984-3.125-3.125-3.125-1.7227 0-3.125 1.3984-3.125 3.125v15.613-0.003906c0.003906 0.83594-0.32422 1.6328-0.91406 2.2227s-1.3906 0.91797-2.2227 0.91797h-56.219c-1.7148-0.007812-3.1094-1.3867-3.1367-3.1016v-56.305c0-1.7148 1.3945-3.1016 3.1172-3.1016h15.633c1.7266 0 3.125-1.3984 3.125-3.125s-1.3984-3.125-3.125-3.125z\"/>\n" +
            "</svg>";
    ;
    private static final int BAR_WIDTH = 800;
    private static final int BAR_HEIGHT = 42;
    public static final String REPOSITORIES_COLOR = "#F1F0C0";
    public static final String MAIN_LOC_FRESH_COLOR = "#B7E5DD";
    public static final String MAIN_LOC_COLOR = "#A0BCC2";
    public static final String TEST_LOC_COLOR = "#c0c0c0";
    public static final String PEOPLE_COLOR = "lavender";
    private final TagMap customTagsMap;
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
    private RichTextReport landscapeRecentContributorsReport = new RichTextReport("", "contributors-recent.html");
    private RichTextReport landscapeContributorsReport = new RichTextReport("", "contributors.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int dependencyVisualCounter = 1;
    private List<TagGroup> tagGroups;
    private File folder;
    private File reportsFolder;
    private List<RichTextReport> individualContributorReports = new ArrayList<>();
    private Map<String, List<String>> contributorsPerWeekMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerWeekMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerMonthMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerMonthMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerYearMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerYearMap = new HashMap<>();
    private SourceFileAgeDistribution overallFileLastModifiedDistribution;
    private SourceFileAgeDistribution overallFileFirstModifiedDistribution;

    public LandscapeReportGenerator(LandscapeAnalysisResults landscapeAnalysisResults, List<TagGroup> tagGroups, File folder, File reportsFolder) {
        this.tagGroups = tagGroups;
        this.folder = folder;
        this.reportsFolder = reportsFolder;

        this.landscapeAnalysisResults = landscapeAnalysisResults;

        overallFileFirstModifiedDistribution = landscapeAnalysisResults.getOverallFileFirstModifiedDistribution();
        overallFileLastModifiedDistribution = landscapeAnalysisResults.getOverallFileLastModifiedDistribution();
        populateTimeSlotMaps();

        landscapeRepositoriesReportShort.setEmbedded(true);
        landscapeRepositoriesReportLong.setEmbedded(true);
        landscapeContributorsReport.setEmbedded(true);
        landscapeRecentContributorsReport.setEmbedded(true);
        LandscapeDataExport dataExport = new LandscapeDataExport(landscapeAnalysisResults, folder);

        LOG.info("Exporting repositories...");
        List<RepositoryAnalysisResults> repositories = getRepositories();

        customTagsMap = updateTagsData(landscapeAnalysisResults, tagGroups, repositories);

        dataExport.exportRepositories(customTagsMap);
        LOG.info("Exporting contributors...");
        dataExport.exportContributors();
        LOG.info("Exporting analysis results...");
        dataExport.exportAnalysisResults();

        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        Metadata metadata = configuration.getMetadata();
        String landscapeName = metadata.getName();
        if (StringUtils.isNotBlank(landscapeName)) {
            landscapeReport.setDisplayName(landscapeName);
        }
        landscapeReport.setParentUrl(configuration.getParentUrl());
        landscapeReport.setLogoLink(metadata.getLogoLink());
        landscapeReport.setBreadcrumbs(configuration.getBreadcrumbs());
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

        landscapeReport.addLineBreak();

        landscapeReport.startTabGroup();
        landscapeReport.addTab(OVERVIEW_TAB_ID, "Overview", true);
        List<SubLandscapeLink> subLandscapes = configuration.getSubLandscapes();
        List<SubLandscapeLink> level1SubLandscapes = configuration.getSubLandscapes().stream().filter(l -> getPathDepth(l.getIndexFilePath()) == 1).collect(Collectors.toList());
        if (subLandscapes.size() > 0) {
            landscapeReport.addTab(SUB_LANDSCAPES_TAB_ID, "Sub-Landscapes (" + (level1SubLandscapes.size() == 0 ? subLandscapes.size() : level1SubLandscapes.size()) + ")", false);
        }
        landscapeReport.addTab(REPOSITORIES_TAB_ID, "Repositories (" + landscapeAnalysisResults.getFilteredRepositoryAnalysisResults().size() + ")", false);

        landscapeReport.addTab(TAGS_TAB_ID, "Tags (" + customTagsMap.tagsCount() + ")", false);
        int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount();
        landscapeReport.addTab(CONTRIBUTORS_TAB_ID, "Contributors" + (recentContributorsCount > 0 ? " (" + recentContributorsCount + ")" + "" : ""), false);
        landscapeReport.addTab(TOPOLOGIES_TAB_ID, "Team Topology", false);
        configuration.getCustomTabs().forEach(tab -> {
            int index = configuration.getCustomTabs().indexOf(tab);
            landscapeReport.addTab(CUSTOM_TAB_ID_PREFIX + index, tab.getName(), false);
        });
        landscapeReport.endTabGroup();

        landscapeReport.startTabContentSection(OVERVIEW_TAB_ID, true);
        ProcessingStopwatch.start("reporting/overview");
        addBigSummary(landscapeAnalysisResults);
        if (configuration.isShowExtensionsOnFirstTab()) {
            addExtensions();
        }
        addIFrames(configuration.getiFramesAtStart());
        addIFrames(configuration.getiFrames());
        ProcessingStopwatch.end("reporting/overview");
        landscapeReport.endTabContentSection();

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
            landscapeReport.endTabContentSection();
        }

        landscapeReport.startTabContentSection(REPOSITORIES_TAB_ID, false);
        ProcessingStopwatch.start("reporting/big summary");
        LOG.info("Adding big summary...");
        addBigRepositoriesSummary(landscapeAnalysisResults);
        addIFrames(configuration.getiFramesRepositoriesAtStart());
        if (!configuration.isShowExtensionsOnFirstTab()) {
            LOG.info("Adding extensions...");
            addExtensions();
        }
        ProcessingStopwatch.end("reporting/big summary");

        LOG.info("Adding repository section...");
        ProcessingStopwatch.start("reporting/repositories");
        addRepositoriesSection(configuration, repositories);
        addIFrames(configuration.getiFramesRepositories());
        ProcessingStopwatch.end("reporting/repositories");
        landscapeReport.endTabContentSection();

        landscapeReport.startTabContentSection(TAGS_TAB_ID, false);
        ProcessingStopwatch.start("reporting/tags");
        addTagsSection(repositories);
        ProcessingStopwatch.end("reporting/tags");
        landscapeReport.endTabContentSection();


        landscapeReport.startTabContentSection(CONTRIBUTORS_TAB_ID, false);
        ProcessingStopwatch.start("reporting/summary");
        LOG.info("Adding big contributors summary...");
        addBigContributorsSummary();
        addIFrames(configuration.getiFramesContributorsAtStart());
        LOG.info("Adding contributors...");
        addContributors();
        if (recentContributorsCount > 0) {
            addContributorsPerExtension(true);
        }
        addContributorsPerExtension();
        LOG.info("Adding trends...");
        landscapeReport.addLevel2Header("Contribution Trends");
        addContributionTrends();
        addIFrames(configuration.getiFramesContributors());
        ProcessingStopwatch.end("reporting/summary");
        landscapeReport.endTabContentSection();

        landscapeReport.startTabContentSection(TOPOLOGIES_TAB_ID, false);
        ProcessingStopwatch.start("reporting/team topologies");
        LOG.info("Adding Contributor Dependencies...");
        addTeamTopology(landscapeAnalysisResults);
        ProcessingStopwatch.end("reporting/team topologies");
        landscapeReport.endTabContentSection();

        configuration.getCustomTabs().forEach(tab -> {
            int index = configuration.getCustomTabs().indexOf(tab);
            landscapeReport.startTabContentSection(CUSTOM_TAB_ID_PREFIX + index, false);
            landscapeReport.addLineBreak();
            addIFrames(tab.getiFrames());
            landscapeReport.endTabContentSection();
        });

        String generationDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        landscapeReport.addContentInDiv("generated by <a target='_blank' href='https://sokrates.dev/'>sokrates.dev</a> " +
                        " (<a href='config.json' target='_blank'>configuration</a> | <a href='config-tags.json' target='_blank'>tag definitions</a>) " +
                        " on " + generationDate,
                "color: grey; font-size: 80%; margin: 10px");
        LOG.info("Done report generation.");
    }

    private void getHiddenFilesTagGroup(List<RepositoryAnalysisResults> repositories, List<TagGroup> extensionTagGroups) {
        Set<String> hiddenFiles = new HashSet<>();
        Set<String> hiddenFolders = new HashSet<>();
        repositories.forEach(repository -> {
            repository.getFiles().forEach(path -> {
                File file = new File(path);
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

    public static List<ContributionTimeSlot> getContributionMonths(List<ContributionTimeSlot> contributorsPerMonthOriginal, int pastMonths, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>(contributorsPerMonthOriginal);
        List<String> slots = contributorsPerMonth.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastMonths(pastMonths, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerMonth.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerMonth;
    }

    private void addTeamTopology(LandscapeAnalysisResults landscapeAnalysisResults) {
        int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount();
        double c2cMax = recentContributorsCount * (recentContributorsCount - 1) / 2.0; // n * (n - 1) / 2

        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "contributors", "30 days", "");

        int c2c = (int) Math.round(landscapeAnalysisResults.getC2cConnectionsCount30Days());
        int cMedian = (int) Math.round(landscapeAnalysisResults.getcMedian30Days());
        int cMean = (int) Math.round(landscapeAnalysisResults.getcMean30Days());
        int cIndex = (int) Math.round(landscapeAnalysisResults.getcIndex30Days());
        String formattedPercentage = c2cMax > 0 ? FormattingUtils.getFormattedPercentage(100.0 * c2c / c2cMax) : "0";
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(c2c), "C2C connections", "30 days (" + formattedPercentage + "%)", "unique contributor to contributor connections (via shared repositories)");
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(cMedian), "C-Median", "30 days", "half of contributors have >= than this number of connections to other contributors");
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(cMean), "C-Mean", "30 days", "average number of contributor connections");
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(cIndex), "C-Index", "30 days", "N contributors have at least N contributor connections");

        int pMedian = (int) Math.round(landscapeAnalysisResults.getpMedian30Days());
        int pMean = (int) Math.round(landscapeAnalysisResults.getpMean30Days());
        int pIndex = (int) Math.round(landscapeAnalysisResults.getpIndex30Days());
        this.landscapeReport.addLineBreak();
        int repositoriesCount = landscapeAnalysisResults.getRepositoriesCount();
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(repositoriesCount), repositoriesCount == 1 ? "repository" : "repositories", "", "", REPOSITORIES_COLOR);
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(pMedian), "R-Median", "30 days", "half of contributors have >= than this number of connections to repositories", REPOSITORIES_COLOR);
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(pMean), "R-Mean", "30 days", "average number of contributor repository connections", REPOSITORIES_COLOR);
        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(pIndex), "R-Index", "30 days", "N contributors have at least N repository connections", REPOSITORIES_COLOR);

        addPeopleDependencies();
    }

    private void addPeopleDependencies() {
        boolean recentlyActive = landscapeAnalysisResults.getRecentContributorsCount() > 0;
        landscapeReport.addLevel2Header("Highlights");

        landscapeReport.startSubSection("Contributor Topology (past 30 days)", "");

        if (recentlyActive) {
            landscapeReport.addParagraph("The diagram shows contributor collaborations defined as working on " +
                    "the same repositories in the past 30 days. The lines display the number of shared repositories " +
                    "between two contributors.\n", "color: grey");
            landscapeReport.addNewTabLink("<div style='font-weight: bold; font-size: 110%; margin-bottom: 8px;'>3D graph (including repositories)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON + "</div>", "visuals/people_dependencies_including_repositories_30_2_force_3d.html");
            landscapeReport.startDiv("font-size: 90%");
            landscapeReport.addNewTabLink("2D graph", "visuals/people_dependencies_30_1.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("2D graph (including contributors)", "visuals/people_dependencies_including_repositories_30_2.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D graph", "visuals/people_dependencies_30_1_force_3d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("data", "data/repository_shared_repositories_30_days.txt");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON);
            landscapeReport.endDiv();
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("<iframe src=\"visuals/people_dependencies_30_1.svg\" " +
                    "style=\"border: 1px solid lightgrey; width: 100%; height: 600px\"></iframe>");
        } else {
            landscapeReport.addParagraph("No commits in past 30 days.", "color: grey");
        }
        landscapeReport.endSection();
        landscapeReport.startSubSection("Repository Topology (past 30 days)", "");
        if (recentlyActive) {
            landscapeReport.addParagraph("The diagram shows repository dependencies defined as having the same " +
                    "contributors working on the same repositories in the past 30 days. " +
                    "The lines between repositories display the number of contributors working on both repositories.", "color: grey");
            landscapeReport.addNewTabLink("2D graph", "visuals/repository_dependencies_30_3.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("2D graph (including contributors)", "visuals/people_dependencies_including_repositories_30_2.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D graph", "visuals/repository_dependencies_30_3_force_3d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D graph (including contributors)", "visuals/people_dependencies_including_repositories_30_2_force_3d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("data", "data/repository_shared_repositories_30_days.txt");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON);
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("<iframe src=\"visuals/repository_dependencies_30_3.svg\" " +
                    "style=\"border: 1px solid lightgrey; width: 100%; height: 600px\"></iframe>");
        } else {
            landscapeReport.addParagraph("No commits in past 30 days.", "color: grey");
        }
        landscapeReport.endSection();
        landscapeReport.startSubSection("Knowledge Topology (past 30 days)", "");
        if (recentlyActive) {
            landscapeReport.addParagraph("The diagram shows dependencies between programming languages (file extensions) defined as having the same contributors committing to files with these etensions in the past 30 days. " +
                    "The lines between repositories display the number of contributors committing to files with both extensions in ht past 30 days.", "color: grey");
            landscapeReport.addNewTabLink("2D graph", "visuals/extension_dependencies_30d.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D graph", "visuals/extension_dependencies_30d_force_3d.html");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON);
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("<iframe src=\"visuals/extension_dependencies_30d.svg\" " +
                    "style=\"border: 1px solid lightgrey; width: 100%; height: 600px\"></iframe>");
        } else {
            landscapeReport.addParagraph("No commits in past 30 days.", "color: grey");
        }
        landscapeReport.endSection();
        landscapeReport.addLineBreak();
        landscapeReport.addLevel2Header("Details");
        landscapeReport.startSubSection("Contributor Dependencies Details", "");

        List<ComponentDependency> peopleDependencies30Days = landscapeAnalysisResults.getPeopleDependencies30Days();
        List<ComponentDependency> peoplerepositoryDependencies30Days = landscapeAnalysisResults.getPeopleRepositoryDependencies30Days();
        List<ContributorConnections> connectionsViaRepositories30Days = landscapeAnalysisResults.getConnectionsViaRepositories30Days();
        this.renderPeopleDependencies(peopleDependencies30Days, peoplerepositoryDependencies30Days, connectionsViaRepositories30Days,
                landscapeAnalysisResults.getcIndex30Days(), landscapeAnalysisResults.getpIndex30Days(),
                landscapeAnalysisResults.getcMean30Days(), landscapeAnalysisResults.getpMean30Days(),
                landscapeAnalysisResults.getcMedian30Days(), landscapeAnalysisResults.getpMedian30Days(),
                30);

        List<ComponentDependency> peopleDependencies90Days = landscapeAnalysisResults.getPeopleDependencies90Days();
        List<ContributorConnections> connectionsViaRepositories90Days = landscapeAnalysisResults.getConnectionsViaRepositories90Days();
        this.renderPeopleDependencies(peopleDependencies90Days, null, connectionsViaRepositories90Days,
                landscapeAnalysisResults.getcIndex90Days(), landscapeAnalysisResults.getpIndex90Days(),
                landscapeAnalysisResults.getcMean90Days(), landscapeAnalysisResults.getpMean90Days(),
                landscapeAnalysisResults.getcMedian90Days(), landscapeAnalysisResults.getpMedian90Days(),
                90);

        List<ComponentDependency> peopleDependencies180Days = landscapeAnalysisResults.getPeopleDependencies180Days();
        List<ContributorConnections> connectionsViaRepositories180Days = landscapeAnalysisResults.getConnectionsViaRepositories180Days();
        this.renderPeopleDependencies(peopleDependencies180Days, null, connectionsViaRepositories180Days,
                landscapeAnalysisResults.getcIndex180Days(), landscapeAnalysisResults.getpIndex180Days(),
                landscapeAnalysisResults.getcMean180Days(), landscapeAnalysisResults.getpMean180Days(),
                landscapeAnalysisResults.getcMedian180Days(), landscapeAnalysisResults.getpMedian180Days(),
                180);

        landscapeReport.endSection();
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
                                "onerror=\"this.onerror=null;this.src='" + ReportConstants.SOKRATES_SVG_ICON_SMALL_BASE64 + "'\">"
                                : ReportConstants.SOKRATES_SVG_ICON_SMALL) +
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
                "", "all repositories updated after " + configuration.getIgnoreRepositoriesLastUpdatedBefore() + " with at least " + FormattingUtils.formatCountPlural(configuration.getRepositoryThresholdContributors(), "contributor", "contributors"), REPOSITORIES_COLOR);
        addLocInfoBlock(landscapeAnalysisResults);
        int mainLoc1YearActive = landscapeAnalysisResults.getMainLoc1YearActive();
        int totalValue = getSumOfValues(overallFileLastModifiedDistribution);
        addActiveCodeBlock(landscapeAnalysisResults, totalValue);

        List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
        long contributorsCount = contributors.size();
        if (contributorsCount > 0) {
            int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount();
            int locPerRecentContributor = 0;
            if (recentContributorsCount > 0) {
                locPerRecentContributor = (int) Math.round((double) mainLoc1YearActive / recentContributorsCount);
            }
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "recent contributors",
                    "(past 30 days)", getExtraPeopleInfo(contributors, contributorsCount) + "\n" + FormattingUtils.formatCount(locPerRecentContributor) + " active lines of code per recent contributor");
            int rookiesContributorsCount = landscapeAnalysisResults.getRookiesContributorsCount();
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
        landscapeAnalysisResults.getRecentContributorsCount();
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
        String contributorConstraint = " with at least " + FormattingUtils.formatCountPlural(configuration.getRepositoryThresholdContributors(), "contributor", "contributors");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size), "all time",
                FormattingUtils.getSmallTextForNumber(locAll) + " LOC",
                "all repositories updated after " + configuration.getIgnoreRepositoriesLastUpdatedBefore() + " with at least " + contributorConstraint, REPOSITORIES_COLOR);
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size365Days), "past 365d",
                FormattingUtils.getSmallTextForNumber(loc365Days) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * loc365Days / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 365 days with at least " + contributorConstraint, REPOSITORIES_COLOR);
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size180Days), "past 180d",
                FormattingUtils.getSmallTextForNumber(loc180Days) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * loc180Days / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 180 days with at least " + contributorConstraint, REPOSITORIES_COLOR);
        addInfoBlock(FormattingUtils.getSmallTextForNumber(size90Days), "past 90d",
                FormattingUtils.getSmallTextForNumber(loc180Days) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * loc90Days / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 90 days with at least " + contributorConstraint, REPOSITORIES_COLOR);
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(recentSize), "past 30d",
                FormattingUtils.getSmallTextForNumber(recentLoc) + " LOC (" + FormattingUtils.getFormattedPercentage(100.0 * recentLoc / Math.max(1, locAll)) + "%)",
                "all repositories updated in the past 30 days with at least " + contributorConstraint, REPOSITORIES_COLOR);
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

    private void addActiveCodeBlock(LandscapeAnalysisResults landscapeAnalysisResults, int locAll) {
        int mainLocActive = landscapeAnalysisResults.getMainLoc1YearActive();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocActive), "main code touched", "1 year (" + FormattingUtils.getFormattedPercentage(100.0 * mainLocActive / Math.max(1, locAll)) + "%)",
                "files updated in past year", MAIN_LOC_FRESH_COLOR);
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocNew), "new main code", "1 year (+" + FormattingUtils.getFormattedPercentage(100.0 * mainLocNew / Math.max(1, locAll)) + "%)", "files created in past year", MAIN_LOC_FRESH_COLOR);
    }

    private void addLocInfoBlock(LandscapeAnalysisResults landscapeAnalysisResults) {
        int mainLoc = landscapeAnalysisResults.getMainLoc();
        int secondaryLoc = landscapeAnalysisResults.getSecondaryLoc();
        int mainFilesCount = landscapeAnalysisResults.getMainFilesCount();
        int secondaryFilesCount = landscapeAnalysisResults.getSecondaryFilesCount();
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(mainLoc), "lines of main code", FormattingUtils.getSmallTextForNumber(mainFilesCount) + " files", "main lines of code", MAIN_LOC_COLOR);
        addFreshInfoBlock(FormattingUtils.getSmallTextForNumber(secondaryLoc), "lines of other code", FormattingUtils.getSmallTextForNumber(secondaryFilesCount) + " files", "test, build & deployment, generated, all other code in scope", TEST_LOC_COLOR);
    }

    private void addBigContributorsSummary() {
        List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
        long contributorsCount = contributors.size();
        int mainLocActive = landscapeAnalysisResults.getMainLoc1YearActive();
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        if (contributorsCount > 0) {
            int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount();
            int locPerRecentContributor = 0;
            int locNewPerRecentContributor = 0;
            if (recentContributorsCount > 0) {
                locPerRecentContributor = (int) Math.round((double) mainLocActive / recentContributorsCount);
                locNewPerRecentContributor = (int) Math.round((double) mainLocNew / recentContributorsCount);
            }
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "recent contributors",
                    "(past 30 days)", getExtraPeopleInfo(contributors, contributorsCount) + "\n" + FormattingUtils.formatCount(locPerRecentContributor) + " active lines of code per recent contributor");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getRecentContributorsCount3Months()), "3m contributors",
                    "(past 90 days)", getExtraPeopleInfo(contributors, contributorsCount));
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getRecentContributorsCount6Months()), "6m contributors",
                    "(past 180 days)", getExtraPeopleInfo(contributors, contributorsCount));
            int rookiesContributorsCount = landscapeAnalysisResults.getRookiesContributorsCount();
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(rookiesContributorsCount),
                    rookiesContributorsCount == 1 ? "active rookie" : "active rookies",
                    "(started in past year)", "active contributors with the first commit in past year");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(locPerRecentContributor), "contributor load",
                    "(active LOC/contributor)", "active lines of code per recent contributor\n\n" + FormattingUtils.getPlainTextForNumber(locNewPerRecentContributor) + " new LOC/recent contributor");
            List<ComponentDependency> peopleDependencies = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 30);
            peopleDependencies.sort((a, b) -> b.getCount() - a.getCount());
        }
    }

    private void addContributionTrends() {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int commitsMaxYears = configuration.getCommitsMaxYears();
        int significantContributorMinCommitDaysPerYear = configuration.getSignificantContributorMinCommitDaysPerYear();

        landscapeReport.startSubSection("Contributors Per Year", "Past " + commitsMaxYears + " years");
        addContributorsPerYear(true);
        landscapeReport.endSection();
        LOG.info("Adding contributors per extension...");

        landscapeReport.startSubSection("Significant Contributors Per Year (" + significantContributorMinCommitDaysPerYear + "+ commit days per year)", "Past " + commitsMaxYears + " years");
        addContributorsPerYear();
        landscapeReport.endSection();

        landscapeReport.startSubSection("Contributors Per Month", "Past two years");
        addContributorsPerMonth();
        landscapeReport.endSection();

        landscapeReport.startSubSection("Contributors Per Week", "Past two years");
        addContributorsPerWeek();
        landscapeReport.endSection();

        landscapeReport.addParagraph("latest commit date: <b>" + landscapeAnalysisResults.getLatestCommitDate() + "</b>", "color: grey");
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
        landscapeReport.startShowMoreBlockDisappear("", "&nbsp;&nbsp;Show test and other code...");
        addMainExtensions("Test", LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getTestLinesOfCodePerExtension()), false);
        addMainExtensions("Other", LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getOtherLinesOfCodePerExtension()), false);
        landscapeReport.endShowMoreBlock();
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
        landscapeReport.startShowMoreBlockDisappear("", "&nbsp;&nbsp;Show commit history per extension...");

        landscapeReport.startSubSection("Commit history per file extension", "");
        landscapeReport.startDiv("max-height: 600px; overflow-y: auto;");
        landscapeReport.startDiv("margin-bottom: 16px; vertical-align: middle;");
        landscapeReport.addContentInDiv(ReportConstants.ANIMATION_SVG_ICON, "display: inline-block; vertical-align: middle; margin: 4px;");
        landscapeReport.addHtmlContent("animated commit history: ");
        landscapeReport.addNewTabLink("all time cumulative", "visuals/racing_charts_extensions_commits.html?tickDuration=1200");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("12 months window", "visuals/racing_charts_extensions_commits_window.html?tickDuration=1200");
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
        landscapeReport.startSubSection("Tags (" + customTagsMap.tagsCount() + ")", "");
        new LandscapeRepositoriesTagsLine(tagGroups, customTagsMap).addTagsLine(landscapeReport);
        landscapeReport.endSection();

        landscapeReport.addLineBreak();
    }

    private void addMainExtensions(String type, List<NumericMetric> linesOfCodePerExtension, boolean linkCharts) {
        int threshold = landscapeAnalysisResults.getConfiguration().getExtensionThresholdLoc();
        landscapeReport.startSubSection("File Extensions in " + type + " Code (" + linesOfCodePerExtension.size() + ")",
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
        landscapeReport.addNewTabLink(" - show extension dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_3d.html");
        new Force3DGraphExporter().export3DForceGraph(dependencies, reportsFolder, "extension_dependencies_30d");
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

    private void addContributors() {
        ProcessingStopwatch.start("reporting/contributors");
        int contributorsCount = landscapeAnalysisResults.getContributorsCount();

        if (contributorsCount > 0) {
            ProcessingStopwatch.start("reporting/contributors/preparing");

            List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
            List<ContributorRepositories> recentContributors = landscapeAnalysisResults.getContributors().stream()
                    .filter(c -> c.getContributor().isActive(RECENT_THRESHOLD_DAYS))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(recentContributors, (a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days());
            int totalCommits = contributors.stream().mapToInt(c -> c.getContributor().getCommitsCount()).sum();
            int totalRecentCommits = recentContributors.stream().mapToInt(c -> c.getContributor().getCommitsCount30Days()).sum();
            final String[] latestCommit = {""};
            contributors.forEach(c -> {
                if (c.getContributor().getLatestCommitDate().compareTo(latestCommit[0]) > 0) {
                    latestCommit[0] = c.getContributor().getLatestCommitDate();
                }
            });

            int recentContributorsCount = recentContributors.size();

            if (recentContributorsCount > 0) {
                landscapeReport.startSubSection("<a href='contributors-recent.html' target='_blank' style='text-decoration: none'>" +
                                "Recent Contributors (" + recentContributorsCount + ")</a>&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON,
                        "latest commit " + latestCommit[0]);

                addRecentContributorLinks();

                DescriptiveStatistics stats = new DescriptiveStatistics();
                recentContributors.forEach(c -> stats.addValue(c.getContributor().getCommitsCount30Days()));
                double max = Math.max(stats.getMax(), 1);
                double sum = Math.max(stats.getSum(), 1);

                int cumulativeCount[] = {0};
                double prevCumulativePercentage[] = {0};
                int index[] = {0};

                StringBuilder html = new StringBuilder();
                ProcessingStopwatch.end("reporting/contributors/preparing");

                ProcessingStopwatch.start("reporting/contributors/table");
                recentContributors.stream().limit(landscapeAnalysisResults.getConfiguration().getContributorsListLimit()).forEach(c -> {
                    index[0] += 1;
                    Contributor contributor = c.getContributor();
                    int count = contributor.getCommitsCount30Days();
                    int height = (int) (Math.round(64 * count / max)) + 1;
                    cumulativeCount[0] += count;
                    double cumulativePercentage = Math.round(1000.0 * cumulativeCount[0] / sum) / 10;
                    double contributorPercentage = Math.round(10000.0 * index[0] / recentContributorsCount) / 100;
                    String tooltip = contributor.getEmail()
                            + "\n - commits (30d): " + count
                            + "\n - cumulative commits (top " + index[0] + "): " + cumulativeCount[0]
                            + "\n - cumulative percentage (top " + contributorPercentage + "% " + "): " + cumulativePercentage + "%";
                    String color = (prevCumulativePercentage[0] < 50 && cumulativePercentage >= 50) ? "blue" : "skyblue";
                    String style = "margin-right: 1px; vertical-align: bottom; width: 8px; background-color: " + color + "; display: inline-block; height: " + height + "px";

                    if (contributor.isRookie()) {
                        style += "; border-bottom: 4px solid green;";
                    } else {
                        style += "; border-bottom: 4px solid " + color + ";";
                    }

                    html.append("<div title='" + tooltip + "' style='" + style + "'></div>");
                    prevCumulativePercentage[0] = cumulativePercentage;
                });
                landscapeReport.startDiv("white-space: nowrap; width: 100%; overflow-x: scroll;");
                landscapeReport.addHtmlContent(html.toString());
                landscapeReport.endDiv();
                landscapeReport.startDiv("color: grey; font-size: 70%");
                landscapeReport.addHtmlContent("commits per contributor | ");
                for (int p = 90; p >= 10; p -= 10) {
                    double percentile = stats.getPercentile(p);
                    landscapeReport.addHtmlContent("p(" + p + ") = " + (int) Math.round(percentile) + "; ");
                }
                landscapeReport.endDiv();

                landscapeReport.addHtmlContent("<iframe src='contributors-recent.html' frameborder=0 style='height: 450px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

                landscapeReport.endSection();
            }

            landscapeReport.startDiv("margin-bottom: 16px; margin-top: -6px; vertical-align: middle;");
            landscapeReport.addContentInDiv(ReportConstants.ANIMATION_SVG_ICON, "display: inline-block; vertical-align: middle; margin: 4px;");
            landscapeReport.addNewTabLink("animated contributors history (all time)", "visuals/racing_charts_commits_contributors.html?tickDuration=1200");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("animated contributors history (12 months window)", "visuals/racing_charts_commits_window_contributors.html?tickDuration=1200");
            landscapeReport.endDiv();

            landscapeReport.startSubSection("<a href='contributors.html' target='_blank' style='text-decoration: none'>" +
                            "All Contributors (" + contributorsCount + ")</a>&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON,
                    "latest commit " + latestCommit[0]);

            landscapeReport.startShowMoreBlock("show details...");
            addContributorLinks();

            landscapeReport.addHtmlContent("<iframe src='contributors.html' frameborder=0 style='height: 450px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

            landscapeReport.endShowMoreBlock();
            landscapeReport.endSection();
            ProcessingStopwatch.end("reporting/contributors/table");

            ProcessingStopwatch.start("reporting/contributors/saving tables");
            Set<String> contributorsLinkedFromTables = new HashSet<>();
            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeRecentContributorsReport, contributorsLinkedFromTables)
                    .saveContributorsTable(recentContributors, totalRecentCommits, true);
            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeContributorsReport, contributorsLinkedFromTables)
                    .saveContributorsTable(contributors, totalCommits, false);
            ProcessingStopwatch.end("reporting/contributors/saving tables");

            ProcessingStopwatch.start("reporting/contributors/individual reports");
            List<ContributorRepositories> linkedContributors = contributors.stream()
                    .filter(c -> contributorsLinkedFromTables.contains(c.getContributor().getEmail()))
                    .collect(Collectors.toList());
            LOG.info("Saving individual reports for " + linkedContributors.size() + " contributor(s) linked from tables (out of " + contributors.size() + ")");
            individualContributorReports = new LandscapeIndividualContributorsReports(landscapeAnalysisResults).getIndividualReports(linkedContributors);
            ProcessingStopwatch.end("reporting/contributors/individual reports");
        }
        ProcessingStopwatch.end("reporting/contributors");
    }

    private void addContributorsPerExtension() {
        int commitsCount = landscapeAnalysisResults.getCommitsCount();
        if (commitsCount > 0) {
            List<CommitsPerExtension> perExtension = landscapeAnalysisResults.getContributorsPerExtension();

            if (perExtension.size() > 0) {
                int count = perExtension.size();
                int limit = 100;
                if (perExtension.size() > limit) {
                    perExtension = perExtension.subList(0, limit);
                }
                landscapeReport.startSubSection("Commits & File Extensions (" + count + ")", "");

                landscapeReport.startShowMoreBlock("show details...");

                landscapeReport.startTable("");
                landscapeReport.addTableHeader("", "Extension",
                        "# contributors<br>30 days", "# commits<br>30 days", "# files<br>30 days",
                        "# contributors<br>90 days", "# commits<br>90 days", "# files<br>90 days",
                        "# contributors", "# commits", "# files");

                perExtension.forEach(commitsPerExtension -> {
                    addCommitExtension(commitsPerExtension);
                });
                landscapeReport.endTable();
                if (perExtension.size() < count) {
                    landscapeReport.addParagraph("Showing top " + limit + " items (out of " + count + ").");
                }

                landscapeReport.endShowMoreBlock();

                landscapeReport.endSection();
            }
        }
    }

    private void addContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_contributors.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_contributors.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/contributors.txt");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addRecentContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_contributors_30_days.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_contributors_30_days.html");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addCommitExtension(CommitsPerExtension commitsPerExtension) {
        landscapeReport.startTableRow(commitsPerExtension.getCommitters30Days().size() > 0 ? "font-weight: bold;"
                : "color: " + (commitsPerExtension.getCommitters90Days().size() > 0 ? "grey" : "lightgrey"));
        String extension = commitsPerExtension.getExtension();
        landscapeReport.addTableCell("" + DataImageUtils.getLangDataImageDiv42(extension), "text-align: center;");
        landscapeReport.addTableCell("" + extension, "text-align: center; max-width: 100px; width: 100px");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters30Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters90Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount(), "text-align: center;");
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
    }

    private void addRepositoriesSection(LandscapeConfiguration configuration, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
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
        ProcessingStopwatch.end("reporting/repositories/file age & freshness");

        landscapeReport.startSubSection("<a href='repositories-short.html' target='_blank' style='text-decoration: none'>" +
                "All Repositories (" + repositoryAnalysisResults.size() + ")</a>&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "");
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

            landscapeReport.addHtmlContent("<iframe src='repositories-short.html' frameborder=0 style='height: 600px; width: 100%; margin-left: 0; margin-bottom: 0px; padding: 0;'></iframe>");

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
                            ((thresholdContributors > 0) ? "have ≤ " + FormattingUtils.formatCountPlural(thresholdContributors, "contributor", "contributors") + "; " : "") +
                            (thresholdLocMain > 0 ? "have less than " + thresholdLocMain + " lines of main code" : ""),
                    "color: grey; margin: 10px; font-size: 80%");
        }

        landscapeReport.endSection();

        landscapeReport.startSubSection("<a href='repositories-extensions.html' target='_blank' style='text-decoration: none'>" +
                "File Extension Stats</a>&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "");
        landscapeReport.startDiv("margin-bottom: 18px;");
        landscapeReport.addNewTabLink("<b>Open expanded view</b> (stats per sub-folder)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-extensions-matrix.html");
        landscapeReport.endDiv();
        landscapeReport.addHtmlContent("<iframe src='repositories-extensions.html' frameborder=0 style='height: 600px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");
        landscapeReport.endSection();

    }

    private void addTagsSection(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        if (repositoryAnalysisResults.size() > 0) {
            landscapeReport.startDiv("margin-top: 14px;");
            landscapeReport.addNewTabLink("<b>Open in a new tab&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-tags.html");
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

    private void addInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "skyblue; opacity: 0.8", tooltip);
    }

    private void addInfoBlock(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color + "; opacity: 0.8", tooltip);
    }

    private void addFreshInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        addFreshInfoBlock(mainValue, subtitle, description, tooltip, "skyblue");
    }

    private void addFreshInfoBlock(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip);
    }

    private void addSecondaryFreshInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 60%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "#c0c0c0", tooltip);
    }

    private String getExtraLocInfo() {
        String info = "";

        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getMainLoc()) + " LOC (main)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getTestLoc()) + " LOC (test)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getGeneratedLoc()) + " LOC (generated)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getBuildAndDeploymentLoc()) + " LOC (build and deployment)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getOtherLoc()) + " LOC (other)";

        return info;
    }

    private String getSecondaryLocInfo() {
        String info = "";

        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getTestLoc()) + " LOC (test)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getGeneratedLoc()) + " LOC (generated)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getBuildAndDeploymentLoc()) + " LOC (build and deployment)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getOtherLoc()) + " LOC (other)";

        return info;
    }

    private String getExtraPeopleInfo(List<ContributorRepositories> contributors, long contributorsCount) {
        String info = "";

        int recentContributorsCount6Months = landscapeAnalysisResults.getRecentContributorsCount6Months();
        int recentContributorsCount3Months = landscapeAnalysisResults.getRecentContributorsCount3Months();
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getRecentContributorsCount()) + " contributors (30 days)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount3Months) + " contributors (3 months)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount6Months) + " contributors (6 months)\n";

        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdCommits = configuration.getContributorThresholdCommits();
        info += FormattingUtils.getPlainTextForNumber((int) contributorsCount) + " contributors (all time)\n";
        info += "\nOnly the contributors with " + (thresholdCommits > 1 ? "(" + thresholdCommits + "+&nbsp;commits)" : "") + " included";

        return info;
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        addPeopleInfoBlock(mainValue, subtitle, description, tooltip, PEOPLE_COLOR);
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip);
    }

    private void addCommitsInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "#fefefe", tooltip);
    }

    private void addInfoBlockWithColor(String mainValue, String subtitle, String color, String tooltip) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        landscapeReport.startDiv(style, tooltip);
        String specialColor = mainValue.equals("<b>0</b>") ? " color: grey;" : "";
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px;" + specialColor + "'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 15px;" + specialColor + "'>" + subtitle + "</div>");
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


    private void addSmallInfoBlockLoc(String value, String subtitle, String link) {
        addSmallInfoBlock(value, subtitle, "skyblue", link);
    }

    private void addSmallInfoBlockPeople(String value, String subtitle, String link) {
        addSmallInfoBlock(value, subtitle, "lavender", link);
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
        reports.add(this.landscapeContributorsReport);
        reports.add(this.landscapeRecentContributorsReport);

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

    private void addContributorsPerWeek() {
        int limit = 104;
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(landscapeAnalysisResults.getContributorsPerWeek(),
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerWeek.size() > 0) {
            if (contributorsPerWeek.size() > limit) {
                contributorsPerWeek = contributorsPerWeek.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerWeek.size() >= 4 ? 4 : contributorsPerWeek.size();

            addChartRows(contributorsPerWeek, "weeks", minMaxWindow,
                    (timeSlot, rookiesOnly) -> getContributorsPerWeek(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerWeek(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerWeek(timeSlot, false), 14);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addContributorsPerMonth() {
        int limit = 24;
        List<ContributionTimeSlot> monthlyContributions = landscapeAnalysisResults.getContributorsPerMonth();
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsPerRepositoryAndMonth(), "repositories").exportRacingChart(reportsFolder);
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsCommits(), "contributors").exportRacingChart(reportsFolder);
        List<ContributionTimeSlot> contributorsPerMonth = getContributionMonths(monthlyContributions,
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerMonth.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerMonth.size() > 0) {
            if (contributorsPerMonth.size() > limit) {
                contributorsPerMonth = contributorsPerMonth.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerMonth.size() >= 3 ? 3 : contributorsPerMonth.size();

            addChartRows(contributorsPerMonth, "months", minMaxWindow, (timeSlot, rookiesOnly) -> getContributorsPerMonth(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerMonth(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerMonth(timeSlot, false), 40);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addContributorsPerYear() {
        List<ContributionTimeSlot> yearlyContributions = landscapeAnalysisResults.getContributorsPerYear();
        List<ContributionTimeSlot> contributorsPerYear = getContributionYears(yearlyContributions,
                landscapeAnalysisResults.getConfiguration().getCommitsMaxYears(), landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerYear.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerYear.size() > 0) {
            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerYear.size() >= 3 ? 3 : contributorsPerYear.size();

            addChartRows(contributorsPerYear, "years", minMaxWindow, (timeSlot, rookiesOnly) -> getSignificantContributorsPerYear(landscapeAnalysisResults.getContributors(), timeSlot, rookiesOnly, landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerYear(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerYear(timeSlot, false), 64);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addChartRows(List<ContributionTimeSlot> contributorsPerWeek, String unit, int minMaxWindow, ContributorsExtractor contributorsExtractor, ContributorsExtractor firstContributorsExtractor, ContributorsExtractor lastContributorsExtractor, int barWidth) {
        addTickMarksPerWeekRow(contributorsPerWeek, barWidth);
        addCommitsPerWeekRow(contributorsPerWeek, minMaxWindow, barWidth);
        addContributersPerWeekRow(contributorsPerWeek, contributorsExtractor);
        int maxContributors = contributorsPerWeek.stream().mapToInt(c -> contributorsExtractor.getContributors(c.getTimeSlot(), false).size()).max().orElse(1);
        addContributorsPerTimeUnitRow(contributorsPerWeek, firstContributorsExtractor, maxContributors, true, "bottom");
        addContributorsPerTimeUnitRow(contributorsPerWeek, lastContributorsExtractor, maxContributors, false, "top");
    }

    private void addContributersPerWeekRow(List<ContributionTimeSlot> contributorsPerWeek, ContributorsExtractor contributorsExtractor) {
        landscapeReport.startTableRow();
        int max = 1;
        for (ContributionTimeSlot contributionTimeSlot : contributorsPerWeek) {
            max = Math.max(contributorsExtractor.getContributors(contributionTimeSlot.getTimeSlot(), false).size(), max);
        }
        int maxContributors = max;
        landscapeReport.addTableCell("<b>Contributors</b>" +
                "<div style='font-size: 80%; margin-left: 8px'><div style='color: green'>rookies</div> vs.<div style='color: #588BAE'>veterans</div></div>", "border: none");
        contributorsPerWeek.forEach(week -> {
            landscapeReport.startTableCell("max-width: 20px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px");
            List<String> contributors = contributorsExtractor.getContributors(week.getTimeSlot(), false);
            List<String> rookies = contributorsExtractor.getContributors(week.getTimeSlot(), true);
            int count = contributors.size();
            int rookiesCount = rookies.size();
            int height = 2 + (int) (64.0 * count / maxContributors);
            int heightRookies = 1 + (int) (64.0 * rookiesCount / maxContributors);
            String title = "period " + week.getTimeSlot() + " = " + count + " contributors (" + rookiesCount + " rookies):\n\n" +
                    contributors.subList(0, contributors.size() < 200 ? contributors.size() : 200).stream().collect(Collectors.joining(", "));
            String yearString = week.getTimeSlot().split("[-]")[0];

            String color = "darkgrey";

            if (StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                color = year % 2 == 0 ? "#89CFF0" : "#588BAE";
            }

            landscapeReport.addHtmlContent("<div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: green; height:" + (heightRookies) + "px; margin: 1px'></div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + (height - heightRookies) + "px; margin: 1px'></div>");
            landscapeReport.addHtmlContent("</div>");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    private void addContributorsPerTimeUnitRow(List<ContributionTimeSlot> contributorsPerWeek, ContributorsExtractor contributorsExtractor, int maxContributors, boolean first, final String valign) {
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("<b>" + (first ? "First" : "Last") + " Contribution</b>" +
                "<div style='color: grey; font-size: 80%; margin-left: 8px; margin-top: 4px;'>"
                + "</div>", "border: none; vertical-align: " + (first ? "bottom" : "top"));
        boolean firstItem[] = {true};
        contributorsPerWeek.forEach(timeUnit -> {
            landscapeReport.startTableCell("max-width: 20px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: " + valign + "; font-size: 80%; height: 100px");
            List<String> contributors = contributorsExtractor.getContributors(timeUnit.getTimeSlot(), true);
            int count = contributors.size();
            int height = 4 + (int) (64.0 * count / maxContributors);
            String title = "timeUnit of " + timeUnit.getTimeSlot() + " = " + count + " contributors:\n\n" +
                    contributors.subList(0, contributors.size() < 200 ? contributors.size() : 200).stream().collect(Collectors.joining(", "));
            String yearString = timeUnit.getTimeSlot().split("[-]")[0];

            String color = "lightgrey";

            if (count > 0 && StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                if (first) {
                    color = year % 2 == 0 ? "limegreen" : "darkgreen";
                } else {
                    if (firstItem[0]) {
                        color = "rgba(220,220,220,100)";
                    } else {
                        color = year % 2 == 0 ? "crimson" : "rgba(100,0,0,100)";
                    }
                }
            } else {
                height = 1;
            }

            if (first && count > 0) {
                landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>");
            }
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px; margin: 1px'></div>");
            if (!first && count > 0) {
                landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>");
            }
            landscapeReport.endTableCell();
            firstItem[0] = false;
        });
        landscapeReport.endTableRow();
    }

    private void addTickMarksPerWeekRow(List<ContributionTimeSlot> contributorsPerWeek, int barWidth) {
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("", "border: none");

        for (int i = 0; i < contributorsPerWeek.size(); i++) {
            ContributionTimeSlot week = contributorsPerWeek.get(i);

            String yearString = week.getTimeSlot().split("[-]")[0];

            String color = "darkgrey";

            if (StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                color = year % 2 == 0 ? "#c9c9c9" : "#656565";
            }
            String[] splitNow = week.getTimeSlot().split("-");
            String textNow = splitNow.length < 2 ? splitNow[0] : splitNow[0] + "<br>" + splitNow[1];

            int colspan = 1;

            while (true) {
                String nextTimeSlot = contributorsPerWeek.size() > i + 1 ? contributorsPerWeek.get(i + 1).getTimeSlot() : "";
                String[] splitNext = nextTimeSlot.split("-");
                String textNext = splitNext.length < 2 ? "" : splitNext[0] + "<br>" + splitNext[1];
                if (contributorsPerWeek.size() <= i + 1 || !textNow.equalsIgnoreCase(textNext)) {
                    break;
                }
                colspan++;
                i++;
            }
            landscapeReport.startTableCellColSpan(colspan, "width: "
                    + barWidth + "px; min-width: "
                    + barWidth + "px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 16px");
            landscapeReport.addHtmlContent("<div style='width: 100%; margin: 1px; font-size: 80%; color: '" + color + ">"
                    + textNow + "</div>");
            landscapeReport.endTableCell();
        }
        landscapeReport.endTableRow();
    }

    private void addCommitsPerWeekRow(List<ContributionTimeSlot> contributorsPerWeek, int minMaxWindow, int barWidth) {
        landscapeReport.startTableRow();
        int maxCommits = contributorsPerWeek.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);
        int maxCommits4Weeks = contributorsPerWeek.subList(0, minMaxWindow).stream().mapToInt(c -> c.getCommitsCount()).max().orElse(0);
        int minCommits4Weeks = contributorsPerWeek.subList(0, minMaxWindow).stream().mapToInt(c -> c.getCommitsCount()).min().orElse(0);
        landscapeReport.addTableCell("<b>Commits</b>" +
                "<div style='color: grey; font-size: 80%; margin-left: 8px; margin-top: 4px;'>"
                + "min (" + minMaxWindow + " weeks): " + minCommits4Weeks
                + "<br>max (" + minMaxWindow + " weeks): " + maxCommits4Weeks + "</div>", "border: none");
        contributorsPerWeek.forEach(week -> {
            landscapeReport.startTableCell("width: " + barWidth + "px; min-width: " + barWidth + "px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px");
            int count = week.getCommitsCount();
            int height = 1 + (int) (64.0 * count / maxCommits);
            String title = "week of " + week.getTimeSlot() + " = " + count + " commits";
            String yearString = week.getTimeSlot().split("[-]")[0];

            String color = "darkgrey";

            if (StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                color = year % 2 == 0 ? "#c9c9c9" : "#656565";
            }

            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 70%; margin: 0px'>" + count + "</div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px; margin: 1px'></div>");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
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

                updateTimeSlotMap(contributorRepositories, contributorsPerWeekMap, rookiesPerWeekMap, week, week);
                updateTimeSlotMap(contributorRepositories, contributorsPerMonthMap, rookiesPerMonthMap, month, month + "-01");
                updateTimeSlotMap(contributorRepositories, contributorsPerYearMap, rookiesPerYearMap, year, year + "-01-01");
            });
        });

    }

    private List<String> getSignificantContributorsPerYear(List<ContributorRepositories> contributorRepositories, String year, boolean rookiesOnly, int thresholdCommitDays) {
        if (rookiesOnly) {
            return getLastContributorsPerYear(year, true);
        }
        return contributorRepositories.stream()
                .filter(c -> c.getContributor().getCommitDates().stream().filter(d -> d.startsWith(year)).count() >= thresholdCommitDays)
                .map(c -> c.getContributor().getEmail())
                .collect(Collectors.toList());
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

    private List<String> getContributorsPerWeek(String week, boolean rookiesOnly) {
        Map<String, List<String>> map = rookiesOnly ? rookiesPerWeekMap : contributorsPerWeekMap;
        return map.containsKey(week) ? map.get(week) : new ArrayList<>();
    }

    private List<String> getLastContributorsPerWeek(String week, boolean first) {
        Map<String, String> emails = new HashMap();

        landscapeAnalysisResults.getContributors().stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !DateUtils.getWeekMonday(c.getContributor().getFirstCommitDate()).equals(DateUtils.getWeekMonday(c.getContributor().getLatestCommitDate())))
                // .filter(c -> c.getContributor().getCommitDates().size() >= landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear())
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if (DateUtils.getWeekMonday(first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(week)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private List<String> getContributorsPerMonth(String month, boolean rookiesOnly) {
        Map<String, List<String>> map = rookiesOnly ? rookiesPerMonthMap : contributorsPerMonthMap;
        return map.containsKey(month) ? map.get(month) : new ArrayList<>();
    }

    private List<String> getContributorsPerYear(String year, boolean rookiesOnly) {
        Map<String, List<String>> map = rookiesOnly ? rookiesPerYearMap : contributorsPerYearMap;
        return map.containsKey(year) ? map.get(year) : new ArrayList<>();
    }

    private List<String> getLastContributorsPerYear(String year, boolean first) {
        Map<String, String> emails = new HashMap();

        landscapeAnalysisResults.getContributors().stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !DateUtils.getYear(c.getContributor().getLatestCommitDate()).equals(DateUtils.getYear(c.getContributor().getFirstCommitDate())))
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if (DateUtils.getYear(first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(year)) {
                        String email = contributor.getEmail();
                        // only look at contributors with at least 10 commits days per year
                        if (contributor.getCommitDates().size() >= landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()) {
                            emails.put(email, email);
                        }
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private List<String> getLastContributorsPerMonth(String month, boolean first) {
        Map<String, String> emails = new HashMap();

        landscapeAnalysisResults.getContributors().stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !DateUtils.getMonth(c.getContributor().getLatestCommitDate()).equals(DateUtils.getMonth(c.getContributor().getFirstCommitDate())))
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if (DateUtils.getMonth(first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(month)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private void renderPeopleDependencies(List<ComponentDependency> peopleDependencies,
                                          List<ComponentDependency> peopleRepositoryDependencies,
                                          List<ContributorConnections> contributorConnections,
                                          double cIndex, double pIndex,
                                          double cMean, double pMean,
                                          double cMedian, double pMedian,
                                          int daysAgo) {
        List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
        List<ComponentDependency> repositoryDependenciesViaPeople = ContributorConnectionUtils.getRepositoryDependenciesViaPeople(contributors, 0, daysAgo);

        landscapeReport.addLevel2Header("Contributor Dependencies (past " + daysAgo + " days)", "margin-top: 40px");

        landscapeReport.startTable();
        landscapeReport.startTableRow();
        landscapeReport.startTableCell("border: none; vertical-align: top");
        landscapeReport.addHtmlContent(DEPENDENCIES_ICON);
        landscapeReport.endTableCell();
        landscapeReport.startTableCell("border: none");
        addPeopleGraph(peopleDependencies, daysAgo, "", "");
        if (peopleRepositoryDependencies != null) {
            addPeopleGraph(peopleRepositoryDependencies, daysAgo, "including_repositories_", " (including repositories)");
        }
        addRepositoriesGraph(daysAgo, repositoryDependenciesViaPeople);
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
        landscapeReport.endTable();

        List<Double> activeContributors30DaysHistory = landscapeAnalysisResults.getActiveContributors30DaysHistory();
        if (activeContributors30DaysHistory.size() > 0 && daysAgo == 30) {
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            addDataSection("Active Contributors", activeContributors30DaysHistory.get(0), daysAgo, activeContributors30DaysHistory,
                    "An active contributor is anyone who has committed code changes in past " + daysAgo + " days.");
        }

        landscapeReport.startShowMoreBlock("show details...");
        landscapeReport.startDiv("margin-left: 4px; padding-left: 4px; border-left: 3px solid lightgrey;");
        List<Double> peopleDependenciesCount30DaysHistory = landscapeAnalysisResults.getPeopleDependenciesCount30DaysHistory();
        if (peopleDependenciesCount30DaysHistory.size() > 0 && daysAgo == 30) {
            addDataSection("Unique Contributor-to-Contributor (C2C) Connections",
                    peopleDependenciesCount30DaysHistory.get(0), daysAgo, peopleDependenciesCount30DaysHistory,
                    "C2C dependencies are measured via the same repositories that two persons changed in the past " + daysAgo + " days. " +
                            "<br>Currently there are <b>" + FormattingUtils.formatCount(peopleDependencies.size()) + "</b> " +
                            "unique contributor-to-contributor (C2C) connections via <b>" +
                            landscapeAnalysisResults.getRepositoriesCount() + "</b> shared repositories.");
        }

        addDataSection("C-median", cMedian, daysAgo, landscapeAnalysisResults.getcMedian30DaysHistory(),
                "C-median is the average number of contributors a person worked with in the past " + daysAgo + " days.");
        addDataSection("C-mean", cMean, daysAgo, landscapeAnalysisResults.getcMean30DaysHistory(),
                "C-mean is the mean number of contributors a person worked with in the past " + daysAgo + " days.");
        addDataSection("C-index", cIndex, daysAgo, landscapeAnalysisResults.getcIndex30DaysHistory(),
                "you have people with " + cIndex + " or more repository connections with other people");
        landscapeReport.addLineBreak();

        addDataSection("R-median", pMedian, daysAgo, landscapeAnalysisResults.getpMedian30DaysHistory(),
                "R-median is the average number of repositories a person worked on in the past " + daysAgo + " days.");
        addDataSection("R-mean", pMean, daysAgo, landscapeAnalysisResults.getpMean30DaysHistory(),
                "R-mean is the mean number of repositories a person worked on in the past " + daysAgo + " days.");
        addDataSection("R-index", pIndex, daysAgo, landscapeAnalysisResults.getpIndex30DaysHistory(),
                "you have " + pIndex + " people committing to " + pIndex + " or more repositories");
        landscapeReport.addLineBreak();

        peopleDependencies.sort((a, b) -> b.getCount() - a.getCount());

        addMostConnectedPeopleSection(contributorConnections, daysAgo);
        addMostRepositoriesPeopleSection(contributorConnections, daysAgo);
        addTopConnectionsSection(peopleDependencies, daysAgo, contributors);
        addRepositoryContributors(contributors, daysAgo);
        addRepositoryDependenciesViaPeople(repositoryDependenciesViaPeople);

        landscapeReport.endDiv();
        landscapeReport.endShowMoreBlock();
    }

    private void addRepositoriesGraph(int daysAgo, List<ComponentDependency> repositoryDependenciesViaPeople) {
        landscapeReport.startShowMoreBlock("show repository dependencies graph...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Repository 1\tRepository 2\t# people\n");
        repositoryDependenciesViaPeople.subList(0, Math.min(10000, repositoryDependenciesViaPeople.size())).forEach(d -> builder
                .append(d.getFromComponent()).append("\t")
                .append(d.getToComponent()).append("\t")
                .append(d.getCount()).append("\n"));
        String fileName = "repository_dependencies_via_people_" + daysAgo + "_days.txt";
        saveData(fileName, builder.toString());

        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("See data...", "data/" + fileName);

        List<String> repositoryNames = landscapeAnalysisResults.getRepositoryAnalysisResults().stream()
                .filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() > 0)
                .map(p -> "[" + p.getAnalysisResults().getMetadata().getName() + "]")
                .collect(Collectors.toList());

        String graphId = addDependencyGraphVisuals(repositoryDependenciesViaPeople, new ArrayList<>(repositoryNames), "repository_dependencies_" + daysAgo + "_", "TB");

        landscapeReport.endShowMoreBlock();
        landscapeReport.addNewTabLink(" - show repository dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/" + graphId + "_force_3d.html");
        landscapeReport.addLineBreak();
    }

    private void addDataSection(String type, double value, int daysAgo, List<Double> history, String info) {
        if (StringUtils.isNotBlank(info)) {
            landscapeReport.addParagraph(type + ": <b>" + ((int) Math.round(value)) + "</b>");
            landscapeReport.addParagraph("<span style='color: #a2a2a2; font-size: 90%;'>" + info + "</span>", "margin-top: -12px;");
        } else {
            landscapeReport.addParagraph(type + ": <b>" + ((int) Math.round(value)) + "</b>");
        }

        if (daysAgo == 30 && history.size() > 0) {
            landscapeReport.startTable("border: none");
            landscapeReport.startTableRow("font-size: 70%;");
            double max = history.stream().max(Double::compare).get();
            history.forEach(historicalValue -> {
                landscapeReport.startTableCell("text-align: center; vertical-align: bottom;border: none");
                landscapeReport.addContentInDiv((int) Math.round(historicalValue) + "", "width: 20px;border: none");
                landscapeReport.addContentInDiv("", "width: 20px; background-color: skyblue; border: none; height:"
                        + (int) (1 + Math.round(40.0 * historicalValue / max)) + "px;");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();
            landscapeReport.startTableRow("font-size: 70%;");
            landscapeReport.addTableCell("<b>now</b>", "border: none");
            landscapeReport.addTableCell("1m<br>ago", "text-align: center; border: none");
            for (int i = 0; i < history.size() - 2; i++) {
                landscapeReport.addTableCell((i + 2) + "m<br>ago", "text-align: center; border: none");
            }
            landscapeReport.endTableRow();
            landscapeReport.endTable();
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
        }
    }

    private void addRepositoryDependenciesViaPeople(List<ComponentDependency> repositoryDependenciesViaPeople) {
        landscapeReport.startShowMoreBlock("show repository dependencies via people...<br>");
        landscapeReport.startTable();
        int maxListSize = Math.min(100, repositoryDependenciesViaPeople.size());
        if (maxListSize < repositoryDependenciesViaPeople.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + maxListSize + " items (out of " + repositoryDependenciesViaPeople.size() + ").");
        } else {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + maxListSize + (maxListSize == 1 ? " item" : " items") + ".");
        }
        repositoryDependenciesViaPeople.subList(0, maxListSize).forEach(dependency -> {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(dependency.getFromComponent());
            landscapeReport.addTableCell(dependency.getToComponent());
            landscapeReport.addTableCell(dependency.getCount() + (dependency.getCount() == 1 ? " person" : " people"));
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addRepositoryContributors(List<ContributorRepositories> contributors, int daysAgo) {
        Map<String, Pair<String, Integer>> map = new HashMap<>();
        final List<String> list = new ArrayList<>();

        contributors.forEach(contributorRepositories -> {
            contributorRepositories.getRepositories().stream().filter(repository -> DateUtils.isAnyDateCommittedBetween(repository.getCommitDates(), 0, daysAgo)).forEach(repository -> {
                String key = repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
                if (map.containsKey(key)) {
                    Integer currentValue = map.get(key).getRight();
                    map.put(key, Pair.of(key, currentValue + 1));
                } else {
                    Pair<String, Integer> pair = Pair.of(key, 1);
                    map.put(key, pair);
                    list.add(key);
                }
            });
        });

        Collections.sort(list, (a, b) -> map.get(b).getRight() - map.get(a).getRight());

        List<String> displayList = list;
        if (list.size() > 100) {
            displayList = list.subList(0, 100);
        }

        landscapeReport.startShowMoreBlock("show repositories with most people...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# people\n");
        list.forEach(repository -> builder.append(map.get(repository).getLeft()).append("\t")
                .append(map.get(repository).getRight()).append("\n"));
        String prefix = "repository_with_most_people_" + daysAgo + "_days";
        String fileName = prefix + ".txt";
        saveData(fileName, builder.toString());

        if (displayList.size() < list.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top 100 items (out of " + list.size() + ").");
        }
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        list.forEach(repository -> visualizationItems.add(new VisualizationItem(repository, map.get(repository).getRight())));
        exportVisuals(prefix, visualizationItems);
        landscapeReport.startTable();
        displayList.forEach(repository -> {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(map.get(repository).getLeft());
            Integer count = map.get(repository).getRight();
            landscapeReport.addTableCell(count + (count == 1 ? " person" : " people"));
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addPeopleGraph(List<ComponentDependency> peopleDependencies, int daysAgo, String suffix, String extraLabel) {
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor 1\tContributor 2\t# shared repositories\n");
        peopleDependencies.forEach(d -> builder
                .append(d.getFromComponent()).append("\t")
                .append(d.getToComponent()).append("\t")
                .append(d.getCount()).append("\n"));
        String fileName = "repository_shared_repositories_" + suffix + daysAgo + "_days.txt";
        saveData(fileName, builder.toString());

        landscapeReport.startShowMoreBlock("show contributor dependencies graph..." + extraLabel + "<br>");
        landscapeReport.startDiv("border-left: 6px solid lightgrey; padding-left: 4px; margin-left: 4px; overflow-x: auto");
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("See data...", "data/" + fileName);


        String orientation = suffix.length() > 0 ? "LR" : "TB";
        String graphId = addDependencyGraphVisuals(peopleDependencies, new ArrayList<>(),
                "people_dependencies_" + suffix + daysAgo + "_", orientation);
        landscapeReport.endDiv();
        landscapeReport.endShowMoreBlock();

        landscapeReport.addNewTabLink(" - show contributor dependencies as 3D force graph" + extraLabel + "&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON,
                "visuals/" + graphId + "_force_3d.html");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addTopConnectionsSection(List<ComponentDependency> peopleDependencies, int daysAgo, List<ContributorRepositories> contributors) {
        landscapeReport.startShowMoreBlock("show top connections...<br>");
        landscapeReport.startTable();
        List<ComponentDependency> displayListConnections = peopleDependencies.subList(0, Math.min(100, peopleDependencies.size()));
        if (displayListConnections.size() < peopleDependencies.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + displayListConnections.size() + " items (out of " + peopleDependencies.size() + ").");
        } else {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + displayListConnections.size() + (displayListConnections.size() == 1 ? " item" : " items") + ".");
        }
        int index[] = {0};
        displayListConnections.forEach(dependency -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();
            int dependencyCount = dependency.getCount();
            landscapeReport.addTableCell(index[0] + ".");
            int repositoryCount1 = ContributorConnectionUtils.getRepositoryCount(contributors, from, 0, daysAgo);
            int repositoryCount2 = ContributorConnectionUtils.getRepositoryCount(contributors, to, 0, daysAgo);
            double perc1 = 0;
            double perc2 = 0;
            if (repositoryCount1 > 0) {
                perc1 = 100.0 * dependencyCount / repositoryCount1;
            }
            if (repositoryCount2 > 0) {
                perc2 = 100.0 * dependencyCount / repositoryCount2;
            }
            landscapeReport.addTableCell(from + "<br><span style='color: grey'>" + repositoryCount1 + " repositories (" + FormattingUtils.getFormattedPercentage(perc1) + "%)</span>", "");
            landscapeReport.addTableCell(to + "<br><span style='color: grey'>" + repositoryCount2 + " repositories (" + FormattingUtils.getFormattedPercentage(perc2) + "%)</span>", "");
            landscapeReport.addTableCell(dependencyCount + " shared repositories", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addMostConnectedPeopleSection(List<ContributorConnections> contributorConnections, int daysAgo) {
        landscapeReport.startShowMoreBlock("show most connected people...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# repositories\t# connections\n");
        contributorConnections.forEach(c -> builder.append(c.getEmail()).append("\t")
                .append(c.getRepositoriesCount()).append("\t")
                .append(c.getConnectionsCount()).append("\n"));
        String prefix = "most_connected_people_" + daysAgo + "_days";
        String fileName = prefix + ".txt";

        saveData(fileName, builder.toString());

        List<ContributorConnections> displayListPeople = contributorConnections.subList(0, Math.min(100, contributorConnections.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addHtmlContent("<p>&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + "). ");
        } else {
            landscapeReport.addHtmlContent("<p>&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ". ");
        }
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        contributorConnections.forEach(c -> visualizationItems.add(new VisualizationItem(c.getEmail(), c.getConnectionsCount())));
        exportVisuals(prefix, visualizationItems);
        landscapeReport.addHtmlContent("</p>");
        int index[] = {0};
        landscapeReport.startTable();
        displayListPeople.forEach(name -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(index[0] + ".", "");
            landscapeReport.addTableCell(name.getEmail(), "");
            landscapeReport.addTableCell(name.getRepositoriesCount() + "&nbsp;repositories");
            landscapeReport.addTableCell(name.getConnectionsCount() + " connections", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();

    }

    private void addMostRepositoriesPeopleSection(List<ContributorConnections> contributorConnections, int daysAgo) {
        landscapeReport.startShowMoreBlock("show people with most repositories...<br>");
        List<ContributorConnections> sorted = new ArrayList<>(contributorConnections);
        sorted.sort((a, b) -> b.getRepositoriesCount() - a.getRepositoriesCount());
        List<ContributorConnections> displayListPeople = sorted.subList(0, Math.min(100, sorted.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addHtmlContent("<p>&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + "). ");
        } else {
            landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ". ");
        }
        String prefix = "most_repositories_people_" + daysAgo + "_days";
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# repositories\t# connections\n");
        contributorConnections.forEach(c -> builder.append(c.getEmail()).append("\t")
                .append(c.getRepositoriesCount()).append("\t")
                .append(c.getConnectionsCount()).append("\n"));
        String fileName = prefix + ".txt";
        saveData(fileName, builder.toString());

        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        contributorConnections.forEach(c -> visualizationItems.add(new VisualizationItem(c.getEmail(), c.getRepositoriesCount())));
        int index[] = {0};
        landscapeReport.startTable();
        displayListPeople.forEach(name -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(index[0] + ".", "");
            landscapeReport.addTableCell(name.getEmail(), "");
            landscapeReport.addTableCell(name.getRepositoriesCount() + "&nbsp;repositories");
            landscapeReport.addTableCell(name.getConnectionsCount() + " connections", "");
            landscapeReport.endTableRow();
        });

        exportVisuals(prefix, visualizationItems);
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void exportVisuals(String prefix, List<VisualizationItem> visualizationItems) {
        try {
            new LandscapeVisualsGenerator(reportsFolder).exportVisuals(prefix, visualizationItems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames,
                                             String prefix, String orientation) {
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setOrientation(orientation);
        graphvizDependencyRenderer.setArrow("--");

        if (100 < componentDependencies.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + 100 + " items (out of " + componentDependencies.size() + ").");
        } else {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + componentDependencies.size() + (componentDependencies.size() == 1 ? " item" : " items") + ".");
        }
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(componentNames, componentDependencies);
        String graphId = prefix + dependencyVisualCounter++;
        landscapeReport.addGraphvizFigure(graphId, "", graphvizContent);
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();

        addDownloadLinks(graphId);
        new Force3DGraphExporter().export3DForceGraph(componentDependencies, reportsFolder, graphId);

        return graphId;
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

    private void saveData(String fileName, String content) {
        File reportsFolder = Paths.get(this.folder.getParent(), "").toFile();
        File folder = Paths.get(reportsFolder.getPath(), "_sokrates_landscape/data").toFile();
        folder.mkdirs();

        try {
            File file = new File(folder, fileName);
            FileUtils.writeStringToFile(file, content, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<RichTextReport> getIndividualContributorReports() {
        return individualContributorReports;
    }

    public void setIndividualContributorReports(List<RichTextReport> individualContributorReports) {
        this.individualContributorReports = individualContributorReports;
    }

    abstract class ZommableCircleCountExtractors {
        public abstract int getCount(RepositoryAnalysisResults repositoryAnalysisResults);
    }
}
