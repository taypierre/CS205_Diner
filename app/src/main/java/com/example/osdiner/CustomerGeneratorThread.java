package com.example.osdiner;

import android.util.Log;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class CustomerGeneratorThread extends Thread {
    private static final String TAG = "CustomerGenerator";

    private final BlockingQueue<Customer> customerQueue;
    private final Random random = new Random();
    private volatile boolean running = true;
    // Average time between customer arrivals (adjust later for difficulty)
    private static final int AVG_ARRIVAL_DELAY_MS = 10000;

    private final DinerState dinerState;

    private volatile boolean paused = false;
    private final Object pauseLock = new Object();



    private static final int BASE_MIN_SLEEP_MS = 2500;
    private static final int BASE_MAX_SLEEP_MS = 8000;
    private static final int SCORE_THRESHOLD = 150;
    private static final int MAX_TIME_REDUCTION_MS = 300;
    private static final int ABSOLUTE_MIN_TIME_MS = 1500;


    public CustomerGeneratorThread(BlockingQueue<Customer> queue, DinerState dinerState) {
        super("CustomerGeneratorThread");
        this.dinerState = dinerState; // Correctly store reference
        this.customerQueue = queue;
    }

    // <<< START PAUSE/RESUME METHODS >>>
    public void pauseGeneration() {
        paused = true;
        // No need to notify, just set flag
        Log.d(TAG, "Pause signaled.");
    }

    public void resumeGeneration() {
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll(); // Wake up thread if it was waiting
        }
        Log.d(TAG, "Resume signaled.");
    }
    // <<< END PAUSE/RESUME METHODS >>>


    public void stopGenerating() {
        Log.i(TAG, ">>> stopGenerating() method ENTERED. Setting running=false and interrupting.");
        running = false;
        resumeGeneration(); // <<< ADD: Ensure thread wakes up if paused before interrupting
        interrupt(); // Interrupt sleep or wait
    }

    @Override
    public void run() {
        Log.d(TAG, "run() started.");
        // Use 'running' flag consistently, checking interrupt status is good too
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // <<< START PAUSE CHECK >>>
                synchronized (pauseLock) {
                    while (paused && running) { // Check running flag too
                        Log.d(TAG, "Generation paused, waiting...");
                        pauseLock.wait(); // Wait until notified by resumeGeneration()
                        Log.d(TAG, "Generation woken up from pause.");
                    }
                }
                // If stopGenerating was called while waiting, exit loop
                if (!running) break;
                // <<< END PAUSE CHECK >>>


                // --- Calculate Sleep Time (Your existing logic) ---
                int sleepTimeMs = ABSOLUTE_MIN_TIME_MS; // Default value
                try { // Wrap calculation just in case
                    int score = (dinerState != null) ? dinerState.getScore() : 0;
                    int totalReductionMs = (score / SCORE_THRESHOLD) * MAX_TIME_REDUCTION_MS;
                    int dynamicMaxTimeMs = Math.max(ABSOLUTE_MIN_TIME_MS, BASE_MAX_SLEEP_MS - totalReductionMs);
                    int dynamicMinTimeMs = Math.min(BASE_MIN_SLEEP_MS, dynamicMaxTimeMs - 500);
                    if (dynamicMinTimeMs >= dynamicMaxTimeMs) {
                        dynamicMinTimeMs = Math.max(500, dynamicMaxTimeMs - 500);
                    }
                    sleepTimeMs = random.nextInt(dynamicMaxTimeMs - dynamicMinTimeMs + 1) + dynamicMinTimeMs;
                    Log.d(TAG, "Score: " + score + " => Sleep Range: [" + dynamicMinTimeMs + "-" + dynamicMaxTimeMs + "]ms. Sleeping for " + sleepTimeMs + " ms...");
                } catch (Exception calcEx) {
                    Log.e(TAG, "Error calculating sleep time", calcEx);
                    sleepTimeMs = BASE_MIN_SLEEP_MS; // Fallback sleep time
                }
                // --------------------------------------------

                Thread.sleep(sleepTimeMs);

                // <<< Check if paused or stopped DURING sleep >>>
                if (paused || !running) {
                    continue; // Go back to the start of the loop to check pause/running state
                }

                // 2. Create a new customer
                Customer newCustomer = new Customer(); // Using your creation logic
                Log.d(TAG, "Generated Customer: " + newCustomer.getDisplayId() + ". Attempting offer()...");

                // 3. Offer onto the queue (non-blocking)
                if (dinerState != null && !dinerState.isGameOver()) { // Check game not over
                    boolean added = customerQueue.offer(newCustomer);
                    if (!added) {
                        Log.w(TAG, "Arrival queue is full! Customer " + newCustomer.getDisplayId() + " was not added.");
                        // Optional: Wait a bit if queue full
                        // try { Thread.sleep(500); } catch (InterruptedException ie) { running = false; }
                    }
                }

            } catch (InterruptedException e) {
                Log.w(TAG, "Thread interrupted (likely stopping or pause wait).");
                // If interrupted, let the while loop condition (!running || isInterrupted) handle termination
                if (!running) { // If stopGenerating was called
                    break; // Exit loop immediately
                }
                // If it was interrupted during pause 'wait', the outer loop re-evaluates 'paused'
            } catch (Exception e) { // Catch other potential runtime errors
                Log.e(TAG, "!!! Unexpected Exception in CustomerGenerator loop !!!", e);
                running = false; // Stop on other errors
            }
        } // End while loop
        Log.i(TAG, "run() finished.");
    }
}