import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Supports optimized buffered printing to system console from a separate thread with minimal blocking.
 */
public final class ConsoleSupport {
    private final static String END = "$EOF";
    private final static boolean DEBUG = false;
    private static final int CAPACITY = 10_000;

    private final PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(java.io.FileDescriptor.out), StandardCharsets.UTF_8), 8192), false);
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(CAPACITY);

    ConsoleSupport() {
        final long start = System.currentTimeMillis();

        new Thread(() -> {
            while (true) {
                try {
                    String line = queue.take();

                    if (line == END)
                        break;

                    out.println(line);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }

            if (DEBUG)
                out.print("!!! Completed in " + (System.currentTimeMillis() - start));

            out.flush();
        }, "Console Printer Worker").start();
    }

    public void addToPrintQueue(String s) {
        try {
            queue.put(s); // will block if the queue has reached its capacity.
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

    public void shutdown() {
        try {
            queue.put(END);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }
}
