/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExtensionGroupTest {
    @Test
    public void testToString() throws Exception {
        assertEquals(new ExtensionGroup("java").toString(), "java: 0 files");
        ExtensionGroup extensionGroup = new ExtensionGroup("js");
        extensionGroup.setNumberOfFiles(50);
        extensionGroup.setTotalSizeInBytes(1000);
        assertEquals(extensionGroup.toString(), "js: 50 files");
    }

}
