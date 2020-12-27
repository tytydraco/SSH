package com.draco.ssh.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.File

class LoginActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    init {
        prepareKeys()
    }

    private fun prepareKeys() {
        if (File("${context.filesDir}/id_rsa").exists() &&
            File("${context.filesDir}/id_rsa.pub").exists())
            return

        KeyPair.genKeyPair(JSch(), KeyPair.RSA).apply {
            writePrivateKey("${context.filesDir}/id_rsa")
            writePublicKey("${context.filesDir}/id_rsa.pub", null)
            dispose()
        }
    }
}