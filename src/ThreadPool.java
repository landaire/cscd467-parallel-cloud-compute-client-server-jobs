import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Lander Brandt on 2/18/16.
 */
public class ThreadPool {
    final int _capacity;
    private int numActiveThreads;
    private final Worker[] workers;
    private boolean _stopped = false;
    private final SharedQueue<Job> queue;

    public ThreadPool(int capacity, SharedQueue<Job> queue) {
        this._capacity = capacity;
        this.workers = new Worker[capacity];
        this.queue = queue;
    }

    /**
     * The maximum number of workers
     * @return
     */
    public int capacity() {
        return this._capacity;
    }

    /**
     * Grows the number of active workers to {count}
     * @param count the number of workers that should exist
     */
    public synchronized void growActiveWorkers(int count) {
        if (isStopped()) {
            throw new IllegalStateException("Cannot grow active workers in a stopped state");
        }

        if (count > _capacity) {
            throw new IllegalArgumentException("count > capacity");
        }

        log(String.format("Growing workers from %d to %d", numActiveThreads, count));

        int neededWorkers = count - numActiveThreads, lastFreeIndex = 0;

        for (int i = 0; i < neededWorkers; i++) {
            for (int j = lastFreeIndex; j < workers.length; j++) {
                if (workers[j] != null) {
                    continue;
                }

                workers[j] = new Worker(j, queue);
                workers[j].start();
                workers[j].setName("Worker " + j);
                lastFreeIndex = j;
                numActiveThreads++;

                break;
            }
        }
    }

    /**
     * Shrinks the number of active workers to {count}
     * @param count the number of workers that should exist
     */
    public synchronized void shrinkActiveWorkers(int count) {
        if (isStopped()) {
            throw new IllegalStateException("Cannot shrink active workers in a stopped state");
        }

        if (count < 0) {
            throw new IllegalArgumentException("count > 0");
        }

        log(String.format("Shrinking workers from %d to %d", numActiveThreads, count));

        // Notify waiting threads that they should exit
        int killed = 0, killNum = numActiveThreads - count;
        ArrayList<Worker> killedWorkers = new ArrayList<>(killNum);

        for (int i = 0; i < workers.length; i++) {
            if (killed == killNum) {
                break;
            }

            Worker worker = workers[i];

            if (worker == null) {
                continue;
            }

            if (worker.getState() == Thread.State.WAITING) {
                worker.kill();
                workers[i] = null;

                killedWorkers.add(worker);
                killed++;
            }
        }

        // this can occur if the number of waiting threads < killNum
        // in this case we'll just start notifying other active threads that
        // they need to die
        if (killed != killNum) {
            for (int i = 0; i < workers.length; i++) {
                if (killed == killNum) {
                    break;
                }

                Worker worker = workers[i];
                if (worker == null) {
                    continue;
                }

                worker.kill();
                workers[i] = null;
                killedWorkers.add(worker);

                killed++;
            }
        }

        // Wait for all threads to exit before we release the lock on `this`
        while (killedWorkers.size() > 0) {
            for (int i = 0; i < killedWorkers.size(); i++) {
                if (!killedWorkers.get(i).isAlive()) {
                    killedWorkers.remove(i);
                }
            }
        }

        numActiveThreads -= killed;
    }

    /**
     * Sets the number of job threads to {count}
     * @param count
     */
    public synchronized void setNumActiveWorkers(int count) {
        if (count > capacity()) {
            log("count exceeds capacity -- using capacity instead");
            count = capacity();
        }

        if (count == numActiveThreads) {
            return;
        }

        // the equals case is ignored
        if (count > numActiveThreads) {
            this.growActiveWorkers(count);
        } else {
            this.shrinkActiveWorkers(count);
        }

        log("Total number of threads in pool: " + activeWorkers());
    }

    /**
     * Returns the number of worker threads available
     * @return
     */
    public synchronized int activeWorkers() {
        return numActiveThreads;
    }

    /**
     * Stops the thread pool from growing/shrinking and kills all existing workers
     */
    public synchronized void stop() {
        log("Stop received");
        this._stopped = true;
        killWorkers();
    }

    public synchronized boolean isStopped() {
        return this._stopped;
    }

    public synchronized void join() {
        for (int i = 0; i < workers.length; i++) {
            Worker worker = workers[i];

            if (worker == null) {
                continue;
            }

            try {
                worker.join();
            } catch (InterruptedException e) {
                continue;
            }

            workers[i] = null;
        }
    }
    private synchronized void killWorkers() {
        log("Killing all workers");
        for (Worker worker : workers) {
            if (worker == null) {
                continue;
            }

            worker.kill();
            worker.interrupt();

            numActiveThreads--;
        }
    }

    private void log(String message) {
        SimpleDateFormat dt = new SimpleDateFormat("hh:mm:ss yyyy-mm-dd");

        System.out.printf("[ThreadPool] %s at %s\n", message, dt.format(new Date()));
    }
}
