package com.example.ridenowapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PathFinderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var startMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var polyline: Polyline? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_path_finder)

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("PathFinderActivity", "Map is ready")

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
        val keyParam = "AIzaSyDKbJODJVfzfvDyy1fdWcapiW1pxn6sLm4"
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
}