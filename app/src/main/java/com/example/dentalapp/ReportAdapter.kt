package com.example.dentalapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import com.bumptech.glide.Glide

class ReportAdapter : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private var reportList: List<Map<String, Any>> = listOf()

    fun submitList(list: List<Map<String, Any>>) {
        reportList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportList[position]
        val label = report["prediction"] as? String ?: "No Prediction"
        val timestamp = report["timestamp"] as? Long ?: 0L
        val status = report["status_of_visit"] as? String ?: "Unknown"

        holder.predictionLabel.text = "Prediction: $label"
        holder.timestamp.text = "Timestamp: $timestamp"
        holder.status.text = "Status: $status"

        val imageUrl = report["image_url"] as? String ?: ""
        //add from here
        if (imageUrl.isNullOrEmpty()) {
            // Handle missing or empty image URL (e.g., use a placeholder image)
            holder.predictionImage.setImageResource(R.drawable.ic_launcher_background)
        } else {
            // Load image from Firebase Storage
            loadImageFromFirebase(imageUrl, holder.predictionImage)
        }
        //till here

        //before adding
        // Load image from Firebase Storage
//        loadImageFromFirebase(imageUrl, holder.predictionImage)
    }

    override fun getItemCount(): Int {
        return reportList.size
    }

    private fun loadImageFromFirebase(imageUrl: String, imageView: ImageView) {
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_launcher_background)
            return
        }

        // Load image using Glide
        Glide.with(imageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background) // Optional placeholder
            .error(R.drawable.ic_launcher_background) // Optional error image
            .into(imageView)
    }

//    private fun loadImageFromFirebase(imageUrl: String, imageView: ImageView) {
//        //add from here
//        if (imageUrl.isNullOrEmpty()) {
//            // Handle empty or null imageUrl (you can log this or set a default image)
//            imageView.setImageResource(R.drawable.ic_launcher_background)
//            return
//        }
//        //till here
//        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child(imageUrl)
//
//        val localFile = File.createTempFile("tempImage", "jpg")
//        storageRef.getFile(localFile)
//            .addOnSuccessListener {
//                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
//                imageView.setImageBitmap(bitmap)
//            }
//            .addOnFailureListener {
//                imageView.setImageResource(R.drawable.ic_launcher_background)
//            }
//    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val predictionImage: ImageView = itemView.findViewById(R.id.predictionImage)
        val predictionLabel: TextView = itemView.findViewById(R.id.predictionLabel)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val status: TextView = itemView.findViewById(R.id.status)
    }
}
