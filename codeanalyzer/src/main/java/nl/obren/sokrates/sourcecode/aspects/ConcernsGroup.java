/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import java.util.ArrayList;
import java.util.List;

public class ConcernsGroup {
    private String name = "";
    private List<Concern> concerns = new ArrayList<>();
    private List<MetaRule> metaConcerns = new ArrayList<>();

    public ConcernsGroup() {
    }

    public ConcernsGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Concern> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<Concern> concerns) {
        this.concerns = concerns;
    }

    public List<MetaRule> getMetaConcerns() {
        return metaConcerns;
    }

    public void setMetaConcerns(List<MetaRule> metaConcerns) {
        this.metaConcerns = metaConcerns;
    }
}
