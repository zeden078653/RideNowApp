package com.example.ridenowapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class LocationPermissionActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        // Hide the action bar
        supportActionBar?.hide()

        // Set up allow button
        findViewById<MaterialButton>(R.id.allowButton).setOnClickListener {
            requestLocationPermission()
        }

        // Set up don't allow button
        findViewById<MaterialButton>(R.id.dontAllowButton).setOnClickListener {
            // Show a toast explaining why location is important
            Toast.makeText(
                this,
                "Location permission is required to show nearby transport options",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, proceed to main activity
                startMainActivity()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Show an explanation to the user
                Toast.makeText(
                    this,
                    "Location permission is required to show nearby transport options",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed to main activity
                    startMainActivity()
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Location permission is required for full app functionality",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}