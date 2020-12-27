package com.draco.ssh.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.draco.ssh.utils.Shell
import java.io.File

class ShellActivityViewModel : ViewModel() {
    private val outputText = MutableLiveData<String>()
    fun getOutputText(): LiveData<String> = outputText

    fun updateOutputText(file: File) {
        val out = readOutputFile(file)
        val currentText = getOutputText().value
        if (out != currentText)
            outputText.postValue(out)
    }

    private fun readOutputFile(file: File): String {
        val out = ByteArray(Shell.MAX_OUTPUT_BUFFER_SIZE)
        file.inputStream().use {
            val size = it.channel.size()

            if (size <= out.size)
                return String(it.readBytes())

            val newPos = (it.channel.size() - out.size)
            it.channel.position(newPos)
            it.read(out)
        }

        return String(out)
    }
}