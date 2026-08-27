package nl.obren.sokrates.sourcecode.githistory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorCommit {
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    private String date = "";
    private String authorEmail = "";
    private String userName = "";

    private int fileUpdatesCount = 1;

    // Lines added/deleted across all files touched by this commit. Summed from the per-file
    // FileUpdate churn columns; 0 for history files without churn data.
    private int linesAdded = 0;
    private int linesDeleted = 0;

    private boolean bot = false;

    // Co-authors from the commit's trailers (see CoAuthorsConfig); empty for histories without
    // the git-commit-trailers.txt sidecar.
    private List<CoAuthor> coAuthors = new ArrayList<>();

    public AuthorCommit() {
    }

    public AuthorCommit(String date, String authorEmail, String userName, boolean bot) {
        this.date = date;
        this.authorEmail = authorEmail;
        this.userName = userName;
        this.bot = bot;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JsonIgnore
    public String getYear() {
        return getDate().length() >= 4 ? getDate().substring(0, 4) : "";
    }

    @JsonIgnore
    public String getMonth() {
        return getDate().length() >= 7 ? getDate().substring(0, 7) : "";
    }

    @JsonIgnore
    public Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        if (this.date.length() >= 10) {
            String string = date.substring(0, 10);
            String elements[] = string.split("-");
            if (elements.length == 3) {
                if (StringUtils.isNumeric(elements[0]) && StringUtils.isNumeric(elements[1]) && StringUtils.isNumeric(elements[2])) {
                    calendar.set(Calendar.YEAR, Integer.parseInt(elements[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(elements[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(elements[2]));

                    return calendar;
                }
            }
        }
        return null;
    }

    public String getWeekOfYear() {
        Calendar calendar = getCalendar();

        if (calendar != null) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, -1);
            }
            return new SimpleDateFormat(ISO_DATE_FORMAT).format(calendar.getTime());
        }

        return "";
    }

    public List<CoAuthor> getCoAuthors() {
        return coAuthors;
    }

    public void setCoAuthors(List<CoAuthor> coAuthors) {
        this.coAuthors = coAuthors == null ? new ArrayList<>() : coAuthors;
    }

    @JsonIgnore
    public boolean hasAiCoAuthor() {
        return coAuthors.stream().anyMatch(CoAuthor::isAi);
    }

    /**
     * Distinct AI agent names among the co-authors (e.g. "Claude Code"), in trailer order.
     */
    @JsonIgnore
    public List<String> getAiAgents() {
        return coAuthors.stream().filter(CoAuthor::isAi).map(CoAuthor::getAgent).distinct().collect(Collectors.toList());
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public int getFileUpdatesCount() {
        return fileUpdatesCount;
    }

    public void setFileUpdatesCount(int fileUpdatesCount) {
        this.fileUpdatesCount = fileUpdatesCount;
    }

    @JsonIgnore
    public void incrementFileUpdatesCount() {
        this.fileUpdatesCount += 1;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public void setLinesDeleted(int linesDeleted) {
        this.linesDeleted = linesDeleted;
    }

    @JsonIgnore
    public void addChurn(int added, int deleted) {
        this.linesAdded += added;
        this.linesDeleted += deleted;
    }
}
