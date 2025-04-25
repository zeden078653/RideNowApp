package com.example.ridenowapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RideHistoryActivity : AppCompatActivity() {

    private lateinit var historyList: RecyclerView
    private lateinit var emptyHistoryText: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_history)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        historyList = findViewById(R.id.historyList)
        emptyHistoryText = findViewById(R.id.emptyHistoryText)

        // Setup RecyclerView
        historyList.layoutManager = LinearLayoutManager(this)
        loadRideHistory()
    }

    private fun loadRideHistory() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    emptyHistoryText.visibility = View.VISIBLE
                    historyList.visibility = View.GONE
                } else {
                    emptyHistoryText.visibility = View.GONE
                    historyList.visibility = View.VISIBLE

                    val rides = documents.mapNotNull { doc ->
                        try {
                            val cabType = doc.getString("cabType") ?: return@mapNotNull null
                            val fromLocation = doc.getString("fromLocation") ?: return@mapNotNull null
                            val toLocation = doc.getString("toLocation") ?: return@mapNotNull null
                            val distance = doc.getDouble("distance") ?: return@mapNotNull null
                            val totalFare = doc.getDouble("totalFare") ?: return@mapNotNull null
                            val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: return@mapNotNull null

                            RideHistoryItem(
                                cabType = cabType,
                                fromLocation = fromLocation,
                                toLocation = toLocation,
                                distance = distance,
                                totalFare = totalFare,
                                timestamp = timestamp
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    historyList.adapter = RideHistoryAdapter(rides)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading ride history: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    data class RideHistoryItem(
        val cabType: String,
        val fromLocation: String,
        val toLocation: String,
        val distance: Double,
        val totalFare: Double,
        val timestamp: java.util.Date
    )

    inner class RideHistoryAdapter(private val rides: List<RideHistoryItem>) :
        RecyclerView.Adapter<RideHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cabType: TextView = view.findViewById(R.id.cabType)
            val rideDate: TextView = view.findViewById(R.id.rideDate)
            val rideRoute: TextView = view.findViewById(R.id.rideRoute)
            val rideDistance: TextView = view.findViewById(R.id.rideDistance)
            val rideFare: TextView = view.findViewById(R.id.rideFare)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ride_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ride = rides[position]
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val fareFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

            holder.cabType.text = ride.cabType
            holder.rideDate.text = dateFormat.format(ride.timestamp)
            holder.rideRoute.text = "${ride.fromLocation} → ${ride.toLocation}"
            holder.rideDistance.text = "Distance: ${String.format("%.1f", ride.distance)} km"
            holder.rideFare.text = "Fare: ${fareFormat.format(ride.totalFare)}"
        }

        override fun getItemCount() = rides.size
    }
}