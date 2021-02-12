/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.List;

public class Concern extends NamedSourceCodeAspect {
    private List<OperationStatement> textOperations = new ArrayList<>();

    public Concern() {
    }

    public Concern(String name) {
        super(name);
    }

    public List<OperationStatement> getTextOperations() {
        return textOperations;
    }

    public void setTextOperations(List<OperationStatement> textOperations) {
        this.textOperations = textOperations;
    }
}
