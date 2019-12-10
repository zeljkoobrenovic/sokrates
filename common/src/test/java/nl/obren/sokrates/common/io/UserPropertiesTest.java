/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.io;

import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class UserPropertiesTest {
    @Test
    public void addAndGetListProperty() throws Exception {
        UserProperties userProperties = UserProperties.getInstance("sokrates");
        userProperties.setReadOnly(true);

        File tempFile1 = File.createTempFile("test", "test");

        assertEquals(userProperties.getFileListProperty("testFileList").size(), 0);

        userProperties.addToListProperty("testFileList", tempFile1);

        assertEquals(userProperties.getFileListProperty("testFileList").size(), 1);
        assertEquals(userProperties.getFileListProperty("testFileList").get(0).getPath(), tempFile1.getPath());

        File tempFile2 = File.createTempFile("test", "test");
        File tempFile3 = File.createTempFile("test", "test");
        userProperties.addToListProperty("testFileList", tempFile2);
        userProperties.addToListProperty("testFileList", tempFile3);

        assertEquals(userProperties.getFileListProperty("testFileList").size(), 3);

        assertEquals(userProperties.getFileListProperty("testFileList").get(0).getPath(), tempFile3.getPath());
        assertEquals(userProperties.getFileListProperty("testFileList").get(1).getPath(), tempFile2.getPath());
        assertEquals(userProperties.getFileListProperty("testFileList").get(2).getPath(), tempFile1.getPath());
    }
}
