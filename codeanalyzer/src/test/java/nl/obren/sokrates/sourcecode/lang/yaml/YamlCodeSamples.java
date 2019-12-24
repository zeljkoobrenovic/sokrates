/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.yaml;

public class YamlCodeSamples {
    protected final static String SAMPLE_1 = "key: #comment 1\n" +
            "   - value line 1\n" +
            "   #comment 2\n" +
            "   - value line 2\n" +
            "   #comment 3\n" +
            "   - value line 3";
    protected final static String SAMPLE_1_CLEANED = "key: \n" +
            "   - value line 1\n" +
            "   - value line 2\n" +
            "   - value line 3";
    protected final static String SAMPLE_1_CLEANED_FOR_DUPLICATION = "key:\n" +
            "- value line 1\n" +
            "- value line 2\n" +
            "- value line 3";
}
