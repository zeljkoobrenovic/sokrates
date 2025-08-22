/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.kotlin;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

import java.util.ArrayList;
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
        List<nl.obren.sokrates.sourcecode.units.UnitInfo> adjustedUnits = new ArrayList<>();
        
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
        // Better approach: Check if this unit contains multiple 'fun' keywords
        // Interface methods should have exactly 1 'fun', implemented methods should also have 1 'fun'
        // But if the unit has multiple 'fun' keywords, it means the original extractor 
        // incorrectly included multiple methods due to missing braces (interface case)
        
        int startLine = unit.getStartLine() - 1; // Convert to 0-based index
        int endLine = Math.min(lines.size(), startLine + unit.getLinesOfCode());
        
        if (startLine >= 0 && startLine < lines.size()) {
            int funCount = 0;
            int firstFunLine = -1;
            boolean hasOpeningBrace = false;
            
            // Count 'fun' keywords and check for opening braces
            for (int i = startLine; i < endLine; i++) {
                String line = lines.get(i).trim();
                
                if (line.contains("fun ")) {
                    funCount++;
                    if (firstFunLine == -1) {
                        firstFunLine = i;
                    }
                }
                
                // Check for opening brace (but not in strings/comments)
                if (line.contains("{") && !line.trim().startsWith("//") && !isInString(line, "{")) {
                    hasOpeningBrace = true;
                }
            }
            
            // If we have exactly 1 'fun' and no opening brace, it's an interface method
            if (funCount == 1 && !hasOpeningBrace) {
                return true;
            }
            
            // If we have multiple 'fun' keywords, the first one is likely an interface method
            // that got merged with subsequent methods due to missing braces
            if (funCount > 1) {
                return true; // This unit represents multiple functions, first is interface method
            }
        }
        
        return false;
    }
    
    private boolean isInString(String line, String target) {
        // Simple check to avoid matching braces inside strings
        // This is not perfect but good enough for most cases
        boolean inString = false;
        char quote = 0;
        
        for (int i = 0; i < line.length() - target.length() + 1; i++) {
            char c = line.charAt(i);
            
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                quote = c;
            } else if (inString && c == quote) {
                inString = false;
            } else if (!inString && line.substring(i, i + target.length()).equals(target)) {
                return false; // Found target outside of string
            }
        }
        
        return inString; // Target was inside string if we're still in string context
    }
    

}
