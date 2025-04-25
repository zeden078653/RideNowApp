package com.example.ridenowapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val countryCodeSpinner = findViewById<Spinner>(R.id.countryCodeSpinner)
        val emailField = findViewById<EditText>(R.id.emailField)
        val requestOtpButton = findViewById<Button>(R.id.requestOtpButton)
        val otpField = findViewById<EditText>(R.id.otpField)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Populate the country code spinner
        val countryCodes = arrayOf("+1 (US)", "+44 (UK)", "+91 (India)", "+61 (Australia)", "+81 (Japan)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countryCodeSpinner.adapter = adapter

        // Set India (+91) as the default selection
        countryCodeSpinner.setSelection(countryCodes.indexOf("+91 (India)"))

        // Add a TextWatcher to dynamically validate phone number or email
        emailField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()

                if (input.isDigitsOnly()) {
                    // Validate as phone number
                    if (input.length >= 10) {
                        requestOtpButton.isEnabled = true
                        emailField.error = null
                    } else {
                        requestOtpButton.isEnabled = false
                        emailField.error = "Phone number must be at least 10 digits"
                    }
                } else {
                    // Validate as email address
                    if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                        requestOtpButton.isEnabled = true
                        emailField.error = null
                    } else {
                        requestOtpButton.isEnabled = false
                        emailField.error = "Invalid email address"
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle OTP request
        requestOtpButton.setOnClickListener {
            val selectedCountryCode = countryCodeSpinner.selectedItem.toString().split(" ")[0]
            val input = emailField.text.toString()

            if (input.isDigitsOnly()) {
                val phoneNumber = selectedCountryCode + input
                Toast.makeText(this, "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "OTP sent to email: $input", Toast.LENGTH_SHORT).show()
            }

            // Show OTP field and Login button
            otpField.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
        }

        // Add a TextWatcher to the OTP field to show the "Request OTP" button only after 6 digits
        otpField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                requestOtpButton.visibility = if (s?.length == 6) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle Login button click
        loginButton.setOnClickListener {
            val input = emailField.text.toString()

            if (input.isDigitsOnly()) {
                if (input.length < 10) {
                    Toast.makeText(this, "Phone number must be at least 10 digits", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val selectedCountryCode = countryCodeSpinner.selectedItem.toString().split(" ")[0]
                val phoneNumber = selectedCountryCode + input
                Toast.makeText(this, "Login successful with phone number: $phoneNumber", Toast.LENGTH_SHORT).show()
            } else if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                Toast.makeText(this, "Login successful with email: $input", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid input. Please enter a valid phone number or email.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Extension function to check if a string contains only digits
    private fun CharSequence.isDigitsOnly(): Boolean {
        return this.matches(Regex("\\d+"))
    }
}