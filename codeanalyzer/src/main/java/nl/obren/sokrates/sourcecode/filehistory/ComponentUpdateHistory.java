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
    }

    public void addDate(String date) {
        if (!dates.contains(date)) {
            dates.add(date);
            Collections.sort(dates);
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
        List<SourceFile> sourceFilePaths = dateSourceFileMap.get(date);
        if (sourceFilePaths == null) {
            sourceFilePaths = new ArrayList<>();
            dateSourceFileMap.put(date, sourceFilePaths);
        }

        if (!sourceFilePaths.contains(sourceFile)) {
            sourceFilePaths.add(sourceFile);
        }
    }

}
