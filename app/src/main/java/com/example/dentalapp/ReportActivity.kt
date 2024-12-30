package com.example.dentalapp

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class ReportActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportAdapter: ReportAdapter
    private var patientID: String? = null
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        reportAdapter = ReportAdapter()
        recyclerView.adapter = reportAdapter

        patientID = intent.getStringExtra("PATIENT_ID")
        db = FirebaseFirestore.getInstance()

        if (patientID != null) {
            fetchReportData()
        } else {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchReportData() {
        db.collection("patients").document(patientID!!)
            .get()
            .addOnSuccessListener { document ->
                val predictions = document.get("predictions") as? List<Map<String, Any>>?
                if (predictions != null) {
                    reportAdapter.submitList(predictions)
                } else {
                    Toast.makeText(this, "No predictions found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
