package com.example.faceauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PassRecognitionActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;

    private String registeredUsername = null;
    private String registeredPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pass_recognition);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Check if you are already registered
                if (registeredUsername == null || registeredPassword == null) {
                    Toast.makeText(PassRecognitionActivity.this, "Please register first", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verify that the user name and password entered match the registered information
                if (username.equals(registeredUsername) && password.equals(registeredPassword)) {
                    Toast.makeText(PassRecognitionActivity.this, "Login Successful！", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PassRecognitionActivity.this, SuccessActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(PassRecognitionActivity.this, "Incorrect user name or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Check that the username and password are not empty
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    Toast.makeText(PassRecognitionActivity.this, "Please enter your username and password to register", Toast.LENGTH_SHORT).show();
                } else {
                    // Record the registered user name and password
                    registeredUsername = username;
                    registeredPassword = password;
                    Toast.makeText(PassRecognitionActivity.this, "Registration was successful!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}