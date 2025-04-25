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
import com.google.android.material.textfield.TextInputLayout

class SignInActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Hide the action bar
        supportActionBar?.hide()

        // Set up sign in button
        findViewById<MaterialButton>(R.id.signInButton).setOnClickListener {
            val emailPhone = findViewById<TextInputLayout>(R.id.emailPhoneInput)
                .editText?.text.toString()

            if (emailPhone.isNotEmpty()) {
                // TODO: Implement actual sign in
                checkLocationPermission()
            } else {
                Toast.makeText(this, "Please enter email or phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up social sign in buttons
        findViewById<MaterialButton>(R.id.googleSignInButton).setOnClickListener {
            // TODO: Implement Google Sign In
            checkLocationPermission()
        }

        findViewById<MaterialButton>(R.id.appleSignInButton).setOnClickListener {
            // TODO: Implement Apple Sign In
            checkLocationPermission()
        }

        findViewById<MaterialButton>(R.id.xSignInButton).setOnClickListener {
            // TODO: Implement X Sign In
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Location permission already granted, proceed to main activity
            startMainActivity()
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
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
                    startMainActivity()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for the app to function properly",
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