import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by Lander Brandt on 2/19/16.
 */
public class ParallelClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9898;
    private static Random rand;

    public static void main(String[] args) {
        rand = new Random(System.currentTimeMillis());
        int numClients = randInt(10, 500);
        Thread[] workers = new Thread[numClients];

        log(String.format("Creating %d clients", numClients));

        for (int i = 0; i < numClients; i++) {
            workers[i] = new Thread(new ClientWorker(i));
            workers[i].start();
        }

        for (int i = 0; i < numClients; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException ignored) {
            }
        }

        log("All clients have quit");
    }

    /**
     * Taken from http://stackoverflow.com/a/363692/455678
     *
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    private static int randInt(int min, int max) {
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return rand.nextInt((max - min) + 1) + min;
    }

    private static void log(String message) {
        SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

        System.out.printf("[Main] %s at %s\n", message, dt.format(new Date()));
    }

    private static class ClientWorker implements Runnable {
        private static String[] commands = new String[] {
                "ADD",
                "MUL",
                "DIV",
                "SUB",
        };

        private final int index;

        ClientWorker(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            int delay = randInt(100, 5000);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return;
            }

            try {
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                while (!Thread.currentThread().isInterrupted()) {
                    String command = commands[randInt(0, commands.length - 1)];
                    int num1 = randInt(0, 1000);
                    int num2 = randInt(1, 1000); // min 1 because of division by 0

                    String sentCommand = String.format("%s,%d,%d", command, num1, num2);

                    log(sentCommand);
                    out.println(sentCommand);
                    String response = in.readLine();

                    if (response == null) {
                        break;
                    }

                    log(response);
                }
            } catch (IOException e) {
                log(e.toString());
            }
        }

        private void log(String message) {
            SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

            System.out.printf("[Client %d] %s at %s\n", index, message, dt.format(new Date()));
        }
    }
}
