import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Imagine you have any number of servers (1 to 1000+) that generate log files for your distributed app.
 * Each log file can range from 100MB - 512GB in size.
 * They are copied to your machine which contains only 16GB of RAM.
 * <p>
 * The local directory would look like this:
 * /temp/server-ac329xbv.log
 * /temp/server-buyew12x.log
 * /temp/server-cnw293z2.log
 * <p>
 * Our goal is to print the individual lines out to your screen, sorted by timestamp.
 * .....
 * <p>
 * A log file structured as a `CSV` with the date in ISO 8601 format in the first column and an event in the second column.
 * <p>
 * Each individual file is already in time order.
 * <p>
 * As an example, if file /temp/server-bc329xbv.log looks like:
 * <p>
 * 2016-12-20T19:00:45Z, Server A started.
 * 2016-12-20T19:01:25Z, Server A completed job.
 * 2016-12-20T19:02:48Z, Server A terminated.
 * <p>
 * And file /temp/server-cuyew12x.log looks like:
 * <p>
 * 2016-12-20T19:01:16Z, Server B started.
 * 2016-12-20T19:03:25Z, Server B completed job.
 * 2016-12-20T19:04:50Z, Server B terminated.
 * <p>
 * Then our output would be:
 * <p>
 * 2016-12-20T19:00:45Z, Server A started.
 * 2016-12-20T19:01:16Z, Server B started.
 * <p>
 * NOTE, to be documented: log processing errors will be written to error_log.txt in the working dir.
 * This program should produce similar results to > cat *.log | sort -n.
 * Before running with large number of files i.e. > 512 make sure that console process is configured for the right
 * number of open files. I.e. on *nix call ulimit -n 10000 to support up to 10000 open files.
 * <p>
 * We assume that all the dates are ISO 8601 format and all the dates always use the 'Z' or '+00' timezone (= UTC),
 * do not use any time zone offset and there are no fractional parts of the seconds.
 * <p>
 */
public class LogPrinter {
    /**
     * Defines number of files in the folder after which we switch from lexicographical order compare to parsed date compare.
     */
    public static final int PARSE_DATE_THRESHOLD = 2000;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Run with exactly one argument - path to the source log directory.");

            System.exit(1);
        }

        File folder = new File(args[0]);

        if (!folder.exists()) {
            System.err.println("Source log directory was not found: '" + folder.getAbsolutePath() + "'.");
            System.exit(2);
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".log"));

        if (files == null) {
            System.err.println("Error listing the specified directory.");
            System.exit(3);
        }

        if (files.length == 0) {
            System.err.println("No log files were found in the specified directory.");
            System.exit(4);
        }

        try {
            System.setErr(new PrintStream(Paths.get("error_log.txt").toFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        process(files);
    }

    private static void process(File[] files) {
        ConsoleSupport out = new ConsoleSupport();

        List<LogFileEntry> entries = new ArrayList<>(files.length);
        boolean parseDate = files.length > PARSE_DATE_THRESHOLD;

        for (File f : files) {
            try {
                LogFileEntry fe = new LogFileEntry(f, parseDate);

                fe.parseNextLine();

                if (fe.line != null)
                    entries.add(fe);
            } catch (Exception e) {
                System.err.println("I/O exception while reading from file: " + f.getName());
                e.printStackTrace(System.err);
            }
        }

        while (entries.size() > 1) {
            LogFileEntry first = entries.get(0);
            LogFileEntry second = entries.get(1);

            // Let's find two files which have earliest current timestamps in more optimal way than list sorting.
            if (first.isAfter(second)) {
                LogFileEntry temp = first;
                first = second;
                second = temp;
            }

            int size = entries.size();
            if (size > 2)
                for (int i = 2; i < size; i++) {
                    LogFileEntry fe = entries.get(i);
                    if (fe.isBefore(first)) {
                        second = first;
                        first = fe;
                    } else if (fe.isBefore(second)) {
                        second = fe;
                    }
                }

            // Print all lines from the first file until we find a date in the second file which is before current timestamp.
            while (true) {
                out.addToPrintQueue(first.line);

                try {
                    first.parseNextLine();
                } catch (Exception e) {
                    System.err.println("I/O exception while reading from file: " + first.file.getName());
                    e.printStackTrace();
                    entries.remove(first);
                    break;
                }

                if (first.line == null) { // No more lines left in this file, let's remove it from the list.
                    entries.remove(first);

                    break;
                }

                if (second.isBefore(first))
                    break;
            }
        }

        LogFileEntry last = entries.get(0);

        // Let's print the remaining block of the last file, no need to parse any dates here.
        try {
            while (true) {
                if (last.line == null)
                    break;
                else
                    out.addToPrintQueue(last.line);

                last.nextLine();
            }

            out.shutdown();
        } catch (Exception e) {
            System.err.println("I/O exception while reading from file: " + last.file.getName());
            e.printStackTrace(System.err);
        }
    }
}
