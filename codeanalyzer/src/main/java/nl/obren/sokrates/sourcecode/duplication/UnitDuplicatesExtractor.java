package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.duplication.impl.Block;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitDuplicatesExtractor {
    public List<DuplicationInstance> findDuplicatedUnits(List<UnitInfo> units, int threshold) {
        List<DuplicationInstance> duplicates = new ArrayList<>();
        Map<String, DuplicationInstance> map = new HashMap<>();

        units.forEach(unit1 -> {
            units.forEach(unit2 -> {
                List<String> lines1 = getLinesCleanedForDuplication(unit1);
                int size = lines1.size() - 1;
                if (size >= threshold && unit1 != unit2) {
                    List<String> lines2 = getLinesCleanedForDuplication(unit2);
                    String bodyForDuplication1 = lines1.stream().skip(1).collect(Collectors.joining("\n"));
                    String bodyForDuplication2 = lines2.stream().skip(1).collect(Collectors.joining("\n"));
                    if (bodyForDuplication1.equalsIgnoreCase(bodyForDuplication2)) {
                        DuplicatedFileBlock block1 = getDuplicatedFileBlock(unit1);
                        DuplicatedFileBlock block2 = getDuplicatedFileBlock(unit2);
                        if (map.containsKey(bodyForDuplication1)) {
                            DuplicationInstance instance = map.get(bodyForDuplication1);
                            if (!containsBlock(instance, block1)) {
                                instance.getDuplicatedFileBlocks().add(block1);
                            }
                            if (!containsBlock(instance, block2)) {
                                instance.getDuplicatedFileBlocks().add(block2);
                            }
                        } else {
                            DuplicationInstance instance = new DuplicationInstance();
                            instance.setDisplayContent(unit1.getCleanedBody());
                            instance.setBlockSize(size);

                            instance.getDuplicatedFileBlocks().add(block1);
                            instance.getDuplicatedFileBlocks().add(block2);
                            duplicates.add(instance);
                            map.put(bodyForDuplication1, instance);
                        }
                    }
                }
            });
        });

        return duplicates;
    }

    public List<String> getLinesCleanedForDuplication(UnitInfo unit1) {
        String linesSplit[] = unit1.getCleanedBody().split("\n");
        List<String> lines = new ArrayList<>();
        for (String line : linesSplit) {
            if (StringUtils.isNotBlank(line)) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    public DuplicatedFileBlock getDuplicatedFileBlock(UnitInfo unit) {
        DuplicatedFileBlock block1 = new DuplicatedFileBlock();
        block1.setSourceFile(unit.getSourceFile());
        block1.setStartLine(unit.getStartLine());
        block1.setEndLine(unit.getEndLine());
        block1.setCleanedStartLine(unit.getStartLine());
        block1.setCleanedEndLine(unit.getEndLine());
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
