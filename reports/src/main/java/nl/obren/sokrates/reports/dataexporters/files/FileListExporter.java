/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.files;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.common.utils.BasicColorInfo;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.ArrayList;
import java.util.List;

public class FileListExporter {
    private List<SourceFile> files;

    public FileListExporter(List<SourceFile> files) {
        this.files = files;
    }

    public List<FileExportInfo> getAllFilesData() {
        List<FileExportInfo> fileExportInfos = new ArrayList<>();

        files.forEach(sourceFile -> {
            fileExportInfos.add(FileExportInfo.getInstance(sourceFile));
        });

        return fileExportInfos;
    }

    public String getJson() throws JsonProcessingException {
        return new JsonGenerator().generate(getAllFilesData());
    }

    public void export3D() {
        List<Unit3D> units = new ArrayList<>();
        files.forEach(file -> {
            SourceFileSizeDistribution sourceFileSizeDistribution = new SourceFileSizeDistribution();
            BasicColorInfo color = getRiskProfileColor(sourceFileSizeDistribution, file.getLinesOfCode());
            units.add(new Unit3D(file.getFile().getPath(), file.getLinesOfCode(), color));
        });
        new X3DomExporter("A 3D View of All Files", "Each block is one file. The height of the block represents the file relative size in lines of code. The color of the file represents its unit size category (green=0-200, yellow=201-500, orange=501-1000, red=1001+).").export(units, false, 50);
    }

    public BasicColorInfo getRiskProfileColor(SourceFileSizeDistribution distribution, int linesOfCode) {
        if (linesOfCode <= distribution.getMediumRiskThreshold()) {
            return new BasicColorInfo(0.0, 1.0, 0.0);
        } else if (linesOfCode <= distribution.getHighRiskThreshold()) {
            return new BasicColorInfo(1.0, 1.0, 0.5);
        } else if (linesOfCode <= distribution.getVeryHighRiskThreshold()) {
            return new BasicColorInfo(1.0, 0.65, 0.0);
        } else {
            return new BasicColorInfo(1.0, 0.0, 0.0);
        }
    }

}
