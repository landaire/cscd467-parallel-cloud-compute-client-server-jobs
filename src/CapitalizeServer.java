import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A server program which accepts requests from clients to
 * capitalize strings.  When clients connect, a new thread is
 * started to handle an interactive dialog in which the client
 * sends in a string and the server thread sends back the
 * capitalized version of the string.
 *
 * The program is runs in an infinite loop, so shutdown in platform
 * dependent.  If you ran it from a console window with the "java"
 * interpreter, Ctrl+C generally will shut it down.
 */
public class CapitalizeServer {
    private static SharedQueue<Job> jobQueue;
    private static ThreadManager manager;

    /**
     * Application method to run the server runs in an infinite loop
     * listening on port 9898.  When a connection is requested, it
     * spawns a new thread to do the servicing and immediately returns
     * to listening.  The server keeps a unique client number for each
     * client that connects just to show interesting logging
     * messages.  It is certainly not necessary to do this.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The capitalization server is running.");
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(9898);

        int t1 = 10, t2 = 20, capacity = 50;

        jobQueue = new SharedQueue<>(capacity);

        ThreadPool pool = new ThreadPool(capacity, jobQueue);
        manager = new ThreadManager(listener, pool, jobQueue, t1, t2);

        Thread managerThread = new Thread(manager);
        managerThread.start();

        try {
            while (!manager.isKilled()) {
                ClientHandler clientHandler = new ClientHandler(listener.accept(), clientNumber++);
                clientHandler.start();
            }
        } catch (SocketException e) {
            // we probably got this exception because the manager killed us
            if (!manager.isKilled()) {
                throw e;
            }
        } finally {
            manager.kill();
            pool.join();

            listener.close();
        }
    }

    /**
     * A private thread to handle capitalization requests on a particular
     * socket.  The client terminates the dialogue by sending a single line
     * containing only a period.
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private int clientNumber;

        public ClientHandler(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
        }

        /**
         * Services this thread's client by first sending the
         * client a welcome message then repeatedly reading strings
         * and sending back the capitalized version of the string.
         */
        public void run() {
            try {
                // Decorate the streams so we can send characters
                // and not just bytes.  Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Hello, you are client #" + clientNumber + ".");

                // Get messages from the client, line by line; return them
                // capitalized
                String input = in.readLine();
                if (input == null) {
                    log("bad input");
                    socket.close();
                }

                if (!jobQueue.add(new Job(out, socket, clientNumber, input))) {
                    out.println("Server is too busy to handle request right now, please try again later");
                    log("Too busy -- had to kill client");
                }
            } catch (IOException e) {
                log("Error:" + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }

                log("Connection closed");
            }
        }

        private void log(String message) {
            SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

            System.out.printf("[Capitalize thread for client %d] %s at %s\n", clientNumber, message, dt.format(new Date()));
        }
    }
}
