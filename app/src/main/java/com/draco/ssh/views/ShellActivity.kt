package com.draco.ssh.views

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.draco.ssh.BuildConfig
import com.draco.ssh.R
import com.draco.ssh.viewmodels.ShellActivityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.jcraft.jsch.*
import kotlinx.coroutines.*

class ShellActivity : AppCompatActivity() {
    private val viewModel: ShellActivityViewModel by viewModels()

    private lateinit var progress: ProgressBar
    private lateinit var command: TextInputEditText
    private lateinit var output: MaterialTextView
    private lateinit var outputScrollView: ScrollView

    private lateinit var errorDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shell)

        progress = findViewById(R.id.progress)
        command = findViewById(R.id.command)
        output = findViewById(R.id.output)
        outputScrollView = findViewById(R.id.output_scrollview)

        errorDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error_title)
            .setPositiveButton(R.string.error_disconnect) { _, _ -> finish() }
            .setCancelable(false)
            .create()

        command.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val text = command.text.toString()
                command.text = null
                viewModel.shell.send(text)

                return@setOnKeyListener true
            }

            return@setOnKeyListener false
        }

        setupShell()
    }

    private fun setupShell() {
        val address = intent.getStringExtra("address")!!
        val port = try {
            Integer.parseInt(intent.getStringExtra("port")!!)
        } catch (_: Exception) { 22 }
        val username = intent.getStringExtra("username")!!
        val password = intent.getStringExtra("password")!!

        viewModel.connectClientAndStartOutputThread(username, address, port, password)
        viewModel.shell.getReady().observe(this) {
            if (it == true) {
                progress.visibility = View.INVISIBLE
                command.isEnabled = true
            }
        }

        viewModel.shell.error.observe(this) { error(it) }
        viewModel.getOutputText().observe(this) { output.text = it }
    }

    private fun error(exceptionMessage: String) {
        errorDialog.run {
            setMessage(exceptionMessage)
            show()
        }
    }

    override fun onBackPressed() {
        viewModel.shell.deinitialize()
        super.onBackPressed()
    }

    override fun onDestroy() {
        errorDialog.dismiss()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share -> {
                try {
                    val uri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        viewModel.shell.outputBufferFile
                    )
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.shell, menu)
        return super.onCreateOptionsMenu(menu)
    }
}