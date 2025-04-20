package com.example.osdiner;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
@SuppressWarnings("BusyWait")
public class GameThread extends Thread {
    private static final String TAG = "GameThread";

    private final SurfaceHolder surfaceHolder;
    private final DinerView dinerView;
    private final DinerState dinerState;
    private final Context context;
    private volatile boolean running;
    private long lastUpdateTimeNs;

    private boolean isGameOverNotifiedOrSaved = false;
    private long intervalStartTimeMs;
    private static final long ARRIVAL_PROCESS_INTERVAL_MS = 3000;

    private static final long TARGET_FPS = 60;
    private static final long OPTIMAL_TIME_NS = 1_000_000_000 / TARGET_FPS;

    private volatile boolean paused = false;

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

    public void resumeGame() {
        paused = false;

    }
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        Log.d(TAG, "GameThread run() started.");
        Canvas canvas;
        lastUpdateTimeNs = System.nanoTime();

        while (running) {

            if (paused) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Pause sleep interrupted", e);

                }
                continue;
            }



            long nowNs = System.nanoTime();
            long elapsedTimeNs = nowNs - lastUpdateTimeNs;
            lastUpdateTimeNs = nowNs;

            // Clamp deltaTime
            if (elapsedTimeNs <= 0) elapsedTimeNs = 1;
            if (elapsedTimeNs > OPTIMAL_TIME_NS * 5) {
                elapsedTimeNs = OPTIMAL_TIME_NS * 2;
            }
            double deltaTime = elapsedTimeNs / 1_000_000_000.0;

            int angryLeavers = 0;

            try {
                if (this.dinerState != null) {
                    angryLeavers = this.dinerState.update(deltaTime);
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
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during high score saving check", e);
            }

            // Trigger Angry Leave Effects
            try {
                if (angryLeavers > 0 && this.dinerView != null) {
                    Log.d(TAG, ">>> Angry leavers detected: " + angryLeavers + ", attempting to trigger effects via post <<<");

                    final int finalAngryLeavers = angryLeavers;

                    this.dinerView.post(() -> dinerView.triggerAngryLeaveEffects(finalAngryLeavers));

                }
            } catch (Exception e) { Log.e(TAG, "Exception posting triggerAngryLeaveEffects", e); }

            // Update View-Specific Logic
            try {
                if (this.dinerView != null) {
                    this.dinerView.update(deltaTime);
                }
            } catch (Exception e) { Log.e(TAG, "Exception during DinerView.update()", e); }


            // Check Interval Timer for Customer Arrivals
            long nowMs = System.currentTimeMillis();
            if (nowMs - intervalStartTimeMs >= ARRIVAL_PROCESS_INTERVAL_MS) {

                if (this.dinerView != null) {
                    dinerView.triggerProcessArrivals();
                }
                intervalStartTimeMs = nowMs;
            }


            // Render Game State
            canvas = null;
            try {
                canvas = this.surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        if (this.dinerView != null) {
                            dinerView.drawGame(canvas);
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

            // Frame Rate Control
            long loopTimeNs = System.nanoTime() - nowNs;
            long sleepTimeNs = OPTIMAL_TIME_NS - loopTimeNs;
            if (sleepTimeNs > 0) {
                try { Thread.sleep(sleepTimeNs / 1_000_000, (int) (sleepTimeNs % 1_000_000)); }
                catch (InterruptedException e) { Log.w(TAG, "GameThread sleep interrupted", e); running = false; }
            }


            if (isGameOverNotifiedOrSaved && running) {
                try { Thread.sleep(50); }
                catch (InterruptedException e) { running = false; }
            }

        }
        Log.d(TAG, "GameThread run() finished.");
    }
}