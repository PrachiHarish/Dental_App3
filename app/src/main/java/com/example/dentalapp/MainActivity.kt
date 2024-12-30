package com.example.dentalapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dentalapp.ml.Classification
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    // Declaring variables
    lateinit var TakePhoto: Button
    lateinit var ChoosePhoto: Button
    lateinit var Result: TextView
    lateinit var Image: ImageView
    lateinit var Predict: Button
    lateinit var bitmap: Bitmap
    lateinit var getReportButton: Button

    var patientID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Retrieve patientID passed from LoginActivity
        patientID = intent.getStringExtra("PATIENT_ID")

        if (patientID == null) {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show()
        }

        TakePhoto = findViewById(R.id.button4)
        ChoosePhoto = findViewById(R.id.button5)
        Result = findViewById(R.id.textView2)
        Image = findViewById(R.id.imageView7)
        Predict = findViewById(R.id.button)
        getReportButton = findViewById(R.id.getReportButton)

        getReportButton.setOnClickListener {
            if (patientID != null) {
                val intent = Intent(this, ReportActivity::class.java)
                intent.putExtra("PATIENT_ID", patientID)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No patient data available", Toast.LENGTH_SHORT).show()
            }
        }

        val labels = application.assets.open("labels.txt").bufferedReader().readLines()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        TakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 2)
        }

        ChoosePhoto.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select an Image"), 1)
        }

        Predict.setOnClickListener {
            if (!::bitmap.isInitialized) {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(bitmap)
                val byteBuffer = imageProcessor.process(tensorImage).buffer

                val model = Classification.newInstance(this)

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
                inputFeature0.loadBuffer(byteBuffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                var maxIdx = 0
                outputFeature0.forEachIndexed { index, fl ->
                    if (outputFeature0[maxIdx] < fl) {
                        maxIdx = index
                    }
                }
                val predictedLabel = labels[maxIdx]
                Result.text = "Prediction: $predictedLabel"

                model.close()

                val db = FirebaseFirestore.getInstance()

                if (patientID != null) {
                    val currentTimestamp = System.currentTimeMillis()

                    uploadImageToFirebaseStorage { imageUrl ->
                        if (imageUrl != null) {
                            val predictionData = hashMapOf(
                                "prediction" to predictedLabel,
                                "timestamp" to currentTimestamp,
                                "status_of_visit" to "Not Visited",
                                "image_url" to imageUrl
                            )

                            db.collection("patients").document(patientID!!)
                                .update("predictions", FieldValue.arrayUnion(predictionData))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Prediction and image saved successfully", Toast.LENGTH_SHORT).show()
                                    fetchNotVisitedPredictions(db)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to save prediction: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Cannot save prediction. Patient ID is null.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            val uri = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            Image.setImageBitmap(bitmap)
        } else if (requestCode == 2) {
            bitmap = data?.extras?.get("data") as Bitmap
            Image.setImageBitmap(bitmap)
        }
    }

    private fun uploadImageToFirebaseStorage(callback: (String?) -> Unit) {
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference
        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imagesRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                callback(uri.toString())
            }.addOnFailureListener {
                callback(null)
            }
        }.addOnFailureListener {
            callback(null)
        }
    }

    private fun fetchNotVisitedPredictions(db: FirebaseFirestore) {
        db.collection("patients").document(patientID!!).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val predictions = document.get("predictions") as? List<Map<String, Any>>?
                    val notVisitedPredictions = predictions?.filter {
                        it["status_of_visit"] == "Not Visited"
                    }?.map { it["prediction"].toString() } ?: listOf()

                    if (notVisitedPredictions.isNotEmpty()) {
                        scheduleNotification(notVisitedPredictions)
                    }
                }
            }
    }

    private fun scheduleNotification(predictions: List<String>) {
        val delayMillis = 5000L

        Handler(Looper.getMainLooper()).postDelayed({
            val predictionsMessage = predictions.joinToString(", ") { it }

            Toast.makeText(
                this,
                "Please visit the doctor for further evaluation for: $predictionsMessage",
                Toast.LENGTH_LONG
            ).show()
        }, delayMillis)
    }
}

