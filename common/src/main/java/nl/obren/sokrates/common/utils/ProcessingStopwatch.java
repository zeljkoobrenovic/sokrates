package nl.obren.sokrates.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessingStopwatch {
    private static final Log LOG = LogFactory.getLog(ProcessingStopwatch.class);

    private static List<ProcessingTimes> monitors = new ArrayList<>();
    private static Map<String, ProcessingTimes> monitorsMap = new HashMap<>();
    private static ProcessingTimes referenceTimes = null;

    public static void startAsReference(String processingName) {
        ProcessingTimes times = start(processingName);
        referenceTimes = times;
    }

    public static ProcessingTimes start(String processingName) {
        ProcessingTimes times = new ProcessingTimes(processingName);

        monitors.add(times);
        monitorsMap.put(processingName, times);

        LOG.info("Running " + processingName);

        return times;
    }

    public static void end(String processingName) {
        if (monitorsMap.containsKey(processingName)) {
            ProcessingTimes times = monitorsMap.get(processingName);
            times.end();
            LOG.info("Done '" + times.getProcessing() + "' in " + times.getDurationMs() + "ms");
        } else {
            LOG.error("No processing with the name '" + processingName + "'");
        }
    }

    public static List<ProcessingTimes> getMonitors() {
        return monitors;
    }

    public static void print() {
        LOG.info("Processing times summary:");
        monitors.forEach(monitor -> {
            long duration = monitor.getDurationMs();
            if (duration >= 0) {
                String percentageString = getPercentage(duration);
                LOG.info("Executed '" + monitor.getProcessing() + "' in " + duration + "ms " + percentageString);
            } else {
                LOG.info("Executing '" + monitor.getProcessing() + "' not finished");
            }
        });
    }

    public static String getPercentage(long duration) {
        String percentageString = "";
        if (duration >= 0) {
            if (referenceTimes != null && referenceTimes.getDurationMs() > 0) {
                percentageString = "(" + FormattingUtils.getFormattedPercentage(100.0 * duration / referenceTimes.getDurationMs(), "<1") + "%)";
            }
        }

        return percentageString;
    }
}

