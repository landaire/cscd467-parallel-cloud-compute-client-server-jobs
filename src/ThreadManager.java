import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Lander Brandt on 2/18/16.
 */
public class ThreadManager implements Runnable {
    // frequency in milliseconds
    private static final int POLL_FREQUENCY = 100;
    private static final int INITIAL_WORKERS = 5;
    public static boolean killServer = false;

    ServerSocket socket;
    SharedQueue _queue;
    ThreadPool _pool;

    private int t1, t2;
    private boolean _killed;

    ThreadManager(ServerSocket socket, ThreadPool pool, SharedQueue queue, int t1, int t2) {
        this.t1 = t1;
        this.t2 = t2;
        _pool = pool;
        _queue = queue;
        this.socket = socket;

        // By default set the number of active workers to t1/2
        _pool.growActiveWorkers(INITIAL_WORKERS);
    }

    @Override
    public void run() {
        int jobsPreviousIteration = 0;
        while (!isKilled() && !killServer) {
            int jobs = _queue.size();
            if (jobs > 0) {
                log("# Jobs: " + jobs);
            }

            int newWorkerCount = _pool.activeWorkers(),
                    oldWorkerCount = newWorkerCount;

            String message = "";

            if (jobs <= t1) {
                // if queue size is less than the first threshold -- this should be the lowest number of workers possible
                message = "queue size is less than lowest threshold -- setting to minimum number of workers: " + INITIAL_WORKERS;
                _pool.setNumActiveWorkers(INITIAL_WORKERS);
            } else if (jobsPreviousIteration < jobs) {
                // jobs are growing
                if (jobs < t2) {
                    newWorkerCount *= 2;
                    message = "queue size is less than t2 -- doubling workers: " + newWorkerCount;
                    _pool.setNumActiveWorkers(newWorkerCount);
                } else if (jobs > t2) {
                    newWorkerCount *= 2;
                    message = "queue size is greater than t2 -- doubling workers: " + newWorkerCount;
                    _pool.setNumActiveWorkers(newWorkerCount);
                }
            } else if (jobsPreviousIteration > jobs && jobsPreviousIteration < t2) {
                // jobs are shrinking
                newWorkerCount /= 2;
                message = "queue size is less than t2 -- halving workers: " + newWorkerCount;
                _pool.setNumActiveWorkers(newWorkerCount);
            }

            // the pool may set the number of active workers to capacity, so don't trust the # of jobs above
            jobsPreviousIteration = _pool.activeWorkers();

            // only log a message if we actually did something
            if (newWorkerCount != oldWorkerCount) {
                log(message);
            }

            try {
                Thread.sleep(POLL_FREQUENCY);
            } catch (InterruptedException ignored) {
            }
        }

        log("Thread manager received signal to kill all operations");
        _pool.stop();
        log("Pool stop called");

        // tell anything else that we're done
        killServer = true;
        kill();

        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }

    public boolean isKilled() {
        return _killed;
    }

    public void kill() {
        this._killed = true;
    }

    private void log(String message) {
        SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

        System.out.printf("[ThreadManager] %s at %s\n", message, dt.format(new Date()));
    }
}
