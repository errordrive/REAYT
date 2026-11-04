package com.apkshield.protector.ui

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.apkshield.protector.R
import com.apkshield.protector.engine.ProtectOptions
import com.apkshield.protector.engine.ProtectorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var targetLabel: TextView
    private lateinit var pickApkButton: Button
    private lateinit var protectButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var logView: TextView

    private lateinit var optStrings: CheckBox
    private lateinit var optRename: CheckBox
    private lateinit var optIntegrity: CheckBox
    private lateinit var optAntiDebug: CheckBox
    private lateinit var optResEnc: CheckBox
    private lateinit var optResign: CheckBox
    private lateinit var optPackDex: CheckBox
    private lateinit var optDeepObf: CheckBox

    private var selectedApk: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        targetLabel = findViewById(R.id.targetLabel)
        pickApkButton = findViewById(R.id.pickApkButton)
        protectButton = findViewById(R.id.protectButton)
        progress = findViewById(R.id.progress)
        logView = findViewById(R.id.logView)
        optStrings = findViewById(R.id.optStrings)
        optRename = findViewById(R.id.optRename)
        optIntegrity = findViewById(R.id.optIntegrity)
        optAntiDebug = findViewById(R.id.optAntiDebug)
        optResEnc = findViewById(R.id.optResEnc)
        optResign = findViewById(R.id.optResign)
        optPackDex = findViewById(R.id.optPackDex)
        optDeepObf = findViewById(R.id.optDeepObf)

        pickApkButton.setOnClickListener { pickApk() }
        protectButton.setOnClickListener { startProtection() }
    }

    private fun pickApk() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "application/vnd.android.package-archive"
        @Suppress("DEPRECATION")
        startActivityForResult(i, REQ_PICK)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val name = queryName(uri)
            val copy = File(cacheDir, "in_${System.currentTimeMillis()}_$name")
            contentResolver.openInputStream(uri)!!.use { input ->
                FileOutputStream(copy).use { input.copyTo(it) }
            }
            withContext(Dispatchers.Main) {
                selectedApk = copy
                targetLabel.text = "${copy.name} (${copy.length() / 1024} KB)"
                protectButton.isEnabled = true
            }
        }
    }

    private fun queryName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return "target.apk"
    }

    private fun startProtection() {
        val src = selectedApk ?: return
        val opts = ProtectOptions(
            encryptStrings = optStrings.isChecked,
            renameSymbols = optRename.isChecked,
            injectIntegrityGuard = optIntegrity.isChecked,
            antiInstrumentation = optAntiDebug.isChecked,
            encryptResources = optResEnc.isChecked,
            resign = optResign.isChecked,
            packDex = optPackDex.isChecked,
            deepObfuscate = optDeepObf.isChecked
        )

        protectButton.isEnabled = false
        pickApkButton.isEnabled = false
        progress.visibility = ProgressBar.VISIBLE
        logView.text = ""

        lifecycleScope.launch(Dispatchers.IO) {
            val lines = StringBuilder()
            fun log(s: String) {
                lines.append(s).append('\n')
                runOnUiThread { logView.text = lines.toString() }
            }

            try {
                val runtimeDex = ProtectorEngine.loadRuntimeDex(javaClass.classLoader!!)
                val certSha256 = ProtectorEngine.loadCertSha256(javaClass.classLoader!!)
                log("runtime dex loaded (${runtimeDex.size} bytes)")
                log("signing cert sha256=${certSha256.take(16)}...")

                val outDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ApkShield"
                )
                outDir.mkdirs()
                val out = File(outDir, "protected_${System.currentTimeMillis()}_${src.name}")
                val engine = ProtectorEngine(::log)
                val result = engine.protect(src, out, opts, runtimeDex, certSha256)
                log("DONE -> ${result.outputApk.absolutePath}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Protected: ${result.outputApk.name}", Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                log("FAILED: ${t.javaClass.simpleName}: ${t.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Protection failed: ${t.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                runOnUiThread {
                    progress.visibility = ProgressBar.GONE
                    protectButton.isEnabled = true
                    pickApkButton.isEnabled = true
                }
            }
        }
    }

    companion object {
        private const val REQ_PICK = 0x5101
    }
}
