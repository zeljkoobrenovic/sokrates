package nl.obren.sokrates.reports.generators.explorers;

import java.util.ArrayList;
import java.util.List;

/**
 * The payload embedded into ai-cost-estimator.html: the git history reduced to the tasks an AI
 * coding agent would have worked on (see {@link AiCostEstimatorGenerator}). The page applies the
 * token and price priors to the sessions and simulates their uncertainty in the browser.
 */
public class AiCostEstimatorData {
    private List<Author> authors = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();
    private Noise noise = new Noise();
    private int totalCommitsCount = 0;
    private int keptCommitsCount = 0;
    private int analyzedCommitsCount = 0;
    private List<String> ticketPrefixes = new ArrayList<>();

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Noise getNoise() {
        return noise;
    }

    public void setNoise(Noise noise) {
        this.noise = noise;
    }

    public int getTotalCommitsCount() {
        return totalCommitsCount;
    }

    public void setTotalCommitsCount(int totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
    }

    public int getKeptCommitsCount() {
        return keptCommitsCount;
    }

    public void setKeptCommitsCount(int keptCommitsCount) {
        this.keptCommitsCount = keptCommitsCount;
    }

    public int getAnalyzedCommitsCount() {
        return analyzedCommitsCount;
    }

    public void setAnalyzedCommitsCount(int analyzedCommitsCount) {
        this.analyzedCommitsCount = analyzedCommitsCount;
    }

    public List<String> getTicketPrefixes() {
        return ticketPrefixes;
    }

    public void setTicketPrefixes(List<String> ticketPrefixes) {
        this.ticketPrefixes = ticketPrefixes;
    }

    public static class Author {
        private String email = "";
        private String name = "";

        public Author() {
        }

        public Author(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /** One commit of a task: short sha, date and the author's index in the author list. */
    public static class TaskCommit {
        private String sha = "";
        private String date = "";
        private int author = 0;

        public TaskCommit() {
        }

        public TaskCommit(String sha, String date, int author) {
            this.sha = sha;
            this.date = date;
            this.author = author;
        }

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getAuthor() {
            return author;
        }

        public void setAuthor(int author) {
            this.author = author;
        }
    }

    /**
     * A task: commits grouped by ticket, or consecutive commits of one author touching overlapping
     * files. {@code sessions} are the agent sessions it is split into, each
     * {@code [files, readLines, editAdded, editDeleted, newLines]}.
     */
    public static class Task {
        private String start = "";
        private String end = "";
        private String type = "";
        private String ticket = "";
        private List<TaskCommit> commits = new ArrayList<>();
        private int files = 0;
        private int editAdded = 0;
        private int editDeleted = 0;
        private int newLines = 0;
        private int deletedFiles = 0;
        private List<int[]> sessions = new ArrayList<>();

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }

        public List<TaskCommit> getCommits() {
            return commits;
        }

        public void setCommits(List<TaskCommit> commits) {
            this.commits = commits;
        }

        public int getFiles() {
            return files;
        }

        public void setFiles(int files) {
            this.files = files;
        }

        public int getEditAdded() {
            return editAdded;
        }

        public void setEditAdded(int editAdded) {
            this.editAdded = editAdded;
        }

        public int getEditDeleted() {
            return editDeleted;
        }

        public void setEditDeleted(int editDeleted) {
            this.editDeleted = editDeleted;
        }

        public int getNewLines() {
            return newLines;
        }

        public void setNewLines(int newLines) {
            this.newLines = newLines;
        }

        public int getDeletedFiles() {
            return deletedFiles;
        }

        public void setDeletedFiles(int deletedFiles) {
            this.deletedFiles = deletedFiles;
        }

        public List<int[]> getSessions() {
            return sessions;
        }

        public void setSessions(List<int[]> sessions) {
            this.sessions = sessions;
        }

        public int churn() {
            return editAdded + editDeleted + newLines;
        }
    }

    /** What was left out because it would distort the churn. */
    public static class Noise {
        private int botCommits = 0;
        private int massCommits = 0;
        private int emptiedCommits = 0;
        private int lockFileChanges = 0;
        private int vendoredChanges = 0;
        private int generatedChanges = 0;
        private int oversizedChanges = 0;
        private int unscopedChanges = 0;
        private long droppedLines = 0;

        public int getBotCommits() {
            return botCommits;
        }

        public void setBotCommits(int botCommits) {
            this.botCommits = botCommits;
        }

        public int getMassCommits() {
            return massCommits;
        }

        public void setMassCommits(int massCommits) {
            this.massCommits = massCommits;
        }

        public int getEmptiedCommits() {
            return emptiedCommits;
        }

        public void setEmptiedCommits(int emptiedCommits) {
            this.emptiedCommits = emptiedCommits;
        }

        public int getLockFileChanges() {
            return lockFileChanges;
        }

        public void setLockFileChanges(int lockFileChanges) {
            this.lockFileChanges = lockFileChanges;
        }

        public int getVendoredChanges() {
            return vendoredChanges;
        }

        public void setVendoredChanges(int vendoredChanges) {
            this.vendoredChanges = vendoredChanges;
        }

        public int getGeneratedChanges() {
            return generatedChanges;
        }

        public void setGeneratedChanges(int generatedChanges) {
            this.generatedChanges = generatedChanges;
        }

        public int getOversizedChanges() {
            return oversizedChanges;
        }

        public void setOversizedChanges(int oversizedChanges) {
            this.oversizedChanges = oversizedChanges;
        }

        public int getUnscopedChanges() {
            return unscopedChanges;
        }

        public void setUnscopedChanges(int unscopedChanges) {
            this.unscopedChanges = unscopedChanges;
        }

        public long getDroppedLines() {
            return droppedLines;
        }

        public void setDroppedLines(long droppedLines) {
            this.droppedLines = droppedLines;
        }
    }
}
