/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;

import java.util.ArrayList;
import java.util.List;

public class ConcernConventions {
    private List<Convention> todos = new ArrayList<>();
    private List<Convention> exceptionHandling = new ArrayList<>();
    private List<Convention> logging = new ArrayList<>();
    private List<Convention> printingToConsole = new ArrayList<>();

    private List<Convention> configurationPoints = new ArrayList<>();
    private List<Convention> databaseAccess = new ArrayList<>();
    private List<Convention> fileOperations = new ArrayList<>();
    private List<Convention> networkOperations = new ArrayList<>();

    private List<Convention> synchronization = new ArrayList<>();

    private List<Convention> authentication = new ArrayList<>();
    private List<Convention> authorization = new ArrayList<>();
    private List<Convention> sessionManagement = new ArrayList<>();
    private List<Convention> encryption = new ArrayList<>();
    private List<Convention> securedCommunication = new ArrayList<>();

    private List<Convention> lowLevelMemoryOperations = new ArrayList<>();
    private List<Convention> lowLevelStringManipulation = new ArrayList<>();

    public ConcernConventions() {
        exceptionHandling.add(new Convention("", ".*catch.*", ""));
        exceptionHandling.add(new Convention("", ".*err( |\t)*:=", ""));

        todos.add(new Convention("", ".*(TODO|FIXME)( |:|\t).*", ""));

        logging.add(new Convention("", "(?i).*log[.].*", ""));
        logging.add(new Convention("", "(?i).*logger[.].*", ""));

        printingToConsole.add(new Convention("", ".*System.out.print.*", ""));
    }

    public void addConventions(List<SourceFileFilter> sourceFileFilters, List<SourceFile> sourceFiles) {
        ConventionUtils.addConventions(exceptionHandling, sourceFileFilters, sourceFiles);
    }

}
