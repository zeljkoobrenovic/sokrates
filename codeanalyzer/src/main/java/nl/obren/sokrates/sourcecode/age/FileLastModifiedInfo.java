/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.age;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class FileLastModifiedInfo {
    private String lastModifiedDateTime = "";
    private String path = "";

    public FileLastModifiedInfo() {
    }

    public FileLastModifiedInfo(String lastModifiedDateTime, String path) {
        this.lastModifiedDateTime = lastModifiedDateTime;
        this.path = path;
    }

    public String getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(String lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int ageInDays() {
        Date today = new Date();

        try {
            Date fileDate = new SimpleDateFormat("yyyy-MM-dd").parse(lastModifiedDateTime.substring(0, 10));
            return 1 + (int) TimeUnit.DAYS.convert(Math.abs(today.getTime() - fileDate.getTime()), TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
