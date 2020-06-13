/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TrendAnalysisConfig {
    private String referenceAnalysesFolder = "history";
    private boolean saveHistory = false;
    private String frequency = "weekly";
    private int maxReferencePointsForAnalysis = 20;

    public String getHistoryFolder() {
        return referenceAnalysesFolder;
    }

    public void setReferenceAnalysesFolder(String referenceAnalysesFolder) {
        this.referenceAnalysesFolder = referenceAnalysesFolder;
    }


    public boolean isSaveHistory() {
        return saveHistory;
    }

    public void setSaveHistory(boolean saveHistory) {
        this.saveHistory = saveHistory;
    }

    public int getMaxReferencePointsForAnalysis() {
        return maxReferencePointsForAnalysis;
    }

    public void setMaxReferencePointsForAnalysis(int maxReferencePointsForAnalysis) {
        this.maxReferencePointsForAnalysis = maxReferencePointsForAnalysis;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    @JsonIgnore
    public File getHistoryFolder(File configFileFolder) {
        if (new File(referenceAnalysesFolder).exists()) {
            return new File(referenceAnalysesFolder);
        } else {
            File folderRelativeToConfigFile = new File(configFileFolder, referenceAnalysesFolder);
            folderRelativeToConfigFile.mkdirs();
            return folderRelativeToConfigFile;
        }
    }

    @JsonIgnore
    public String getSnapshotFolderName() {
        Date today = new Date();

        String subFolder = "snapshots/";
        
        if (frequency.toLowerCase().startsWith("week")) {
            return subFolder + new SimpleDateFormat("yyyy-ww").format(today);
        } else if (frequency.toLowerCase().startsWith("month")) {
            return subFolder + new SimpleDateFormat("yyyy-MM").format(today);
        } else {
            return subFolder + new SimpleDateFormat("yyyy-MM-dd").format(today);
        }
    }

    @JsonIgnore
    public File getSnapshotFolder(File configFileFolder) {
        File folder = new File(getHistoryFolder(configFileFolder), getSnapshotFolderName());
        folder.mkdirs();

        return folder;
    }

    @JsonIgnore
    public List<ReferenceAnalysisResult> getReferenceAnalyses(File configFileFolder) {
        List<ReferenceAnalysisResult> references = new ArrayList<>();

        for (File snapshotFolder : getSnapshotFolder(configFileFolder).getParentFile().listFiles()) {
            File zipFile = new File(snapshotFolder, "analysisResults.zip");
            if (zipFile.exists()) {
                references.add(new ReferenceAnalysisResult(snapshotFolder.getName(), zipFile));
            }
        }

        Collections.sort(references, (a, b) -> b.getLabel().compareTo(a.getLabel()));

        if (references.size() > maxReferencePointsForAnalysis) {
            references = references.subList(0, maxReferencePointsForAnalysis);
        }

        return references;
    }

}
