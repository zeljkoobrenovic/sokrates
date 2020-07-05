/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.catalogs;

import nl.obren.sokrates.sourcecode.aspects.Concern;
import nl.obren.sokrates.sourcecode.aspects.ConcernsGroup;

import java.util.List;

public class StandardSecurityAspects extends ConcernsGroup {

    public StandardSecurityAspects() {
        super("security");

        List<Concern> concerns = getConcerns();

        concerns.add(new Concern("authentication"));
        concerns.add(new Concern("session management"));
        concerns.add(new Concern("access control"));
        concerns.add(new Concern("input/output validation"));
        concerns.add(new Concern("cryptography"));
        concerns.add(new Concern("error logging"));
        concerns.add(new Concern("data protection"));
        concerns.add(new Concern("communications"));
        concerns.add(new Concern("file operations"));
    }

}
