// GameThread.java
package com.example.osdiner;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
    private static final String TAG = "GameThread";

    private final SurfaceHolder surfaceHolder;
    private final DinerView dinerView;
    private final DinerState dinerState;
    private final Context context;
    private volatile boolean running;
    private long lastUpdateTimeNs;

    private boolean isGameOverNotifiedOrSaved = false;

    // Interval Timer
    private long intervalStartTimeMs;
    private static final long ARRIVAL_PROCESS_INTERVAL_MS = 3000;

    // Target FPS
    private static final long TARGET_FPS = 60;
    private static final long OPTIMAL_TIME_NS = 1_000_000_000 / TARGET_FPS;

    private volatile boolean paused = false; // <<< ADD Pause flag
    private final Object pauseLock = new Object();

    // <<< MODIFIED Constructor >>>
    public GameThread(Context context, SurfaceHolder surfaceHolder, DinerView dinerView, DinerState dinerState) {
        super("GameThread");
        this.context = context.getApplicationContext();
        this.surfaceHolder = surfaceHolder;
        this.dinerView = dinerView;
        this.dinerState = dinerState;
    }
    public void setRunning(boolean isRunning) {
        this.running = isRunning;
        if (isRunning) {
            lastUpdateTimeNs = System.nanoTime();
            intervalStartTimeMs = System.currentTimeMillis();
            isGameOverNotifiedOrSaved = false;
        }
        if (!isRunning) {
            resumeGame(); // Ensure not left in paused state when stopped
        } else {
            paused = false; // Ensure starts not paused
        }
    }

    public void pauseGame() {
        paused = true;
    }

    // <<< ADD resumeGame method >>>
    public void resumeGame() {
        paused = false;

    }

    // <<< ADD isPaused method >>>
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        Log.d(TAG, "GameThread run() started.");
        Canvas canvas;
        lastUpdateTimeNs = System.nanoTime();

        while (running) {
            // <<< START Pause Check >>>
            if (paused) {
                // Simple pause: Just sleep briefly to yield CPU and check again
                try {
                    // Log.d(TAG, "Thread Paused, sleeping..."); // Optional log for debugging pause
                    Thread.sleep(50); // Sleep for 50ms while paused
                } catch (InterruptedException e) {
                    Log.w(TAG, "Pause sleep interrupted", e);
                    // If interrupted while paused, might want to stop the thread
                    // running = false; // Uncomment if interruption should stop the thread
                }
                // Skip the rest of the game logic updates for this iteration
                continue;
            }
            // <<< END Pause Check >>>


            long nowNs = System.nanoTime();
            long elapsedTimeNs = nowNs - lastUpdateTimeNs;
            lastUpdateTimeNs = nowNs;

            // Clamp deltaTime
            if (elapsedTimeNs <= 0) elapsedTimeNs = 1;
            if (elapsedTimeNs > OPTIMAL_TIME_NS * 5) {
                // Log.w(TAG, "Clamping large deltaTime: " + (elapsedTimeNs / 1e6) + "ms");
                elapsedTimeNs = OPTIMAL_TIME_NS * 2;
            }
            double deltaTime = elapsedTimeNs / 1_000_000_000.0;

            int angryLeavers = 0;

            // 1. Update Game State (Check for null safety)
            try {
                if (this.dinerState != null) {
                    angryLeavers = this.dinerState.update(deltaTime); // <<< CALL STATE UPDATE & GET COUNT
                }
            } catch (Exception e) { Log.e(TAG, "Exception during DinerState.update()", e); }
            try {
                // Check state only if dinerState and context are valid
                if (this.dinerState != null && this.context != null) {
                    // Check if game is over AND we haven't already processed this game over
                    if (this.dinerState.isGameOver() && !this.isGameOverNotifiedOrSaved) {
                        this.isGameOverNotifiedOrSaved = true; // Set flag to save only once per game over
                        Log.i(TAG, "Game Over detected in thread! Attempting to save high score...");

                        int finalScore = this.dinerState.getScore();

                        // Access SharedPreferences using the passed context
                        SharedPreferences prefs = this.context.getSharedPreferences(
                                MainActivity.PREFS_NAME, // Use constant from MainActivity
                                Context.MODE_PRIVATE);
                        int currentHighScore = prefs.getInt(MainActivity.KEY_HIGHSCORE, 0); // Use constant

                        // Compare score and save if it's a new high score
                        if (finalScore > currentHighScore) {
                            Log.i(TAG, "New High Score! Score: " + finalScore + ", Old High Score: " + currentHighScore);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt(MainActivity.KEY_HIGHSCORE, finalScore); // Use constant
                            editor.apply(); // Save asynchronously
                            Log.i(TAG, "New high score saved.");
                        } else {
                            Log.i(TAG, "Score (" + finalScore + ") not higher than High Score ("+ currentHighScore + "). Not saving.");
                        }
                        // Optional: If the game should completely stop here, you could set running = false;
                        // However, that might stop drawing the "GAME OVER" screen. Usually, the user
                        // backs out or interacts with the game over screen to exit.
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during high score saving check", e);
            }

            // 2. Trigger Angry Leave Effects (Check for null safety)
            try {
                if (angryLeavers > 0 && this.dinerView != null) {
                    Log.d(TAG, ">>> Angry leavers detected: " + angryLeavers + ", attempting to trigger effects via post <<<");

                    // <<< Create an effectively final copy for the lambda >>>
                    final int finalAngryLeavers = angryLeavers;

                    // Call the trigger method on the View's UI thread using post
                    // <<< Use the final copy inside the lambda >>>
                    this.dinerView.post(() -> dinerView.triggerAngryLeaveEffects(finalAngryLeavers));

                }
            } catch (Exception e) { Log.e(TAG, "Exception posting triggerAngryLeaveEffects", e); }

            // 3. Update View-Specific Logic (Check for null safety)
            try {
                if (this.dinerView != null) {
                    this.dinerView.update(deltaTime); // <<< CALL VIEW UPDATE (for animations etc)
                }
            } catch (Exception e) { Log.e(TAG, "Exception during DinerView.update()", e); }


            // 4. Check Interval Timer for Customer Arrivals (Check for null safety)
            long nowMs = System.currentTimeMillis();
            if (nowMs - intervalStartTimeMs >= ARRIVAL_PROCESS_INTERVAL_MS) {
                // Log.d(TAG, "Customer arrival interval reached!"); // Can be noisy
                if (this.dinerView != null) {
                    dinerView.triggerProcessArrivals();
                }
                intervalStartTimeMs = nowMs;
            }


            // 5. Render Game State
            canvas = null;
            try {
                canvas = this.surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        if (this.dinerView != null) {
                            dinerView.drawGame(canvas); // <<< CALL DRAW
                        }
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Exception during lockCanvas/drawGame", e); }
            finally {
                if (canvas != null) {
                    try { this.surfaceHolder.unlockCanvasAndPost(canvas); }
                    catch (Exception e) { Log.e(TAG, "Exception during unlockCanvasAndPost", e); }
                }
            }

            // 6. Frame Rate Control
            long loopTimeNs = System.nanoTime() - nowNs;
            long sleepTimeNs = OPTIMAL_TIME_NS - loopTimeNs;
            if (sleepTimeNs > 0) {
                try { Thread.sleep(sleepTimeNs / 1_000_000, (int) (sleepTimeNs % 1_000_000)); }
                catch (InterruptedException e) { Log.w(TAG, "GameThread sleep interrupted", e); running = false; }
            }

            // Exit loop slightly faster if game is over but still running (e.g., showing game over screen)
            if (isGameOverNotifiedOrSaved && running) {
                try { Thread.sleep(50); } // Less aggressive sleep after game over
                catch (InterruptedException e) { running = false; }
            }

        } // End while(running) loop
        Log.d(TAG, "GameThread run() finished.");
    }
}