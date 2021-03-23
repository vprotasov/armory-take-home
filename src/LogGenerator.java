import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * For testing use only. Generates log files with ISO 8601 timestamps.
 */
public class LogGenerator {
    private static final DateFormat SHORT_ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    public static void main(String[] args) throws FileNotFoundException {
        SHORT_ISO_8601_FORMAT.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

        long time = System.currentTimeMillis();
        System.out.println("Started at: " + SHORT_ISO_8601_FORMAT.format(time));
        System.out.println("...");
        Random r = new Random();
        int fileCount = 30;
        PrintStream[] streams = new PrintStream[fileCount];
        long[] times = new long[fileCount];

        for (int i = 0; i < fileCount; i++) {
            streams[i] = new PrintStream(Paths.get("test_" + i + ".log").toFile());
            times[i] = System.currentTimeMillis();

            for (int j = 0; j < 1_000_000; j++) {
                times[i] += r.nextInt(100_000);
                streams[i].println(SHORT_ISO_8601_FORMAT.format(times[i]) + "," + j + " test" + i);
            }

            streams[i].close();
        }

        System.out.println("Finished at: " + Instant.now());
    }
}
