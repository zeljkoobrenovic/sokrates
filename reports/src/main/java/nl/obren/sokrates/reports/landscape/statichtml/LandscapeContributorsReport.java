package nl.obren.sokrates.reports.landscape.statichtml;

import org.apache.commons.lang3.StringUtils;

/**
 * Static helpers for contributor links/avatars. The instance side of this class (the
 * server-rendered contributor/bot/team tables) was removed when the searchable client-rendered
 * {@code contributors-report.html} replaced those tables — only these utilities remain, still used
 * by the contributor report DTOs.
 */
public class LandscapeContributorsReport {

    public static String getContributorUrlFromTemplate(String contributorId, String template) {
        String idVariable = "${contributorid}";
        if (StringUtils.isNotBlank(template) && template.contains(idVariable)) {
            return template.replace(idVariable, contributorId.replaceAll("[@].*", ""));
        }

        return null;
    }

    // Prefer getContributorUrl(email, isTeam) — callers know whether the row is a team (it has
    // members). This set-based overload is a fallback for sites without that context.
    public static String getContributorUrl(String email) {
        return LandscapeIndividualContributorsReports.getContributorReportUrl(email);
    }

    public static String getContributorUrl(String email, boolean isTeam) {
        // Teams open team-report.html; contributors/bots open contributor-report.html. Route on the
        // caller's known type, not the shared team-email set (which is mutable static state reused
        // across landscapes in one JVM run and can mis-route — e.g. all contributors to team-report).
        return LandscapeIndividualContributorsReports.getContributorReportUrl(email, isTeam);
    }

    public static String getAvatarUrl(String contributorId, String linkTemplate) {
        return getContributorUrlFromTemplate(contributorId, linkTemplate);
    }
}
