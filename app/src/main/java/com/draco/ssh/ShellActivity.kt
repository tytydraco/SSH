package com.draco.ssh

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.jcraft.jsch.*
import java.io.File
import java.io.PrintStream

class ShellActivity : AppCompatActivity() {
    companion object {
        const val MAX_CONNECTION_TIMEOUT = 60 * 1000
        const val MAX_OUTPUT_BUFFER_SIZE = 1024 * 4
        const val OUTPUT_BUFFER_DELAY_MS = 100L
    }

    private lateinit var progress: ProgressBar
    private lateinit var command: TextInputEditText
    private lateinit var output: MaterialTextView
    private lateinit var outputScrollView: ScrollView

    private lateinit var address: String
    private var port = 22
    private lateinit var username: String
    private lateinit var password: String

    private lateinit var jSch: JSch
    private lateinit var session: Session
    private lateinit var channel: ChannelShell

    private lateinit var outputBufferFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shell)

        progress = findViewById(R.id.progress)
        command = findViewById(R.id.command)
        output = findViewById(R.id.output)
        outputScrollView = findViewById(R.id.output_scrollview)

        address = intent.getStringExtra("address")!!
        try {
            port = Integer.parseInt(intent.getStringExtra("port")!!)
        } catch (_: Exception) { }
        username = intent.getStringExtra("username")!!
        password = intent.getStringExtra("password")!!

        /* Store the buffer locally to avoid an OOM error */
        outputBufferFile = File.createTempFile("buffer", ".txt").apply {
            deleteOnExit()
        }

        jSch = JSch().apply {
            addIdentity("$filesDir/id_rsa")
        }

        session = jSch.getSession(username, address, port).apply {
            setPassword(password)
            setConfig("StrictHostKeyChecking", "no")
        }

        initializeClient()

        command.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val text = command.text.toString()
                command.text = null
                sendCommand(text)

                return@setOnKeyListener true
            }

            return@setOnKeyListener false
        }
    }

    private fun sendCommand(thisCommand: String) {
        if (!session.isConnected) {
            error(getString(R.string.error_not_connected))
            return
        }

        Thread {
            PrintStream(channel.outputStream).apply {
                println(thisCommand)
                flush()
            }
        }.start()
    }

    private fun readEndOfFile(file: File): String {
        val out = ByteArray(MAX_OUTPUT_BUFFER_SIZE)
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

    private fun updateOutputFeed() {
        val out = readEndOfFile(outputBufferFile)
        val currentText = output.text.toString()
        if (out != currentText) {
            runOnUiThread {
                output.text = out
                outputScrollView.post {
                    outputScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun startOutputFeed() {
        Thread {
            while (!channel.isClosed) {
                updateOutputFeed()
                Thread.sleep(OUTPUT_BUFFER_DELAY_MS)
            }
            updateOutputFeed()
        }.start()
    }

    private fun initializeClient() {
        Thread {
            try {
                /* Connect the session */
                session.connect(MAX_CONNECTION_TIMEOUT)

                /* Initialize the shell channel */
                channel = (session.openChannel("shell") as ChannelShell).apply {
                    outputStream = outputBufferFile.outputStream()
                }

                /* Connect the shell channel */
                channel.connect(MAX_CONNECTION_TIMEOUT)

                /* Start updating the output view */
                startOutputFeed()
                
                /* Unlock UI */
                runOnUiThread {
                    progress.visibility = View.INVISIBLE
                    command.isEnabled = true
                }
            } catch (e: JSchException) {
                e.printStackTrace()

                runOnUiThread {
                    error(e.message!!)
                }
            }
        }.start()
    }

    private fun error(exceptionMessage: String) {
        /* Do not use the activity if it has been destroyed */
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            return

        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.error_title)
            setPositiveButton(R.string.error_disconnect) { _, _ -> finish() }
            setMessage(exceptionMessage)
            setCancelable(false)
            show()
        }
    }

    override fun onDestroy() {
        if (this::channel.isInitialized)
            channel.disconnect()
        if (this::session.isInitialized)
            session.disconnect()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share -> {
                try {
                    val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", outputBufferFile)
                    val intent = Intent(Intent.ACTION_SEND)
                    with (intent) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "file/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Snackbar.make(output, getString(R.string.snackbar_intent_failed), Snackbar.LENGTH_SHORT)
                        .setAction(getString(R.string.dismiss)) {}
                        .show()
            }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.shell, menu)
        return super.onCreateOptionsMenu(menu)
    }
}