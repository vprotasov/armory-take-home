import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class holds a reference to a log file, current text line of the log and a reader.
 * <p>
 * We assume that all the dates are ISO 8601 format and all the dates always use the 'Z' or '+00' timezone (= UTC),
 * do not use any time zone offset and there are no fractional parts of the seconds.
 * In this case this format supports lexicographical order comparisons.
 */
final class LogFileEntry implements Comparable<LogFileEntry> {
    private static final DateFormat SHORT_ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    static {
        SHORT_ISO_8601_FORMAT.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    final File file;
    private final boolean parseDate;

    String line; // Current line of the text.
    private String dateTimeStr;
    private long timestamp;
    private long lineNumber; // For error handling only.

    private final BufferedReader reader;

    LogFileEntry(File file, boolean parseDate) throws IOException {
        this.file = file;
        this.parseDate = parseDate;

        reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
    }

    public String nextLine() throws IOException {
        line = reader.readLine();

        if (line == null)
            try {
                reader.close();
            } catch (Exception e) {
                System.err.println("I/O exception while closing file: " + file.getName());
                e.printStackTrace();
            }

        return line;
    }

    /**
     * Reads next line of text and finds date/time substring.
     *
     * @throws IOException
     */
    public void parseNextLine() throws IOException {
        nextLine();

        if (line != null) {
            int commaPos = line.indexOf(',');
            if (commaPos == -1) {
                System.err.println("No comma found on line " + lineNumber + " in file " + file.getName());

                parseNextLine();
            } else {
                dateTimeStr = line.substring(0, commaPos);
                lineNumber++;

                if (parseDate)
                    try {
                        timestamp = SHORT_ISO_8601_FORMAT.parse(dateTimeStr).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace(System.err);
                    }
            }
        }
    }

    @Override
    public int compareTo(LogFileEntry o) {
        if (parseDate)
            return Long.compare(timestamp, o.timestamp);

        if (dateTimeStr == null)
            return 1;

        if (o.dateTimeStr == null)
            return -1;

        return dateTimeStr.compareTo(o.dateTimeStr);

    }

    /**
     * Returns true if the date/time of the current line is chronologically before the current date/time of the other log file.
     */
    public boolean isBefore(LogFileEntry other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns true if the date/time of the current line is chronologically after the current date/time of the other log file.
     */
    public boolean isAfter(LogFileEntry other) {
        return compareTo(other) > 0;
    }
}
