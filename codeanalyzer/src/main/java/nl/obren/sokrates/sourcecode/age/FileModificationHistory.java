/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.age;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FileModificationHistory {
    private List<String> dates = new ArrayList<>();
    private String path = "";

    public FileModificationHistory() {
    }

    public FileModificationHistory(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public String getOldestDate() {
        sortOldestFirst();
        return dates.get(0);
    }

    public String getLatestDate() {
        sortOldestFirst();
        return dates.get(dates.size() - 1);
    }

    public void sortOldestFirst() {
        Collections.sort(dates);
    }

    public int daysSinceFirstUpdate() {
        return days(getOldestDate());
    }

    public int daysSinceLatestUpdate() {
        return days(getLatestDate());
    }

    private int days(String dateString) {
        Date today = new Date();

        try {
            Date fileDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateString.substring(0, 10));
            return 1 + (int) TimeUnit.DAYS.convert(Math.abs(today.getTime() - fileDate.getTime()), TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
