package com.draco.ssh.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draco.ssh.utils.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class ShellActivityViewModel : ViewModel() {
    private val outputText = MutableLiveData<String>()
    fun getOutputText(): LiveData<String> = outputText

    private val shell = MutableLiveData<Shell>()
    fun getShell(): LiveData<Shell> = shell
    fun setShell(newShell: Shell) {
        shell.value = newShell
    }

    fun initializeClient() {
        viewModelScope.launch(Dispatchers.IO) {
            shell.value!!.initializeClient()
        }
    }

    fun startOutputFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive && shell.value!!.session.isConnected) {
                val out = readOutputFile(shell.value!!.outputBufferFile)
                val currentText = outputText.value
                if (out != currentText)
                    outputText.postValue(out)
                Thread.sleep(Shell.OUTPUT_BUFFER_DELAY_MS)
            }
        }
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