package com.example.ridenowapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import kotlin.jvm.java

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Hide the action bar
        supportActionBar?.hide()

        // Setup features
        setupFeature(
            R.id.featureTracking,
            R.drawable.ic_tracking,
            R.string.feature_tracking_title,
            R.string.feature_tracking_desc
        )
        setupFeature(
            R.id.featureTravel,
            R.drawable.ic_travel,
            R.string.feature_travel_title,
            R.string.feature_travel_desc
        )
        setupFeature(
            R.id.featureEco,
            R.drawable.ic_eco,
            R.string.feature_eco_title,
            R.string.feature_eco_desc
        )
        setupFeature(
            R.id.featureApp,
            R.drawable.ic_app,
            R.string.feature_app_title,
            R.string.feature_app_desc
        )

        // Set up Get Started button
        findViewById<MaterialButton>(R.id.getStartedButton).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun setupFeature(containerId: Int, iconRes: Int, titleRes: Int, descRes: Int) {
        val container = findViewById<android.view.View>(containerId)
        container.findViewById<ImageView>(R.id.featureIcon).setImageResource(iconRes)
        container.findViewById<TextView>(R.id.featureTitle).setText(titleRes)
        container.findViewById<TextView>(R.id.featureDescription).setText(descRes)
    }
}