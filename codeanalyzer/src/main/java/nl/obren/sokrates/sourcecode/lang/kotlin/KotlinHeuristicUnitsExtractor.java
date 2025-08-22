/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.kotlin;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

import java.util.List;

public class KotlinHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    public KotlinHeuristicUnitsExtractor() {
        super.setExtractRecursively(true);
    }

    @Override
    public boolean isUnitSignature(String line) {
        return isFunction(line) || isInitBlock(line);
    }

    private boolean isFunction(String line) {
        String idRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";
        String prefixes = "override |open |protected |public | private |suspend ";
        return !line.contains(";") && !line.contains("=") && (RegexUtils.matchesEntirely("[ ]*(" + prefixes + ")*[ ]*fun[ ]*" + idRegex + "[(].*", line));
    }

    private boolean isInitBlock(String line) {
        return RegexUtils.matchesEntirely("[ ]*init[ ]*{[ ]*", line);
    }

    public List<nl.obren.sokrates.sourcecode.units.UnitInfo> extractUnits(nl.obren.sokrates.sourcecode.SourceFile sourceFile) {
        // Get the basic unit extraction from parent
        List<nl.obren.sokrates.sourcecode.units.UnitInfo> units = super.extractUnits(sourceFile);
        
        // Post-process to adjust unit LOC for interface methods
        return adjustUnitLocForInterfaceMethods(units, sourceFile);
    }
    
    private List<nl.obren.sokrates.sourcecode.units.UnitInfo> adjustUnitLocForInterfaceMethods(
            List<nl.obren.sokrates.sourcecode.units.UnitInfo> units, 
            nl.obren.sokrates.sourcecode.SourceFile sourceFile) {
        
        List<String> lines = sourceFile.getLines();
        List<nl.obren.sokrates.sourcecode.units.UnitInfo> adjustedUnits = new java.util.ArrayList<>();
        
        for (nl.obren.sokrates.sourcecode.units.UnitInfo unit : units) {
            if (isInterfaceMethodUnit(unit, lines)) {
                // Create a new unit with 0 LOC for interface methods
                nl.obren.sokrates.sourcecode.units.UnitInfo interfaceUnit = new nl.obren.sokrates.sourcecode.units.UnitInfo();
                interfaceUnit.setShortName(unit.getShortName());
                interfaceUnit.setStartLine(unit.getStartLine());
                interfaceUnit.setEndLine(unit.getStartLine()); // Same as start line
                interfaceUnit.setSourceFile(unit.getSourceFile());
                interfaceUnit.setLinesOfCode(0); // Interface methods have 0 LOC
                interfaceUnit.setMcCabeIndex(1); // Default complexity
                interfaceUnit.setNumberOfParameters(unit.getNumberOfParameters());
                interfaceUnit.setBody("");
                interfaceUnit.setCleanedBody("");
                adjustedUnits.add(interfaceUnit);
            } else {
                adjustedUnits.add(unit); // Keep regular methods unchanged
            }
        }
        
        return adjustedUnits;
    }
    
    private boolean isInterfaceMethodUnit(nl.obren.sokrates.sourcecode.units.UnitInfo unit, List<String> lines) {
        // Check if this unit represents an interface method by examining the source lines
        int startLine = unit.getStartLine() - 1; // Convert to 0-based index
        if (startLine >= 0 && startLine < lines.size()) {
            // Look for the function signature and check if it has no opening brace
            for (int i = startLine; i < Math.min(lines.size(), startLine + unit.getLinesOfCode()); i++) {
                String line = lines.get(i).trim();
                if (line.contains("fun ")) {
                    boolean hasImpl = hasImplementation(lines, i);
                    return !hasImpl;
                }
            }
        }
        return false;
    }
    
    private boolean hasImplementation(List<String> lines, int functionLineIndex) {
        // Check if this function has an implementation (opening brace)
        String currentLine = lines.get(functionLineIndex).trim();
        
        // If the opening brace is on the same line as the function signature
        if (currentLine.contains("{")) {
            return true;
        }
        
        // Look at the next few lines for an opening brace
        for (int i = functionLineIndex + 1; i < Math.min(lines.size(), functionLineIndex + 5); i++) {
            String line = lines.get(i).trim();
            
            // Skip empty lines and annotations
            if (line.isEmpty() || line.startsWith("@")) {
                continue;
            }
            
            // If we encounter another function signature or end of interface, stop looking
            if (line.contains("fun ") || line.equals("}")) {
                break;
            }
            
            // Found opening brace
            if (line.contains("{")) {
                return true;
            }
        }
        
        return false; // No implementation found
    }

}
