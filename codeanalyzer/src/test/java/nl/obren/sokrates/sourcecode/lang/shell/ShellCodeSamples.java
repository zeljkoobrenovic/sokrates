/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.shell;

public class ShellCodeSamples {
    protected final static String SAMPLE_1 = "#!/bin/sh\n" +
            "# This is a comment!\n" +
            "echo Hello World        # This is a comment, too!";
    protected final static String SAMPLE_1_CLEANED = "echo Hello World        ";
    protected final static String SAMPLE_1_CLEANED_FOR_DUPLICATION = "echo Hello World";
}
