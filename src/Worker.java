import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Lander Brandt on 2/18/16.
 */
public class Worker extends Thread {
    private final int index;
    private boolean killed = false;
    private SharedQueue<Job> jobQueue;

    Worker(int index, SharedQueue<Job> queue) {
        this.index = index;
        jobQueue = queue;
    }

    @Override
    public void run() {
        log("Starting");
        while (!isKilled()) {
            try {
                log("Taking job off queue");
                Job job = jobQueue.take();

                if (job == null) {
                    log("got a null job");
                    continue;
                }

                log("Running job");
                job.run();
                log("Job finished");
            } catch (InterruptedException ignored) {

            }
        }

        log("Done!");
    }

    public synchronized void kill() {
        this.killed = true;
    }

    public synchronized boolean isKilled() {
        return killed;
    }


    private void log(String message) {
        SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

        System.out.printf("[Worker %d] %s at %s\n", index, message, dt.format(new Date()));
    }
}
