package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PromptsUtils {
    // Renders a prompt's "file to upload" link. Data files now live inside data/data.zip, so a link
    // into the data folder becomes a client-side extract+download (downloadDataFile); other links
    // render as normal anchors.
    private static String renderUploadLink(Link uploadFile) {
        String href = uploadFile.getHref();
        // Only the per-repository data folder ("../data/...") is packaged into data.zip and served
        // via the client-side downloader. Landscape-level data links ("data/...") are loose files
        // in the landscape folder and stay normal anchors.
        if (href != null && href.startsWith("../data/")) {
            String dataEntry = href.substring("../data/".length());
            return "<a href='#' onclick=\"return downloadDataFile('" + dataEntry + "')\">" + uploadFile.getLabel() + "</a>";
        }
        return "<a target='_blank' href='" + href + "'>" + uploadFile.getLabel() + "</a>";
    }

    public static void addRepositoryPromptSection(String promptFile, RichTextReport report, CodeAnalysisResults analysisResults, String title, String subTitle, List<Link> filesToUpload) {
        String promptContent = HtmlTemplateUtils.getResource("/prompts/" + promptFile + ".txt");

        Metadata metadata = analysisResults.getMetadata();
        promptContent = promptContent.replace("${name}", metadata.getName());
        promptContent = promptContent.replace("${description}", StringUtils.defaultIfBlank(metadata.getDescription(), "not provided"));

        report.startSubSection(title, subTitle);
        report.addParagraph("Try on: <a target='_blank' href='https://chatgpt.com/?q=" + URLEncoder.encode(promptContent, StandardCharsets.UTF_8) + "'>OpenAI ChatGPT</a> | <a target='_blank' href='https://claude.ai/new?q=" + URLEncoder.encode(promptContent, StandardCharsets.UTF_8) + "'>Claude.ai Chat</a> | <a target='_blank' href='https://gemini.google.com/'>Google Gemini</a>", "");

        String links = "";

        for (Link uploadFile : filesToUpload) {
            if (StringUtils.isNotBlank(links)) {
                links += " | ";
            }
            links += renderUploadLink(uploadFile);
        }

        report.addParagraph("Files to upload: " + links, "");

        report.addParagraph("Prompt:", "");
        report.addTextArea(promptContent, "width: calc(100% - 5px); height: 20em");
        report.endSection();

    }

    public static void addLandscapePromptSection(String promptFile, RichTextReport report, LandscapeAnalysisResults analysisResults, String title, String subTitle, List<Link> filesToUpload) {
        String promptContent = HtmlTemplateUtils.getResource("/prompts/" + promptFile + ".txt");

        Metadata metadata = analysisResults.getConfiguration().getMetadata();
        promptContent = promptContent.replace("${name}", metadata.getName());
        promptContent = promptContent.replace("${description}", StringUtils.defaultIfBlank(metadata.getDescription(), "not provided"));

        report.startSubSection(title, subTitle);
        report.addParagraph("Try on: <a target='_blank' href='https://chatgpt.com/?q=" + URLEncoder.encode(promptContent, StandardCharsets.UTF_8) + "'>OpenAI ChatGPT</a> | <a target='_blank' href='https://claude.ai/new?q=" + URLEncoder.encode(promptContent, StandardCharsets.UTF_8) + "'>Claude.ai Chat</a> | <a target='_blank' href='https://gemini.google.com/'>Google Gemini</a>", "");

        String links = "";

        for (Link uploadFile : filesToUpload) {
            if (StringUtils.isNotBlank(links)) {
                links += " | ";
            }
            links += renderUploadLink(uploadFile);
        }

        report.addParagraph("Files to upload: " + links, "");

        report.addParagraph("Prompt:", "");
        report.addTextArea(promptContent, "width: calc(100% - 5px); height: 20em");
        report.endSection();

    }
}
