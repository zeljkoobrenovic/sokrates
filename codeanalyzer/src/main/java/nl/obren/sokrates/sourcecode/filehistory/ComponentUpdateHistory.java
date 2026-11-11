/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

import java.util.*;

public class ComponentUpdateHistory {
    private NamedSourceCodeAspect component;
    private List<String> dates = new ArrayList<>();
    private Map<String, List<SourceFile>> dateSourceFileMap = new HashMap<>();
    // Distinct, sorted dates kept in a TreeSet so addDate is O(log n) without a contains() scan and
    // a full re-sort on every insert; the dates list mirrors it. Per-date source-file dedup sets.
    private TreeSet<String> datesSet = new TreeSet<>();
    private Map<String, Set<SourceFile>> dateSourceFileSet = new HashMap<>();

    public ComponentUpdateHistory(NamedSourceCodeAspect component) {
        this.component = component;
    }

    public NamedSourceCodeAspect getComponent() {
        return component;
    }

    public void setComponent(NamedSourceCodeAspect component) {
        this.component = component;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
        this.datesSet = new TreeSet<>(dates);
    }

    public void addDate(String date) {
        if (datesSet.add(date)) {
            dates.clear();
            dates.addAll(datesSet);
        }
    }

    public void addDates(List<FileModificationHistory> fileModificationHistories) {
        fileModificationHistories.forEach(history -> addDates(history));
    }

    public void addDates(FileModificationHistory fileModificationHistory) {
        fileModificationHistory.getDates().forEach(date -> {
            SourceFile sourceFile = component.getSourceFileByPath(fileModificationHistory.getPath());
            if (sourceFile != null && sourceFile.isInLogicalComponent(component.getName())) {
                addSourceFileDate(date, sourceFile);
            }
        });
    }

    private void addSourceFileDate(String date, SourceFile sourceFile) {
        addDate(date);
        List<SourceFile> sourceFilePaths = dateSourceFileMap.computeIfAbsent(date, k -> new ArrayList<>());
        Set<SourceFile> seen = dateSourceFileSet.computeIfAbsent(date, k -> new HashSet<>());

        if (seen.add(sourceFile)) {
            sourceFilePaths.add(sourceFile);
        }
    }

}
