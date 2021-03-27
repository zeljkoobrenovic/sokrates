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
}
