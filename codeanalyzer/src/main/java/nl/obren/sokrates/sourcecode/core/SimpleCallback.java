/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

public interface SimpleCallback<P,R> {
    public R call(P param);
}
