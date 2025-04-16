package com.example.osdiner;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    private DinerView dinerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Reset customer ID for new game
        Customer.resetCustomerIdCounter();

        // Create an instance of DinerView
        dinerView = new DinerView(this, null);

        setContentView(dinerView);
        Log.d("GameActivity", "onCreate: DinerView set as content view.");
    }

    public void showInGameMenu() {
        runOnUiThread(() -> {
            if (isFinishing() || dinerView == null || !dinerView.isPaused()) {
                Log.w("GameActivity", "Tried to show menu but game wasn't paused or finishing.");
                return;
            }

            new AlertDialog.Builder(GameActivity.this)
                    .setTitle("Paused")
                    .setCancelable(false)
                    .setPositiveButton("Resume", (dialog, which) -> {
                        if (dinerView != null) {
                            dinerView.resumeGame();
                        }
                    })
                    .setNegativeButton("Quit to Menu", (dialog, which) -> stopGameAndFinish())
                    .show();
            Log.d("GameActivity", "In-Game menu dialog shown.");
        });
    }

    public void stopGameAndFinish() {
        Log.d("GameActivity", "Stopping game and finishing activity...");
        if (dinerView != null) {
            dinerView.stopGame();
        } else {
            Log.e("GameActivity", "DinerView is null, cannot stop game!");
        }
        finish();
    }

    public void showGameOverDialog(int finalScore) {
        Log.d("GameActivity", "showGameOverDialog() called with score: " + finalScore);

        if (isFinishing()) {
            Log.w("GameActivity", "Activity is finishing, cannot show Game Over dialog.");
            return;
        }

        new AlertDialog.Builder(GameActivity.this)
                .setTitle("Game Over!")
                .setMessage("Final Score: " + finalScore)
                .setCancelable(false)
                .setPositiveButton("Main Menu", (dialog, which) -> {
                    stopGameAndFinish(); // Clean up threads and finish activity
                })
                .show(); // Display the dialog
        Log.d("GameActivity", "Game Over dialog shown.");
    }


}

