/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.duplication;

import nl.obren.sokrates.reports.dataexporters.files.FileExportInfo;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;

import java.util.ArrayList;
import java.util.List;

public class DuplicationExporter {
    private List<DuplicationInstance> duplicates = new ArrayList<>();

    public DuplicationExporter(List<DuplicationInstance> duplicates) {
        this.duplicates = duplicates;
    }

    public DuplicationExportInfo getDuplicationExportInfo() {
        DuplicationExportInfo duplicationExportInfo = new DuplicationExportInfo();
        duplicationExportInfo.setTitle("Duplication");

        duplicationExportInfo.setDuplicates(getDuplicatesExportInfo());

        return duplicationExportInfo;
    }

    public List<DuplicateExportInfo> getDuplicatesExportInfo() {
        List<DuplicateExportInfo> export = new ArrayList<>();

        duplicates.forEach(duplicationInstance -> {
            DuplicateExportInfo exportInfo = new DuplicateExportInfo();

            exportInfo.setBlockSize(duplicationInstance.getBlockSize());

            duplicationInstance.getDuplicatedFileBlocks().forEach(fileBlock -> {
                DuplicateFileBlockExportInfo fileBlockExportInfo = new DuplicateFileBlockExportInfo();

                fileBlockExportInfo.setFile(FileExportInfo.getInstance(fileBlock.getSourceFile()));
                fileBlockExportInfo.setSourceFileCleanedLinesOfCode(fileBlock.getSourceFileCleanedLinesOfCode());
                fileBlockExportInfo.setStartLine(fileBlock.getStartLine());
                fileBlockExportInfo.setEndLine(fileBlock.getEndLine());
                fileBlockExportInfo.setCleanedStartLine(fileBlock.getCleanedStartLine());
                fileBlockExportInfo.setCleanedEndLine(fileBlock.getCleanedEndLine());

                exportInfo.getDuplicatedFileBlocks().add(fileBlockExportInfo);
            });

            export.add(exportInfo);
        });

        return export;
    }
}
