import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * This class holds a reference to a log file, current text line of the log and a reader.
 * <p>
 * We assume that all the dates are ISO 8601 format and all the dates always use the 'Z' or '+00' timezone (= UTC),
 * do not use any time zone offset and there are no fractional parts of the seconds.
 * In this case this format supports lexicographical order comparisons.
 */
final class LogFileEntry implements Comparable<LogFileEntry> {
    final File file;

    String line;
    private long lineNumber; // For error handling only.
    private String dateTimeStr;

    private final BufferedReader br;

    LogFileEntry(File file) throws IOException {
        this.file = file;

        br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
    }

    public String nextLine() throws IOException {
        line = br.readLine();

        if (line == null)
            try {
                br.close();
            } catch (Exception e) {
                System.err.println("I/O exception while closing file: " + file.getName());
                e.printStackTrace();
            }

        return line;
    }

    /**
     * Reads next line of text and finds date/time substring.
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
            }
        }
    }

    @Override
    public int compareTo(LogFileEntry o) {
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
