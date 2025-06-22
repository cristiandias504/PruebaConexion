package com.example.pruebaconexion

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_CODE = 1

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    //Modificacion 3 desde el MAC y desde Windows
    private val deviceName = "ESP32-BT-Slave"
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar SPP
    private var btSocket: BluetoothSocket? = null
    private lateinit var btDevice: BluetoothDevice
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar y pedir permisos
        if (!hasPermissions()) {
            solicitarPermiso()
        }

        val btnConnect = findViewById<Button>(R.id.btnConnect)

        btnConnect.setOnClickListener {
            if (!hasPermissions()) {
                Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                solicitarPermiso()
                return@setOnClickListener
            }

            if (btAdapter == null) {
                Toast.makeText(this, "Bluetooth no está disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!btAdapter.isEnabled) {
                Toast.makeText(this, "Activa el Bluetooth", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val pairedDevices: Set<BluetoothDevice> = btAdapter.bondedDevices
                btDevice = pairedDevices.find { it.name == deviceName }
                    ?: run {
                        Toast.makeText(this, "ESP32 no emparejado", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                connectToDevice()
            } else {
                Toast.makeText(this, "No tienes permiso BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos otorgados", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "La app necesita permisos para funcionar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun solicitarPermiso() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE)
    }

    private fun connectToDevice() {
        Thread {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread {
                    Toast.makeText(this, "Sin permiso BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }

            try {
                btSocket = btDevice.createRfcommSocketToServiceRecord(sppUUID)
                btSocket?.connect()
                runOnUiThread {
                    Toast.makeText(this, "Conectado al ESP32", Toast.LENGTH_SHORT).show()
                }

                // Aquí puedes enviar datos
                btSocket?.outputStream?.write("Hola desde Android\n".toByteArray())

            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("Bluetooth", "Error: ", e)
            }
        }.start()
    }
}
