package com.example.osdiner;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class HowToPlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);


        Button closeButton = findViewById(R.id.buttonCloseHowToPlay);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> finish());
        }
    }
}