package it.renvins.downloadit.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class DownloadItFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return String.format(record.getLoggerName() + ": " + record.getMessage() + "%n" + (record.getThrown() != null ? record.getThrown() + "%n" : ""));
    }
}
