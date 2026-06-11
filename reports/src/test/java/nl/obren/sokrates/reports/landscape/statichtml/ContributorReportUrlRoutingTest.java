/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Routing of per-person report links across the three self-contained files: teams →
 * team-report.html, recent (last-30-days) contributors → contributor-report.html (small), and
 * everyone else (non-recent contributors + bots) → contributor-report-all.html.
 */
class ContributorReportUrlRoutingTest {

    private ContributorRepositories person(String email, int commits30Days) {
        Contributor c = new Contributor();
        c.setEmail(email);
        c.setCommitsCount30Days(commits30Days);
        return new ContributorRepositories(c);
    }

    @Test
    void recentContributorRoutesToContributorReport() {
        LandscapeIndividualContributorsReports.registerTeams(List.of());
        LandscapeIndividualContributorsReports.registerRecentContributors(List.of(person("recent@x.com", 5)));

        assertEquals("contributors/contributor-report.html?key=recent_x.com",
                LandscapeIndividualContributorsReports.getContributorReportUrl("recent@x.com"));
    }

    @Test
    void nonRecentContributorRoutesToAllReport() {
        LandscapeIndividualContributorsReports.registerTeams(List.of());
        // old@x.com has no commits in the last 30 days, so it is not registered as recent.
        LandscapeIndividualContributorsReports.registerRecentContributors(List.of(person("old@x.com", 0)));

        assertEquals("contributors/contributor-report-all.html?key=old_x.com",
                LandscapeIndividualContributorsReports.getContributorReportUrl("old@x.com"));
    }

    @Test
    void unknownEmailRoutesToAllReport() {
        LandscapeIndividualContributorsReports.registerTeams(List.of());
        LandscapeIndividualContributorsReports.registerRecentContributors(List.of(person("recent@x.com", 5)));

        // A bot (or anyone not registered as recent) falls through to the all-time file.
        assertEquals("contributors/contributor-report-all.html?key=bot_x.com",
                LandscapeIndividualContributorsReports.getContributorReportUrl("bot@x.com"));
    }

    @Test
    void teamRoutesToTeamReportEvenIfAlsoRecent() {
        // Team membership takes precedence over the recent check.
        LandscapeIndividualContributorsReports.registerTeams(List.of(person("Team Alpha", 5)));
        LandscapeIndividualContributorsReports.registerRecentContributors(List.of(person("Team Alpha", 5)));

        assertEquals("contributors/team-report.html?key=team_alpha",
                LandscapeIndividualContributorsReports.getContributorReportUrl("Team Alpha"));
    }

    @Test
    void registerRecentReplacesPriorLandscape() {
        // A recent contributor in one landscape must not stay recent in the next (sets are per-run).
        LandscapeIndividualContributorsReports.registerTeams(List.of());
        LandscapeIndividualContributorsReports.registerRecentContributors(List.of(person("recent@x.com", 5)));
        assertEquals("contributors/contributor-report.html?key=recent_x.com",
                LandscapeIndividualContributorsReports.getContributorReportUrl("recent@x.com"));

        LandscapeIndividualContributorsReports.registerRecentContributors(List.of(person("someone-else@x.com", 5)));
        assertEquals("contributors/contributor-report-all.html?key=recent_x.com",
                LandscapeIndividualContributorsReports.getContributorReportUrl("recent@x.com"));
    }
}
