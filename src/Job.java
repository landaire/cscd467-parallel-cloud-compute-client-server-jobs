import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Lander Brandt on 2/18/16.
 */
public class Job implements Runnable {
    private Socket socket;
    private int client;
    private String command;
    private PrintWriter output;

    Job(PrintWriter output, Socket socket, int client, String command) {
        this.socket = socket;
        this.client = client;
        this.command = command;
        this.output = output;
    }

    @Override
    public void run() {
        log("Running command " + command);

        output.println(evaluateCommand());

        if (ThreadManager.killServer) {
            try {
                socket.close();
            } catch (IOException e) {
                log("Error closing socket: " + e);
            }
        }

        log("Responded");
    }

    private String evaluateCommand() {
        String[] commandParts = command.split(",");

        if (commandParts[0].equals("KILL")) {
            ThreadManager.killServer = true;

            return "Killing server";
        }

        try {
            int num1 = Integer.parseInt(commandParts[1]), num2 = Integer.parseInt(commandParts[2]);

            switch (commandParts[0]) {
                case "ADD":
                    return String.format("%d + %d = %d", num1, num2, num1 + num2);
                case "SUB":
                    return String.format("%d - %d = %d", num1, num2, num1 - num2);
                case "DIV":
                    return String.format("%d / %d = %d", num1, num2, num1 / num2);
                case "MUL":
                    return String.format("%d * %d = %d", num1, num2, num1 * num2);
            }
        } catch (Exception e) {
            log("Error: " + e);

            return "Error occurred when executing command: " + command;
        }

        return "Unknown command " + command;
    }

    private void log(String message) {
        SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

        System.out.printf("[Job for client %d] %s at %s\n", client, message, dt.format(new Date()));
    }
}
