package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.filehistory.DateUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitHistoryPerExtensionUtils {
    private Map<String, CommitsPerExtension> map = new HashMap<>();
    private Map<String, List<String>> emails = new HashMap<>();
    private Map<String, List<String>> emails30Days = new HashMap<>();
    private Map<String, List<String>> emails90Days = new HashMap<>();
    private Map<String, List<String>> paths = new HashMap<>();
    private Map<String, List<String>> paths30Days = new HashMap<>();
    private Map<String, List<String>> paths90Days = new HashMap<>();

    public List<CommitsPerExtension> getCommitsPerExtensions(File file) {
        GitHistoryUtils.getHistoryFromFile(file).forEach(fileUpdate -> {
            String extension = fileUpdate.getExtension();
            String path = fileUpdate.getPath();
            String email = fileUpdate.getAuthorEmail();
            String date = fileUpdate.getDate();
            boolean isLessThan30DaysAgo = DateUtils.isCommittedLessThanDaysAgo(date, 30);
            boolean isLessThan90DaysAgo = DateUtils.isCommittedLessThanDaysAgo(date, 90);

            CommitsPerExtension commitsPerExtension = getCommitsPerExtension(extension);

            updateTotalCounts(extension, path, email, commitsPerExtension);

            if (isLessThan30DaysAgo) {
                update30DaysCounts(extension, path, email, commitsPerExtension);
            }
            if (isLessThan90DaysAgo) {
                update90DaysCounts(extension, path, email, commitsPerExtension);
            }

        });

        ArrayList<CommitsPerExtension> list = new ArrayList<>(map.values());

        Collections.sort(list, (a, b) -> b.getCommitters().size() - a.getCommitters().size());
        Collections.sort(list, (a, b) -> b.getCommitters90Days().size() - a.getCommitters90Days().size());
        Collections.sort(list, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());

        return list;
    }

    public void update90DaysCounts(String extension, String path, String email, CommitsPerExtension commitsPerExtension) {
        commitsPerExtension.setCommitsCount90Days(commitsPerExtension.getCommitsCount90Days() + 1);

        updateMaps(extension, email, emails90Days);
        updateMaps(extension, path, paths90Days);

        commitsPerExtension.setCommitters90Days(emails90Days.get(extension));
        commitsPerExtension.setFilesCount90Days(paths90Days.get(extension).size());
    }

    public void update30DaysCounts(String extension, String path, String email, CommitsPerExtension commitsPerExtension) {
        commitsPerExtension.setCommitsCount30Days(commitsPerExtension.getCommitsCount30Days() + 1);

        updateMaps(extension, email, emails30Days);
        updateMaps(extension, path, paths30Days);

        commitsPerExtension.setCommitters30Days(emails30Days.get(extension));
        commitsPerExtension.setFilesCount30Days(paths30Days.get(extension).size());
    }

    public void updateTotalCounts(String extension, String path, String email, CommitsPerExtension commitsPerExtension) {
        commitsPerExtension.setCommitsCount(commitsPerExtension.getCommitsCount() + 1);

        updateMaps(extension, email, emails);
        updateMaps(extension, path, paths);

        commitsPerExtension.setCommitters(emails.get(extension));
        commitsPerExtension.setFilesCount(paths.get(extension).size());
    }

    public CommitsPerExtension getCommitsPerExtension(String extension) {
        CommitsPerExtension commitsPerExtension = map.get(extension);
        if (commitsPerExtension == null) {
            commitsPerExtension = new CommitsPerExtension(extension);
            map.put(extension, commitsPerExtension);
        }
        return commitsPerExtension;
    }

    private static void updateMaps(String extension, String value, Map<String, List<String>> map) {
        List<String> extList = map.get(extension);
        if (extList == null) {
            extList = new ArrayList<>();
            map.put(extension, extList);
        }
        if (!extList.contains(value)) {
            extList.add(value);
        }
    }
}