//this is also correct
//package com.example.dentalapp
//
////package com.example.oralapp
//
//import android.content.Intent
//import android.graphics.Bitmap
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.provider.MediaStore
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.dentalapp.ml.Classification
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.FirebaseFirestore
//import org.tensorflow.lite.DataType
//import org.tensorflow.lite.support.image.ImageProcessor
//import org.tensorflow.lite.support.image.TensorImage
//import org.tensorflow.lite.support.image.ops.ResizeOp
//import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
//
//
//class MainActivity : AppCompatActivity() {
//
//    // Declaring variables
//    lateinit var TakePhoto: Button
//    lateinit var ChoosePhoto: Button
//    lateinit var Result: TextView
//    lateinit var Image: ImageView
//    lateinit var Predict: Button
//    lateinit var bitmap: Bitmap
//
//
//    var patientID: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // Retrieve patientID passed from LoginActivity
//        patientID = intent.getStringExtra("PATIENT_ID")
//
//        if (patientID == null) {
//            // Handle the case where no patient ID is passed
//            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show()
//            // Optionally, redirect to the login screen or disable certain features
//            // finish() // Uncomment to close the activity if needed
//        }
//
//        TakePhoto = findViewById(R.id.button4)
//        ChoosePhoto = findViewById(R.id.button5)
//        Result = findViewById(R.id.textView2)
//        Image = findViewById(R.id.imageView7)
//        Predict = findViewById(R.id.button)
//
//        val labels = application.assets.open("labels.txt").bufferedReader().readLines()
//
//        // Image processor setup
//        val imageProcessor = ImageProcessor.Builder()
//            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
//            .build()
//
//        // Take photo on button click
//        TakePhoto.setOnClickListener {
//            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            startActivityForResult(intent, 2)
//        }
//
//        // Choose photo from gallery on button click
//        ChoosePhoto.setOnClickListener {
//            val intent = Intent()
//            intent.setAction(Intent.ACTION_GET_CONTENT)
//            intent.setType("image/*")
//            startActivityForResult(Intent.createChooser(intent, "Select an Image"), 1)
//        }
//
//        // Predict button click logic
//        Predict.setOnClickListener {
//            if (!::bitmap.isInitialized) {
//                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            try {
//                // Preprocess the bitmap
//                val tensorImage = TensorImage(DataType.FLOAT32)
//                tensorImage.load(bitmap)
//                val byteBuffer = imageProcessor.process(tensorImage).buffer
//
//                // Load the TensorFlow Lite model
//                val model = Classification.newInstance(this)
//
//                // Create input tensor
//                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
//                inputFeature0.loadBuffer(byteBuffer)
//
//                // Run model inference
//                val outputs = model.process(inputFeature0)
//                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray
//
//                // Find the predicted label
//                var maxIdx = 0
//                outputFeature0.forEachIndexed { index, fl ->
//                    if (outputFeature0[maxIdx] < fl) {
//                        maxIdx = index
//                    }
//                }
//                val predictedLabel = labels[maxIdx]
//                Result.text = "Prediction: $predictedLabel"
//
//                // Release model resources
//                model.close()
//
//                val db = FirebaseFirestore.getInstance()
//
//                if (patientID != null) {
//                    val currentTimestamp = System.currentTimeMillis()
//
//                    // Data to update
//                    val predictionData = hashMapOf(
//                        "prediction" to predictedLabel,
//                        "timestamp" to currentTimestamp,
//                        "status_of_visit" to "Not Visited"
//                    )
//
//                    // Update predictions in Firestore
//                    db.collection("patients").document(patientID!!)
//                        .update("predictions", FieldValue.arrayUnion(predictionData))
//                        .addOnSuccessListener {
//                            Toast.makeText(this, "Prediction saved successfully", Toast.LENGTH_SHORT).show()
//
//                            // Fetch all "Not Visited" predictions for notifications
//                            db.collection("patients").document(patientID!!).get()
//                                .addOnSuccessListener { document ->
//                                    if (document != null) {
//                                        val predictions = document.get("predictions") as? List<Map<String, Any>>?
//                                        val notVisitedPredictions = predictions?.filter {
//                                            it["status_of_visit"] == "Not Visited"
//                                        }?.map { it["prediction"].toString() } ?: listOf()
//
//                                        // Schedule Notification
//                                        if (notVisitedPredictions.isNotEmpty()) {
//                                            scheduleNotification(notVisitedPredictions)
//                                        }
//                                    }
//                                }
//                        }
//                        .addOnFailureListener { e ->
//                            Toast.makeText(this, "Failed to save prediction: ${e.message}", Toast.LENGTH_SHORT).show()
//                        }
//                } else {
//                    // Handle case when patientID is null
//                    Toast.makeText(this, "Cannot save prediction. Patient ID is null.", Toast.LENGTH_SHORT).show()
//                }
//
//            } catch (e: Exception) {
//                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                e.printStackTrace()
//            }
//        }
//    }
//
//    // Handling the image selection result (from camera or gallery)
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == 1) {
//            val uri = data?.data
//            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
//            Image.setImageBitmap(bitmap)
//        } else if (requestCode == 2) {
//            bitmap = data?.extras?.get("data") as Bitmap
//            Image.setImageBitmap(bitmap)
//        }
//    }
//
//    // Function to schedule notification
//    private fun scheduleNotification(predictions: List<String>) {
//        // Delay of 5 seconds
//        val delayMillis = 5000L
//
//        // Schedule the notification after the delay
//        Handler(Looper.getMainLooper()).postDelayed({
//            // Combine all "Not Visited" predictions into a single notification
//            val predictionsMessage = predictions.joinToString(", ") { it }
//
//            // Show a toast message to remind the user to visit the doctor
//            Toast.makeText(
//                this,
//                "Please visit the doctor for further evaluation for: $predictionsMessage",
//                Toast.LENGTH_LONG
//            ).show()
//        }, delayMillis)
//    }
//}

//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.dentalapp.ui.theme.DentalAppTheme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            DentalAppTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    DentalAppTheme {
//        Greeting("Android")
//    }
//}