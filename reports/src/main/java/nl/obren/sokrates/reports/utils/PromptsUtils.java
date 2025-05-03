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
            links += "<a target='_blank' href='" + uploadFile.getHref() + "'>" + uploadFile.getLabel() + "</a>";
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
            links += "<a target='_blank' href='" + uploadFile.getHref() + "'>" + uploadFile.getLabel() + "</a>";
        }

        report.addParagraph("Files to upload: " + links, "");

        report.addParagraph("Prompt:", "");
        report.addTextArea(promptContent, "width: calc(100% - 5px); height: 20em");
        report.endSection();

    }
}
