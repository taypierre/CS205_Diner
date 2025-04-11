package com.example.osdiner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context; // Import Context
import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;
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
        // Make sure this layout name is correct for THIS activity
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
        Button creditsButton = findViewById(R.id.button_credits);
        Button exitButton = findViewById(R.id.button_exit);

        // Set click listeners (existing code)
        if (startButton != null) {
            startButton.setOnClickListener(v -> {
                Intent gameIntent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(gameIntent);
            });
        }

        if (creditsButton != null) {
            creditsButton.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Credits: [Your Name/Group]", Toast.LENGTH_LONG).show();
            });
        }

        if (exitButton != null) {
            exitButton.setOnClickListener(v -> {
                finishAffinity();
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
}