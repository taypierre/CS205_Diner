package com.example.osdiner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context; // Import Context
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // Import TextView
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Define constants for SharedPreferences
    public static final String PREFS_NAME = "GamePrefs";
    public static final String KEY_HIGHSCORE = "key_highscore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // --- Load and Display High Score ---
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int highScore = prefs.getInt(KEY_HIGHSCORE, 0); // Load high score, default to 0

        TextView highScoreTextView = findViewById(R.id.text_high_score); // Find the new TextView
        if (highScoreTextView != null) {
            highScoreTextView.setText("High Score: " + highScore); // Set the text
        }
        // --- End High Score Display ---


        // --- Find and Setup Buttons (existing code) ---
        Button startButton = findViewById(R.id.button_start_game);
        Button exitButton = findViewById(R.id.button_exit);

        // Set click listeners (existing code)
        if (startButton != null) {
            startButton.setOnClickListener(v -> {
                Intent gameIntent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(gameIntent);
            });
        }

        if (exitButton != null) {
            exitButton.setOnClickListener(v -> {
                finishAffinity();
            });
        }


        Button howToPlayButton = findViewById(R.id.buttonHowToPlay); // Use the ID you set in activity_main.xml
        if (howToPlayButton != null) {
            howToPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create an Intent to launch HowToPlayActivity
                    Intent intent = new Intent(MainActivity.this, HowToPlayActivity.class);
                    startActivity(intent);
                }
            });
        }
        Button creditsButton = findViewById(R.id.buttonCredits); // Use the ID from your layout
        if (creditsButton != null) {
            // Set its text if using string resources (optional)
            // creditsButton.setText(R.string.credits_button_text);

            creditsButton.setOnClickListener(v -> {
                // Call the method to show the dialog when clicked
                showCreditsDialog();
            });
        }
    }

    // Refresh high score display when returning to the menu
    @Override
    protected void onResume() {
        super.onResume();
        // --- Load and Display High Score (repeated from onCreate for refresh) ---
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int highScore = prefs.getInt(KEY_HIGHSCORE, 0);

        TextView highScoreTextView = findViewById(R.id.text_high_score);
        if (highScoreTextView != null) {
            highScoreTextView.setText("High Score: " + highScore);
        }
        // --- End High Score Display ---
    }
    private void showHowToPlayDialog() {
        new AlertDialog.Builder(this) // Use 'this' or 'MainActivity.this' for context
                .setTitle("How to Play")
                .setMessage("Rule 1: Do this...\nRule 2: Do that...\nRule 3: Profit!") // Add your rules as a string (\n for new lines)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Close the dialog when OK is clicked
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info) // Optional icon
                .setCancelable(true) // Allow closing by tapping outside (optional)
                .show();
    }

    private void showCreditsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.credits_dialog_title);
        builder.setMessage(R.string.credits_dialog_message);

        builder.setPositiveButton(R.string.dialog_ok_button, null);


        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}