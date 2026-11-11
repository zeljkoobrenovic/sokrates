/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.reports.utils.LanguageColors;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Overview repositories chart groups circles by MAIN LANGUAGE: one colored group circle per
 * language (ordered by total main LOC desc), each containing its repositories' leaves.
 */
class LanguageCirclesTest {

    private LandscapeReportGenerator.RepositoryBubble bubble(String name, String lang, int loc) {
        return new LandscapeReportGenerator.RepositoryBubble(name, lang, loc);
    }

    @Test
    void groupsRepositoriesByLanguage() {
        List<VisualizationItem> groups = LandscapeReportGenerator.groupByLanguage(List.of(
                bubble("svc-a", "java", 1000),
                bubble("svc-b", "java", 500),
                bubble("ml", "python", 800)));

        assertEquals(2, groups.size(), "one group per language");
        // java total (1500) > python (800) → java first.
        assertTrue(groups.get(0).getName().startsWith("[java]"));
        assertTrue(groups.get(1).getName().startsWith("[python]"));
        assertEquals("[java] (2 repositories)", groups.get(0).getName());
        assertEquals("[python] (1 repository)", groups.get(1).getName());

        // The group circle stays UNCOLORED (depth gradient); only the leaves carry the language color.
        assertEquals("", groups.get(0).getColor());
        groups.get(0).getChildren().forEach(leaf ->
                assertEquals(LanguageColors.getColor("java"), leaf.getColor()));
        List<String> javaRepos = groups.get(0).getChildren().stream()
                .map(VisualizationItem::getName).collect(Collectors.toList());
        assertEquals(List.of("svc-a", "svc-b"), javaRepos);
        assertEquals(1000, groups.get(0).getChildren().get(0).getSize());
    }

    @Test
    void leavesCarryColorAndTooltip() {
        List<VisualizationItem> groups = LandscapeReportGenerator.groupByLanguage(List.of(
                bubble("my-repo", "java", 1200)));

        VisualizationItem leaf = groups.get(0).getChildren().get(0);
        assertEquals("my-repo", leaf.getName());
        assertEquals(LanguageColors.getColor("java"), leaf.getColor());
        assertTrue(leaf.getTooltip().startsWith("my-repo · "));
        assertTrue(leaf.getTooltip().endsWith(" · java"));
    }

    @Test
    void skipsBlankLanguageAndZeroLoc() {
        List<VisualizationItem> groups = LandscapeReportGenerator.groupByLanguage(List.of(
                bubble("no-lang", "", 500),
                bubble("empty", "java", 0),
                bubble("real", "java", 100)));

        assertEquals(1, groups.size());
        assertEquals("[java] (1 repository)", groups.get(0).getName());
        assertEquals("real", groups.get(0).getChildren().get(0).getName());
    }

    @Test
    void emptyInputYieldsNoGroups() {
        assertTrue(LandscapeReportGenerator.groupByLanguage(List.of()).isEmpty());
    }
}
