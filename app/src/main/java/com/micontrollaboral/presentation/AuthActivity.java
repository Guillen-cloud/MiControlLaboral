package com.micontrollaboral.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.micontrollaboral.R;
import com.micontrollaboral.sync.SyncScheduler;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            SyncScheduler.enqueue(this);
            openMainActivity();
            return;
        }
        setContentView(R.layout.activity_auth);

        EditText emailInput = findViewById(R.id.auth_email_input);
        EditText passwordInput = findViewById(R.id.auth_password_input);
        Button loginButton = findViewById(R.id.auth_login_button);
        Button registerButton = findViewById(R.id.auth_register_button);

        loginButton.setOnClickListener(view -> authenticate(emailInput, passwordInput, false));
        registerButton.setOnClickListener(view -> authenticate(emailInput, passwordInput, true));
    }

    private void authenticate(EditText emailInput, EditText passwordInput, boolean createAccount) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, R.string.auth_invalid_credentials, Toast.LENGTH_LONG).show();
            return;
        }
        if (createAccount) {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> openMainActivity())
                    .addOnFailureListener(error -> showAuthError(error.getMessage()));
        } else {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> openMainActivity())
                    .addOnFailureListener(error -> showAuthError(error.getMessage()));
        }
    }

    private void showAuthError(String message) {
        Toast.makeText(this, message == null ? getString(R.string.auth_failed) : message, Toast.LENGTH_LONG).show();
    }

    private void openMainActivity() {
        SyncScheduler.enqueue(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}