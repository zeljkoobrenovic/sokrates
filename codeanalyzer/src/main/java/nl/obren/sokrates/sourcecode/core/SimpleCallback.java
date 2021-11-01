/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

public interface SimpleCallback<P,R> {
    public R call(P param);
}
