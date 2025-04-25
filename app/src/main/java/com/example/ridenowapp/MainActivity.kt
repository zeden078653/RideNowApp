package com.example.ridenowapp

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var startMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var dropPinMarker: Marker? = null
    private var polyline: Polyline? = null
    private val client = OkHttpClient()
    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    data class Booking(
        val userId: String = "",
        val cabType: String = "",
        val fromLocation: String = "",
        val toLocation: String = "",
        val distance: Double = 0.0,
        val baseFare: Double = 0.0,
        val pricePerKm: Double = 0.0,
        val totalFare: Double = 0.0,
        val timestamp: Date = Date()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the vehicle type dropdown
        vehicleTypeSpinner = findViewById(R.id.vehicleTypeSpinner)
        val vehicleOptions = listOf("Cab", "Bus", "Bike")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vehicleTypeSpinner.adapter = adapter

        // Handle dropdown selection
        vehicleTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedVehicle = vehicleOptions[position]
                Toast.makeText(this@MainActivity, "Selected: $selectedVehicle", Toast.LENGTH_SHORT).show()
                // Update ride options or logic based on the selected vehicle
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable zoom controls and my location button
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        // Add a long-click listener to set the destination
        mMap.setOnMapLongClickListener { latLng ->
            setDestination(latLng)
        }

        // Add a drag listener to update the route when the start marker is moved
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                if (marker == startMarker) {
                    updateRoute()
                }
            }
        })

        // Add a camera idle listener to update the drop pin location
        mMap.setOnCameraIdleListener {
            val centerLatLng = mMap.cameraPosition.target
            updateDropPin(centerLatLng)
        }
    }
    private fun showMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    // TODO: Navigate to profile screen
                    Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, RideHistoryActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }


    private fun saveBooking(booking: Booking) {
        db.collection("bookings")
            .add(booking)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving booking: ${e.message}", e)
                Toast.makeText(this, "Error saving booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    setStartLocation(currentLatLng)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    private fun setStartLocation(latLng: LatLng) {
        startMarker?.remove()
        startMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Start Location")
                .draggable(true)
        )
    }

    private fun setDestination(latLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Destination")
        )
        updateRoute()
    }

    private fun updateRoute() {
        val startLatLng = startMarker?.position
        val destinationLatLng = destinationMarker?.position

        if (startLatLng != null && destinationLatLng != null) {
            val url = getDirectionsUrl(startLatLng, destinationLatLng)
            fetchRoute(url)
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val originParam = "origin=${origin.latitude},${origin.longitude}"
        val destParam = "destination=${dest.latitude},${dest.longitude}"
        val keyParam = "key=AIzaSyDKbJODJVfzfvDyy1fdWcapiW1pxn6sLm4" // Use API key from manifest
        return "https://maps.googleapis.com/maps/api/directions/json?$originParam&$destParam&$keyParam"
    }

    private fun fetchRoute(url: String) {
        Thread {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (responseData != null) {
                val jsonObject = JSONObject(responseData)
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")
                    val decodedPath = decodePolyline(points)

                    runOnUiThread {
                        polyline?.remove()
                        polyline = mMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .color(ContextCompat.getColor(this, R.color.primary))
                                .width(10f)
                        )
                    }
                }
            }
        }.start()
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }

        return poly
    }

    private fun updateDropPin(latLng: LatLng) {
        if (dropPinMarker == null) {
            dropPinMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        } else {
            dropPinMarker?.position = latLng
        }

        // Optionally, fetch the address of the location
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0].getAddressLine(0)
            Toast.makeText(this, "Selected Location: $address", Toast.LENGTH_SHORT).show()
        }
    }
}