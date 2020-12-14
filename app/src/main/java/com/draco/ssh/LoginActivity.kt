package com.draco.ssh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    private lateinit var address: TextInputEditText
    private lateinit var port: TextInputEditText
    private lateinit var username: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var start: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        address = findViewById(R.id.address)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        start = findViewById(R.id.start)

        getPreferences(Context.MODE_PRIVATE).apply {
            address.setText(getString("address", "192.168.0.1"))
            port.setText(getString("port", "22"))
            username.setText(getString("username", "root"))
            password.setText(getString("password", ""))
        }

        start.setOnClickListener {
            val intent = Intent(this, ShellActivity::class.java)
                .putExtra("address", address.text.toString())
                .putExtra("port", port.text.toString())
                .putExtra("username", username.text.toString())
                .putExtra("password", password.text.toString())
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        getPreferences(Context.MODE_PRIVATE).apply {
            with (edit()) {
                putString("address", address.text.toString())
                putString("port", port.text.toString())
                putString("username", username.text.toString())
                putString("password", password.text.toString())
                apply()
            }
        }
    }
}