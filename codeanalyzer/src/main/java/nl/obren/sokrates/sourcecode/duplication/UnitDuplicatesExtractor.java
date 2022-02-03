package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitDuplicatesExtractor {
    private static final Log LOG = LogFactory.getLog(UnitDuplicatesExtractor.class);
    private Map<String, String> cleanedUnitsMap = new HashMap<>();

    public List<DuplicationInstance> findDuplicatedUnits(List<UnitInfo> units, int threshold) {
        List<DuplicationInstance> duplicates = new ArrayList<>();
        Map<String, DuplicationInstance> map = new HashMap<>();

        units.forEach(unit1 -> {
            String bodyForDuplication1 = getLinesCleanedForDuplication(unit1);
            int size1 = StringUtils.countMatches(bodyForDuplication1, "\n");
            if (size1 >= threshold) {
                DuplicatedFileBlock block1 = getDuplicatedFileBlock(unit1);
                if (map.containsKey(bodyForDuplication1)) {
                    DuplicationInstance instance = map.get(bodyForDuplication1);
                    if (!containsBlock(instance, block1)) {
                        instance.getDuplicatedFileBlocks().add(block1);
                    }
                } else {
                    DuplicationInstance instance = new DuplicationInstance();
                    instance.setDisplayContent(unit1.getCleanedBody());
                    instance.setBlockSize(size1);

                    instance.getDuplicatedFileBlocks().add(block1);
                    duplicates.add(instance);
                    map.put(bodyForDuplication1, instance);
                }
            }
        });

        return duplicates.stream().filter(d -> d.getDuplicatedFileBlocks().size() > 1).collect(Collectors.toList());
    }

    public String getLinesCleanedForDuplication(UnitInfo unit) {
        String key = unit.getSourceFile().getRelativePath() + " " + unit.getStartLine() + ":" + unit.getEndLine();
        if (cleanedUnitsMap.containsKey(key)) {
            return cleanedUnitsMap.get(key);
        }
        List<String> lines = new ArrayList<>();
        if (unit != null) {
            String cleanedBody = unit.getCleanedBody();
            if (cleanedBody != null) {
                String linesSplit[] = cleanedBody.split("\n");
                for (String line : linesSplit) {
                    if (StringUtils.isNotBlank(line)) {
                        lines.add(line.trim());
                    }
                }
            }
        }

        String body = lines.stream().skip(1).collect(Collectors.joining("\n"));
        cleanedUnitsMap.put(key, body);

        return body;
    }

    public DuplicatedFileBlock getDuplicatedFileBlock(UnitInfo unit) {
        DuplicatedFileBlock block1 = new DuplicatedFileBlock();
        block1.setSourceFile(unit.getSourceFile());
        block1.setStartLine(unit.getStartLine());
        block1.setEndLine(unit.getEndLine());
        block1.setCleanedStartLine(unit.getStartLine());
        block1.setCleanedEndLine(unit.getEndLine());
        block1.setSourceFileCleanedLinesOfCode(0);
        return block1;
    }

    private boolean containsBlock(DuplicationInstance instance, DuplicatedFileBlock block) {
        for (DuplicatedFileBlock duplicatedFileBlock : instance.getDuplicatedFileBlocks()) {
            if (duplicatedFileBlock.getSourceFile().getRelativePath().equalsIgnoreCase(block.getSourceFile().getRelativePath())) {
                return true;
            }
        }

        return false;
    }
}
