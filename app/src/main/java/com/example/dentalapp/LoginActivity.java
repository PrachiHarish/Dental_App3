//package com.example.oralapp;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.DocumentSnapshot;
//
//public class LoginActivity extends AppCompatActivity {
//
//    private EditText etPatientID, etPassword;
//    private Button btnLogin;
//    private FirebaseFirestore firestore;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        // Initialize UI components
//        etPatientID = findViewById(R.id.etPatientID);
//        etPassword = findViewById(R.id.etPassword);
//        btnLogin = findViewById(R.id.btnLogin);
//
//        // Initialize Firestore instance
//        firestore = FirebaseFirestore.getInstance();
//
//        // Set onClick listener for login button
//        btnLogin.setOnClickListener(view -> {
//            String patientID = etPatientID.getText().toString().trim();
//            String password = etPassword.getText().toString().trim();
//
//            // Validate input fields
//            if (TextUtils.isEmpty(patientID) || TextUtils.isEmpty(password)) {
//                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Authenticate the patient using the ID and password
//            authenticatePatient(patientID, password);
//        });
//    }
//
//    private void authenticatePatient(String patientID, String password) {
//        // Get reference to the Firestore document corresponding to the patientID
//        DocumentReference docRef = firestore.collection("patients").document(patientID);
//
//        // Fetch the document
//        docRef.get().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                DocumentSnapshot documentSnapshot = task.getResult();
//
//                if (documentSnapshot.exists()) {
//                    // Retrieve the stored password
//                    String storedPassword = documentSnapshot.getString("password");
//
//                    if (storedPassword != null && storedPassword.equals(password)) {
//                        // Successful login
//                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
//
//                        // Redirect to the HomePageActivity
//                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                        startActivity(intent);
//                        finish(); // Close the LoginActivity to prevent going back
//                    } else {
//                        // Invalid password
//                        Toast.makeText(LoginActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    // Patient ID not found
//                    Toast.makeText(LoginActivity.this, "Patient ID Not Found", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                // Error fetching document
//                Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}



//correct
package com.example.dentalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etPatientID, etPassword;
    private Button btnLogin;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI components
        etPatientID = findViewById(R.id.etPatientID);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Initialize Firestore instance
        firestore = FirebaseFirestore.getInstance();

        // Set onClick listener for login button
        btnLogin.setOnClickListener(view -> {
            String patientID = etPatientID.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validate input fields
            if (TextUtils.isEmpty(patientID) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Authenticate the patient using the ID and password
            authenticatePatient(patientID, password);
        });
    }

    private void authenticatePatient(String patientID, String password) {
        // Get reference to the Firestore document corresponding to the patientID
        DocumentReference docRef = firestore.collection("patients").document(patientID);

        // Fetch the document
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task.getResult();

                if (documentSnapshot.exists()) {
                    // Retrieve the stored password
                    String storedPassword = documentSnapshot.getString("password");

                    if (storedPassword != null && storedPassword.equals(password)) {
                        // Successful login
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        // Redirect to the HomePageActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("PATIENT_ID", patientID);
                        startActivity(intent);
                        finish(); // Close the LoginActivity to prevent going back
                    } else {
                        // Invalid password
                        Toast.makeText(LoginActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Patient ID not found
                    Toast.makeText(LoginActivity.this, "Patient ID Not Found", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Error fetching document
                Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

