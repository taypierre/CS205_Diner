package com.example.osdiner;

import android.util.Log;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class CustomerGeneratorThread extends Thread {
    private static final String TAG = "CustomerGenerator";

    private final BlockingQueue<Customer> customerQueue;
    private final Random random = new Random();
    private volatile boolean running = true;

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
        this.dinerState = dinerState;
        this.customerQueue = queue;
    }


    public void pauseGeneration() {
        paused = true;
        // Set pause flag
        Log.d(TAG, "Pause signaled.");
    }

    public void resumeGeneration() {
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll(); // Wake up thread if it was waiting
        }
        Log.d(TAG, "Resume signaled.");
    }


    public void stopGenerating() {
        Log.i(TAG, ">>> stopGenerating() method ENTERED. Setting running=false and interrupting.");
        running = false;
        resumeGeneration();
        interrupt();
    }

    @Override
    public void run() {
        Log.d(TAG, "run() started.");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Check pause flag
                synchronized (pauseLock) {
                    while (paused && running) {
                        Log.d(TAG, "Generation paused, waiting...");
                        pauseLock.wait();
                        Log.d(TAG, "Generation woken up from pause.");
                    }
                }

                if (!running) break;


                // Calculate Sleep Time
                int sleepTimeMs = ABSOLUTE_MIN_TIME_MS;
                try {
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
                    sleepTimeMs = BASE_MIN_SLEEP_MS;
                }
                // --------------------------------------------

                Thread.sleep(sleepTimeMs);

                //Check if paused or stopped DURING sleep
                if (paused || !running) {
                    continue;
                }

                // Create a new customer
                Customer newCustomer = new Customer();
                Log.d(TAG, "Generated Customer: " + newCustomer.getDisplayId() + ". Attempting offer()...");

                //  Add customer to queue
                if (dinerState != null && !dinerState.isGameOver()) {
                    boolean added = customerQueue.offer(newCustomer);
                    if (!added) {
                        Log.w(TAG, "Arrival queue is full! Customer " + newCustomer.getDisplayId() + " was not added.");
                    }
                }

            } catch (InterruptedException e) {
                Log.w(TAG, "Thread interrupted (likely stopping or pause wait).");

                if (!running) {
                    break;
                }

            } catch (Exception e) {
                Log.e(TAG, "!!! Unexpected Exception in CustomerGenerator loop !!!", e);
                running = false;
            }
        } // End while loop
        Log.i(TAG, "run() finished.");
    }
}