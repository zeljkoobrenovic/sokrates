/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.catalogs;

import nl.obren.sokrates.sourcecode.aspects.CrossCuttingConcern;
import nl.obren.sokrates.sourcecode.aspects.CrossCuttingConcernsGroup;

import java.util.List;

public class StandardSecurityAspects extends CrossCuttingConcernsGroup {

    public StandardSecurityAspects() {
        super("security");

        List<CrossCuttingConcern> concerns = getConcerns();

        concerns.add(new CrossCuttingConcern("authentication"));
        concerns.add(new CrossCuttingConcern("session management"));
        concerns.add(new CrossCuttingConcern("access control"));
        concerns.add(new CrossCuttingConcern("input/output validation"));
        concerns.add(new CrossCuttingConcern("cryptography"));
        concerns.add(new CrossCuttingConcern("error logging"));
        concerns.add(new CrossCuttingConcern("data protection"));
        concerns.add(new CrossCuttingConcern("communications"));
        concerns.add(new CrossCuttingConcern("file operations"));
    }

}
