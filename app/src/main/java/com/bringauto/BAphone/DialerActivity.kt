package com.bringauto.BAphone

import android.Manifest.permission.CALL_PHONE
import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
import android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.core.net.toUri
import com.bringauto.BAphone.databinding.ActivityDialerBinding

class DialerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDialerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.phoneNumberInput.setText(intent?.data?.schemeSpecificPart)
    }

    override fun onStart() {
        super.onStart()
        offerReplacingDefaultDialer()

        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        binding.urlInput.setText(sharedPreferences.getString("url", "").toString(), TextView.BufferType.EDITABLE)
        binding.carIdInput.setText(sharedPreferences.getInt("carId", 0).toString(), TextView.BufferType.EDITABLE)
        binding.usernameInput.setText(sharedPreferences.getString("username", "").toString(), TextView.BufferType.EDITABLE)
        binding.passwordInput.setText(sharedPreferences.getString("password", "").toString(), TextView.BufferType.EDITABLE)

        binding.button.setOnClickListener {
            saveSettings()
            saveToast()
        }

        binding.phoneNumberInput.setOnEditorActionListener { _, _, _ ->
            //makeCall()
            true
        }
    }

    private fun saveSettings() {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("url", binding.urlInput.text.toString())
        editor.putInt("carId", binding.carIdInput.text.toString().toInt())
        editor.putString("username", binding.usernameInput.text.toString())
        editor.putString("password", binding.passwordInput.text.toString())
        editor.apply()
    }

    private fun saveToast() {
        val text = "Settings saved"
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(this, text, duration)
        toast.show()
    }

    private fun makeCall() {
        if (checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED) {
            val uri = "tel:${binding.phoneNumberInput.text}".toUri()
            startActivity(Intent(Intent.ACTION_CALL, uri))
        } else {
            requestPermissions(this, arrayOf(CALL_PHONE), REQUEST_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && PERMISSION_GRANTED in grantResults) {
            makeCall()
        }
    }

    private fun offerReplacingDefaultDialer() {
        if (getSystemService(TelecomManager::class.java).defaultDialerPackage != packageName) {
            Intent(ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                .let(::startActivity)
        }
    }

    companion object {
        const val REQUEST_PERMISSION = 0
    }
}