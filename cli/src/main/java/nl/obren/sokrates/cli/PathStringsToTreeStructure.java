package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

public class PathStringsToTreeStructure {
    public static DirectoryNode createDirectoryTree(final List<SourceFile> list) {
        DirectoryNode treeRootNode = null;
        for (final SourceFile sourceFile : list) {
            final String path = "ROOT/" + sourceFile.getRelativePath();
            final String[] pathElements = path.split("/");
            DirectoryNode movingNode = null;
            for (final String pathElement : pathElements) {
                movingNode = new DirectoryNode(movingNode, pathElement);
                if (pathElement == pathElements[pathElements.length - 1]) movingNode.setSourceFile(sourceFile);
            }

            if (treeRootNode == null) {
                treeRootNode = movingNode.getRoot();
            } else {
                treeRootNode.merge(movingNode.getRoot());
            }
        }

        return treeRootNode;
    }

    public static String getColor(Thresholds thresholds, Palette palette, int size) {
        List<String> colors = palette.getColors();
        if (colors.size() < 5) return "";

        if (size <= thresholds.getLow()) return colors.get(4);
        if (size <= thresholds.getMedium()) return colors.get(3);
        if (size <= thresholds.getHigh()) return colors.get(2);
        if (size <= thresholds.getVeryHigh()) return colors.get(1);

        return colors.get(0);
    }

}

class DirectoryNode {

    private final Set<DirectoryNode> children = new LinkedHashSet<>();

    private final String value;

    private final DirectoryNode parent;
    private SourceFile sourceFile;

    public DirectoryNode(final DirectoryNode parent, final String value) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.children.add(this);
        }

        this.value = value;
    }

    public Set<DirectoryNode> getChildren() {
        return this.children;
    }

    public String getValue() {
        return this.value;
    }

    public DirectoryNode getParent() {
        return this.parent;
    }

    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    public DirectoryNode getRoot() {
        return this.parent == null ? this : this.parent.getRoot();
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void merge(final DirectoryNode that) {
        if (!this.value.equals(that.value)) {
            return;
        } else if (this.children.isEmpty()) {
            this.children.addAll(that.children);
            return;
        }

        final DirectoryNode[] thisChildren = this.children
                .toArray(new DirectoryNode[this.children.size()]);
        for (final DirectoryNode thisChild : thisChildren) {
            for (final DirectoryNode thatChild : that.children) {
                if (thisChild.value.equals(thatChild.value)) {
                    thisChild.merge(thatChild);
                } else if (!this.children.contains(thatChild)) {
                    this.children.add(thatChild);
                }
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DirectoryNode that = (DirectoryNode) o;
        return Objects.equals(this.value, that.value)
                && Objects.equals(this.parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.parent);
    }


    @Override
    public String toString() {
        return "{" + "value='" + this.value + '\'' + ", children=" + this.children + '}';
    }

    public List<VisualizationItem> toVisualizationItems() {
        List<VisualizationItem> items = new ArrayList<>();

        children.forEach(child -> {
            String name = child.value;
            int size = child.getSourceFile() != null && child.getSourceFile().getRelativePath().endsWith(name) ? child.getSourceFile().getLinesOfCode() : 0;
            VisualizationItem item = new VisualizationItem(name, size);
            items.add(item);
            item.getChildren().addAll(child.toVisualizationItems());
        });

        return items;
    }

    public List<VisualizationItem> toVisualizationCommitItems(int daysAgo) {
        List<VisualizationItem> items = new ArrayList<>();

        children.forEach(child -> {
            String name = child.value;
            int size = child.getSourceFile() != null && child.getSourceFile().getRelativePath().endsWith(name) && child.getSourceFile().getFileModificationHistory() != null
                    ? (int) child.getSourceFile().getFileModificationHistory().getCommits().stream().filter(c -> DateUtils.isCommittedLessThanDaysAgo(c.getDate(), daysAgo)).count()
                    : 0;
            VisualizationItem item = new VisualizationItem(name + (size > 0 ? " (" + size + ")" : ""), size);
            List<VisualizationItem> children = child.toVisualizationCommitItems(daysAgo);
            if (size > 0 || children.size() > 0) {
                if (size == 1) {
                    item.setColor("grey");
                }
                items.add(item);
                item.getChildren().addAll(children);
            }
        });

        return items;
    }

    public List<VisualizationItem> toVisualizationContributorItems(int daysAgo) {
        List<VisualizationItem> items = new ArrayList<>();

        children.forEach(child -> {
            String name = child.value;
            int size;
            if (child.getSourceFile() != null && child.getSourceFile().getRelativePath().endsWith(name) && child.getSourceFile().getFileModificationHistory() != null) {
                Stream<CommitInfo> commitInfoStream = child.getSourceFile().getFileModificationHistory().getCommits().stream().filter(c -> DateUtils.isCommittedLessThanDaysAgo(c.getDate(), daysAgo));
                Set<String> emails = new HashSet<>();
                commitInfoStream.forEach(commitInfo -> emails.add(commitInfo.getEmail()));
                size = emails.size();
            } else {
                size = 0;
            }
            VisualizationItem item = new VisualizationItem(name + (size > 0 ? " (" + size + ")" : ""), size);
            List<VisualizationItem> children = child.toVisualizationContributorItems(daysAgo);
            if (size > 0 || children.size() > 0) {
                if (size == 1) {
                    item.setColor("grey");
                }
                items.add(item);
                item.getChildren().addAll(children);
            }
        });

        return items;
    }

    public List<VisualizationItem> toVisualizationRiskColoringItems(Thresholds thresholds, Palette palette, SourceFileValueExtractor valueExtractor) {
        List<VisualizationItem> items = new ArrayList<>();

        children.forEach(child -> {
            String name = child.value;
            int value;
            if (child.getSourceFile() != null && child.getSourceFile().getRelativePath().endsWith(name)) {
                value = valueExtractor.getValue(child.getSourceFile());
            } else {
                value = 0;
            }
            VisualizationItem item = new VisualizationItem(name + (value > 0 ? " (" + value + ")" : ""), value);
            List<VisualizationItem> children = child.toVisualizationRiskColoringItems(thresholds, palette, valueExtractor);
            if (value > 0 || children.size() > 0) {
                item.setColor(PathStringsToTreeStructure.getColor(thresholds, palette, value));
                items.add(item);
                item.getChildren().addAll(children);
            }
        });

        return items;
    }

    public interface SourceFileValueExtractor {
        int getValue(SourceFile sourceFile);
    }
}