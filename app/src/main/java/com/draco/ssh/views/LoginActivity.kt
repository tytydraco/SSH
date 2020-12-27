package com.draco.ssh.views

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.draco.ssh.BuildConfig
import com.draco.ssh.R
import com.draco.ssh.viewmodels.LoginActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginActivityViewModel by viewModels()

    private lateinit var address: TextInputEditText
    private lateinit var port: TextInputEditText
    private lateinit var username: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var pubkey: MaterialButton
    private lateinit var start: ExtendedFloatingActionButton

    private lateinit var encryptedSharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        address = findViewById(R.id.address)
        port = findViewById(R.id.port)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        pubkey = findViewById(R.id.pubkey)
        start = findViewById(R.id.start)

        val masterKeyAlias = MasterKey.Builder(this, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedSharedPrefs = EncryptedSharedPreferences.create(
            this,
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        viewModel.prepareKeys()

        with(encryptedSharedPrefs) {
            address.setText(getString("address", "192.168.0.1"))
            port.setText(getString("port", "22"))
            username.setText(getString("username", "root"))
            password.setText(getString("password", ""))
        }

        pubkey.setOnClickListener {
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File("$filesDir/id_rsa.pub")
                )
                val intent = Intent(Intent.ACTION_SEND)
                with(intent) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        with(encryptedSharedPrefs.edit()) {
            putString("address", address.text.toString())
            putString("port", port.text.toString())
            putString("username", username.text.toString())
            putString("password", password.text.toString())
            apply()
        }
    }
}