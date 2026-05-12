package com.safegps.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.safegps.databinding.ActivityMainBinding
import com.safegps.service.LocationForegroundService
import com.safegps.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            checkBackgroundLocation()
        } else {
            Toast.makeText(this, "Permissions required for tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupIntervalPicker()
        setupToggle()
        updateStatus()
    }

    private fun setupToggle() {
        binding.switchGps.isChecked = isServiceRunning()
        binding.switchGps.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasLocationPermissions()) {
                    checkBackgroundLocation()
                } else {
                    requestPermissions()
                    binding.switchGps.isChecked = false
                }
            } else {
                controlService(LocationForegroundService.ACTION_STOP)
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showBackgroundLocationDialog()
            } else {
                startTracking()
            }
        } else {
            startTracking()
        }
    }

    private fun showBackgroundLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage("To track location while the app is closed, please select 'Allow all the time' in settings.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchGps.isChecked = false
            }
            .show()
    }

    private fun startTracking() {
        controlService(LocationForegroundService.ACTION_START)
        updateStatus()
    }

    private fun controlService(action: String) {
        Intent(this, LocationForegroundService::class.java).also {
            it.action = action
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            } else {
                startService(it)
            }
        }
    }

    private fun setupIntervalPicker() {
        val intervals = listOf(
            "5 minutes" to 5 * 60 * 1000L,
            "10 minutes" to 10 * 60 * 1000L,
            "30 minutes" to 30 * 60 * 1000L,
            "1 hour" to 60 * 60 * 1000L
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerInterval.adapter = adapter

        binding.spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                lifecycleScope.launch {
                    PreferencesManager.saveInterval(this@MainActivity, intervals[pos].second)
                    if (isServiceRunning()) {
                        controlService(LocationForegroundService.ACTION_START) // Restart with new interval
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (LocationForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateStatus() {
        if (isServiceRunning()) {
            binding.textStatus.text = "Status: Tracking Active"
            binding.textStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.textStatus.text = "Status: Inactive"
            binding.textStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.switchGps.isChecked = isServiceRunning()
        updateStatus()
    }
}
