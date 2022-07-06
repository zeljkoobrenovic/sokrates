package nl.obren.sokrates.common.utils;

public class ProcessingTimes {
    private String processing = "";
    private long startMs;
    private long endMs;

    public ProcessingTimes(String processing) {
        this.processing = processing;
        this.start();
    }

    public void start() {
        this.startMs = System.currentTimeMillis();
        this.endMs = 0;
    }

    public void end() {
        this.endMs = System.currentTimeMillis();
    }

    public long getDurationMs() {
        return endMs >= startMs ? endMs - startMs : System.currentTimeMillis() - startMs;
    }

    public String getProcessing() {
        return processing;
    }

    public void setProcessing(String processing) {
        this.processing = processing;
    }

    public long getStartMs() {
        return startMs;
    }

    public void setStartMs(long startMs) {
        this.startMs = startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public void setEndMs(long endMs) {
        this.endMs = endMs;
    }
}
