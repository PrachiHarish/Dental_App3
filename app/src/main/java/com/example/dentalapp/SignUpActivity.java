package com.example.dentalapp;



import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etAge, etPassword;
    private Button btnSignUp;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Sign-up button click listener
        btnSignUp.setOnClickListener(view -> {
            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validate the input fields
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(password)) {
                Toast.makeText(SignUpActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate age
            int age;
            try {
                age = Integer.parseInt(ageStr);
                if (age <= 0 || age > 120) {
                    Toast.makeText(SignUpActivity.this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(SignUpActivity.this, "Age must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate password strength
            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate unique patient ID
//            String patientID = UUID.randomUUID().toString();
            String patientID = "PID" + System.currentTimeMillis() % 100000 + (int)(Math.random() * 100);


            // Create Patient object
            Patient patient = new Patient(patientID, name, String.valueOf(age), password);  // You should hash the password before saving it

            // Save the patient data to Firestore
            db.collection("patients") // "patients" is your Firestore collection
                    .document(patientID)  // Using the patientID as the document ID
                    .set(patient)  // Save the Patient object
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Show Patient ID in an AlertDialog
                            new AlertDialog.Builder(SignUpActivity.this)
                                    .setTitle("Sign-Up Successful")
                                    .setMessage("Your Patient ID: " + patientID)  // Display the Patient ID
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        // Navigate to LoginActivity after successful sign-up
                                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                        finish(); // Optional: Finish SignUpActivity if you don't want to go back
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Sign-Up Failed. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
