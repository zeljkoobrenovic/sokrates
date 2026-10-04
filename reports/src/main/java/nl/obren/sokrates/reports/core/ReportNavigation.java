/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The sticky left sidebar of the per-repository report (the "app shell"): the repository name, a
 * search field that filters the sidebar items in place, and groups of links. {@link ReportHtmlWriter} renders it
 * around the report body of every page that has one, with the page's own entry marked active.
 * <p>
 * An item with a <code>tab</code> points at a tab of the index page (<code>index.html#&lt;tab&gt;</code>);
 * on the index itself the click is routed in-page (see <code>sokratesOpenTabFromHash</code> in
 * {@link ReportConstants#REPORTS_HTML_HEADER}). Labels and the title may come from the configuration or
 * the repository, so they are HTML-escaped.
 */
public class ReportNavigation {
    // Small line icons (24x24, stroke = currentColor) for the sidebar items, keyed by name. The report
    // icons in /icons are detailed illustrations, unreadable at this size and a few KB each.
    private static final Map<String, String> ICONS = new HashMap<>();

    static {
        ICONS.put("overview", "<rect x='3' y='3' width='7' height='7' rx='1'/><rect x='14' y='3' width='7' height='7' rx='1'/><rect x='3' y='14' width='7' height='7' rx='1'/><rect x='14' y='14' width='7' height='7' rx='1'/>");
        ICONS.put("highlights", "<path d='M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z'/>");
        ICONS.put("analyses", "<path d='M4 20V10M10 20V4M16 20v-7M22 20H2'/>");
        ICONS.put("activity", "<path d='M22 12h-4l-3 8L9 4l-3 8H2'/>");
        ICONS.put("files", "<path d='M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z'/><path d='M14 3v6h6'/>");
        ICONS.put("units", "<path d='M8 3H7a2 2 0 0 0-2 2v5a2 2 0 0 1-2 2 2 2 0 0 1 2 2v5a2 2 0 0 0 2 2h1M16 3h1a2 2 0 0 1 2 2v5a2 2 0 0 0 2 2 2 2 0 0 0-2 2v5a2 2 0 0 1-2 2h-1'/>");
        ICONS.put("commits", "<circle cx='12' cy='12' r='3'/><path d='M3 12h6M15 12h6'/>");
        ICONS.put("structure", "<circle cx='12' cy='12' r='9'/><circle cx='9' cy='10' r='3'/><circle cx='15.5' cy='14.5' r='2.5'/>");
        ICONS.put("visuals", "<rect x='3' y='3' width='18' height='18' rx='2'/><circle cx='9' cy='9' r='2'/><path d='M21 15l-5-5L5 21'/>");
        ICONS.put("data", "<ellipse cx='12' cy='5' rx='8' ry='3'/><path d='M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3'/>");
        ICONS.put("custom", "<rect x='3' y='4' width='18' height='16' rx='2'/><path d='M3 9h18'/>");
        ICONS.put("code", "<path d='M16 18l6-6-6-6M8 6l-6 6 6 6'/>");
        ICONS.put("components", "<rect x='3' y='3' width='8' height='8' rx='1'/><rect x='13' y='13' width='8' height='8' rx='1'/><path d='M11 7h4a2 2 0 0 1 2 2v4'/>");
        ICONS.put("dependencies", "<circle cx='6' cy='6' r='2.5'/><circle cx='18' cy='6' r='2.5'/><circle cx='12' cy='18' r='2.5'/><path d='M7.5 8l3.5 7.5M16.5 8L13 15.5M8.5 6h7'/>");
        ICONS.put("temporal", "<circle cx='12' cy='13' r='8'/><path d='M12 9v4l2.5 2.5M9 2h6'/>");
        ICONS.put("duplication", "<rect x='9' y='9' width='12' height='12' rx='2'/><path d='M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1'/>");
        ICONS.put("size", "<path d='M3 17l14-14 4 4L7 21z'/><path d='M7.5 12.5l2 2M10.5 9.5l2 2M13.5 6.5l2 2'/>");
        ICONS.put("age", "<circle cx='12' cy='12' r='9'/><path d='M12 7v5l3 3'/>");
        ICONS.put("churn", "<path d='M21 12a9 9 0 0 1-15.5 6.2M3 12a9 9 0 0 1 15.5-6.2'/><path d='M21 3v6h-6M3 21v-6h6'/>");
        ICONS.put("contributors", "<circle cx='9' cy='8' r='3.5'/><path d='M2 21v-1a6 6 0 0 1 12 0v1M16 4.5a3.5 3.5 0 0 1 0 7M22 21v-1a6 6 0 0 0-4-5.6'/>");
        ICONS.put("complexity", "<circle cx='6' cy='5' r='2'/><circle cx='6' cy='19' r='2'/><circle cx='18' cy='9' r='2'/><path d='M6 7v10M18 11c0 4-6 3-12 6'/>");
        ICONS.put("file_complexity", "<path d='M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z'/><path d='M14 3v6h6'/><path d='M9 18v-6M9 14c0-2 3-2 3-4M9 14c0 2 3 2 3 4'/>");
        ICONS.put("features", "<path d='M20.6 13.4l-7.2 7.2a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8z'/><circle cx='8' cy='8' r='1.5'/>");
        ICONS.put("metrics", "<path d='M4 9h16M4 15h16M10 3L8 21M16 3l-2 18'/>");
        ICONS.put("controls", "<circle cx='12' cy='12' r='9'/><circle cx='12' cy='12' r='5'/><circle cx='12' cy='12' r='1'/>");
        ICONS.put("notes", "<path d='M15 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10z'/><path d='M15 21v-6h6'/>");
        ICONS.put("trend", "<path d='M3 17l6-6 4 4 8-8M15 7h6v6'/>");
    }

    private final String title;
    private final String homeHref;
    private final List<Group> groups = new ArrayList<>();
    private String generatedOn;
    private String referenceDate;

    public ReportNavigation(String title, String homeHref) {
        this.title = title;
        this.homeHref = homeHref;
    }

    /** The generation date (and the analysis reference date when it differs) for the sidebar footer. */
    public void setGeneratedOn(String generatedOn, String referenceDate) {
        this.generatedOn = generatedOn;
        this.referenceDate = referenceDate;
    }

    public Group addGroup(String label) {
        Group group = new Group(label);
        groups.add(group);
        return group;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public String getHomeHref() {
        return homeHref;
    }

    /**
     * The repository name as the first line of the page header (it used to head the sidebar, where long
     * "owner/repository" names had to wrap), linking to the Overview.
     */
    public String contextLineHtml() {
        return "<a class='sk-page-context' href='" + HtmlEscapeUtils.escape(homeHref) + "'>" + HtmlEscapeUtils.escape(title) + "</a>";
    }

    public String getTitle() {
        return title;
    }

    /** The label of the item with this id (a page's file name or an index tab), or null. */
    public String labelOf(String id) {
        for (Group group : groups) {
            for (Item item : group.items) {
                if (item.id.equals(id)) {
                    return item.label;
                }
            }
        }
        return null;
    }

    /** The subtitle of the item with this id (index tabs), or null. */
    public String subtitleOf(String id) {
        for (Group group : groups) {
            for (Item item : group.items) {
                if (item.id.equals(id)) {
                    return item.subtitle;
                }
            }
        }
        return null;
    }

    /** The "Open in new tab" link of the item with this id, or null. */
    public String subtitleLinkOf(String id) {
        for (Group group : groups) {
            for (Item item : group.items) {
                if (item.id.equals(id)) {
                    return item.subtitleLink;
                }
            }
        }
        return null;
    }

    /** The header's link that opens a page in a new browser tab (escaped; the href must be safe). */
    public static String openInNewTabHtml(String href) {
        return "<a class='sk-open-new-tab' target='_blank' rel='noopener' href='" + HtmlEscapeUtils.escape(href) + "'>Open in new tab ↗</a>";
    }

    /** The sidebar icon of the item with this id at the given size (empty when it has none). */
    public String iconOf(String id, int size) {
        for (Group group : groups) {
            for (Item item : group.items) {
                if (item.id.equals(id)) {
                    return Item.icon(item.icon, size);
                }
            }
        }
        return "";
    }

    /**
     * The page's header title on a page with this sidebar: the label of its sidebar item, so the header
     * and the menu always agree. Null when the page is not in the sidebar.
     */
    public static String pageTitle(RichTextReport report) {
        ReportNavigation navigation = report.getNavigation();
        return navigation == null || report.isEmbedded() ? null : navigation.labelOf(report.getNavigationActiveId());
    }

    public String render(String activeId) {
        StringBuilder html = new StringBuilder();
        html.append("<button type='button' class='sk-nav-toggle' aria-label='Open navigation' onclick='sokratesToggleNav()'>")
                .append("<svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'>")
                .append("<path d='M4 6h16M4 12h16M4 18h16'/></svg></button>\n");
        html.append("<nav class='sk-sidebar' aria-label='Report navigation'>\n");
        // Filters the items below in place (ReportShell's sokratesFilterNav); no popup.
        html.append("<div class='sk-nav-search'>")
                .append("<svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.2' stroke-linecap='round'>")
                .append("<circle cx='11' cy='11' r='7'/><path d='M20 20l-3.5-3.5'/></svg>")
                .append("<input type='search' class='sk-nav-search-input' placeholder='Search…' aria-label='Search the menu' autocomplete='off' ")
                .append("oninput='sokratesFilterNav(this.value)'><kbd class='sk-kbd'>⌘K</kbd></div>\n");
        html.append("<div class='sk-nav-empty' hidden>No matching pages</div>\n");
        groups.forEach(group -> {
            html.append("<div class='sk-nav-group'>\n");
            html.append("<div class='sk-nav-group-label'>").append(HtmlEscapeUtils.escape(group.label)).append("</div>\n");
            group.items.forEach(item -> html.append(item.render(activeId)));
            html.append("</div>\n");
        });
        html.append("<div class='sk-nav-footer'><label class='sk-nav-option'>")
                .append("<input type='checkbox' data-sk-palette-toggle onchange='sokratesSetPalette(this.checked ? \"cvd\" : \"default\")'>")
                .append("<span>Colour-blind safe colours</span></label>\n");
        // What generated the report and with which configuration, on every page (it used to sit below
        // all the index tabs only).
        html.append("<div class='sk-nav-meta'>generated by <a target='_blank' href='https://sokrates.dev/'>sokrates.dev</a>");
        if (generatedOn != null) {
            html.append("<br>on ").append(HtmlEscapeUtils.escape(generatedOn));
        }
        html.append(" · <a href='#' onclick=\"return downloadDataFile('config.json')\">config</a>");
        if (referenceDate != null) {
            html.append("<br>reference date: ").append(HtmlEscapeUtils.escape(referenceDate));
        }
        html.append("</div></div>\n");
        html.append("</nav>\n");
        return html.toString();
    }

    public static class Group {
        private final String label;
        private final List<Item> items = new ArrayList<>();

        Group(String label) {
            this.label = label;
        }

        public Group addItem(String id, String label, String href, String icon) {
            items.add(new Item(id, label, href, icon, null));
            return this;
        }

        public Group addTabItem(String tab, String label, String href, String icon) {
            return addTabItem(tab, label, href, icon, null);
        }

        /** An index tab with the subtitle the page header shows while the tab is open. */
        public Group addTabItem(String tab, String label, String href, String icon, String subtitle) {
            return addTabItem(tab, label, href, icon, subtitle, null);
        }

        /**
         * An index tab whose header subtitle ends with an "Open in new tab" link to subtitleLink (e.g. the
         * page a custom tab embeds); the caller passes only safe (http/https/relative) links.
         */
        public Group addTabItem(String tab, String label, String href, String icon, String subtitle, String subtitleLink) {
            Item item = new Item(tab, label, href, icon, tab);
            item.subtitle = subtitle;
            item.subtitleLink = subtitleLink;
            items.add(item);
            return this;
        }

        public String getLabel() {
            return label;
        }

        public List<Item> getItems() {
            return items;
        }
    }

    public static class Item {
        private final String id;
        private final String label;
        private final String href;
        private final String icon;
        private final String tab;
        private String subtitle;
        private String subtitleLink;

        Item(String id, String label, String href, String icon, String tab) {
            this.id = id;
            this.label = label;
            this.href = href;
            this.icon = icon;
            this.tab = tab;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        String render(String activeId) {
            boolean active = id.equals(activeId);
            StringBuilder html = new StringBuilder("<a class='sk-nav-item" + (active ? " active" : "") + "'");
            html.append(" href='").append(HtmlEscapeUtils.escape(href)).append("'");
            html.append(" data-sk-nav='").append(HtmlEscapeUtils.escape(id)).append("'");
            if (tab != null) {
                html.append(" data-sk-tab='").append(HtmlEscapeUtils.escape(tab)).append("'");
                html.append(" data-sk-subtitle='").append(HtmlEscapeUtils.escape(subtitle == null ? "" : subtitle)).append("'");
                if (subtitleLink != null) {
                    html.append(" data-sk-subtitle-link='").append(HtmlEscapeUtils.escape(subtitleLink)).append("'");
                }
            }
            if (active) {
                html.append(" aria-current='page'");
            }
            html.append(">");
            html.append("<span class='sk-nav-icon'>").append(icon(icon)).append("</span>");
            html.append("<span class='sk-nav-label'>").append(HtmlEscapeUtils.escape(label)).append("</span>");
            html.append("</a>\n");
            return html.toString();
        }

        private static String icon(String name) {
            return icon(name, 16);
        }

        static String icon(String name, int size) {
            String paths = name == null ? null : ICONS.get(name);
            if (paths == null) {
                return "";
            }
            return "<svg width='" + size + "' height='" + size + "' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' "
                    + "stroke-linecap='round' stroke-linejoin='round'>" + paths + "</svg>";
        }
    }
}
