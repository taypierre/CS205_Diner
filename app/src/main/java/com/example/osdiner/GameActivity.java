package com.example.osdiner;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GameActivity extends AppCompatActivity {
    private DinerView dinerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- OS Customizations (Keep these) ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // --- End OS Customizations ---


        // <<< CORRECTED CONTENT VIEW >>>
        // Create an instance of your DinerView
        dinerView = new DinerView(this, null);
        // Set your custom DinerView as the content for this activity
        setContentView(dinerView);
        Log.d("GameActivity", "onCreate: DinerView set as content view."); // Add log for confirmation
        // <<< END CORRECTED CONTENT VIEW >>>
    }

    public void showInGameMenu() { // Renamed method
        runOnUiThread(() -> {
            // Check added: Only show if game is actually paused now
            if (isFinishing() || dinerView == null || !dinerView.isPaused()) {
                Log.w("GameActivity", "Tried to show menu but game wasn't paused or finishing.");
                return;
            }

            new AlertDialog.Builder(GameActivity.this)
                    .setTitle("Paused") // Title still makes sense
                    .setCancelable(false)
                    .setPositiveButton("Resume", (dialog, which) -> {
                        if (dinerView != null) {
                            dinerView.resumeGame();
                        }
                    })
                    .setNegativeButton("Quit to Menu", (dialog, which) -> {
                        stopGameAndFinish();
                    })
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
        // This method is called via runOnUiThread from DinerView, so it's safe to show dialog

        if (isFinishing()) { // Check if activity is already closing
            Log.w("GameActivity", "Activity is finishing, cannot show Game Over dialog.");
            return;
        }

        new AlertDialog.Builder(GameActivity.this) // Use Activity context
                .setTitle("Game Over!")
                .setMessage("Final Score: " + finalScore)
                .setCancelable(false) // Prevent dismissing with back button or tapping outside
                .setPositiveButton("Main Menu", (dialog, which) -> {
                    // Main Menu button clicked
                    stopGameAndFinish(); // Clean up threads and finish activity
                })
                .show(); // Display the dialog
        Log.d("GameActivity", "Game Over dialog shown.");
    }


}

