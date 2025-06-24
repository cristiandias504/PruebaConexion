package com.example.pruebaconexion

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // ID de la solicitud de permiso
    private val REQUEST_PERMISSIONS_CODE = 1

    // Inicializar los botones sin definirlos totalmente
    private lateinit var btnConnect: Button
    private lateinit var btnMensaje: Button

    // Variable estado de conexion
    private var estadoConexion = false

    // Permisos necesarios para el uso del Bluetooth   ***ALGUNOS AUN NO SE USAN*** Nesesarios para scanear
    private val requiredPermissions = arrayOf(
        //Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        //Manifest.permission.ACCESS_FINE_LOCATION,
        //Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val deviceName = "ESP32"
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar SPP
    private var btSocket: BluetoothSocket? = null
    private lateinit var btDevice: BluetoothDevice
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Receptor de Broadcast
    private val receptorMensaje = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mensaje = intent?.getStringExtra("Mensaje") ?: "Sin mensaje"
            Log.d("MainActivity", "Mensaje recibido: $mensaje")
            Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()
            if (mensaje == "Mensaje enviado desde el Servicio") {
                btnConnect.text = getString(R.string.btnDesconectar)
                estadoConexion = true
            } else if (mensaje == "Conexión Bluetooth finalizada") {
                btnConnect.text = getString(R.string.btnConectar)
                estadoConexion = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar permisos
        if (!hasPermissions()) {
            solicitarPermiso()
        }

        btnConnect = findViewById(R.id.btnConnect)
        btnMensaje = findViewById(R.id.btnMensaje)


        btnConnect.setOnClickListener {
            if (!estadoConexion){
                //if (!hasPermissions()) {
                //    Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                //    solicitarPermiso()
                //    return@setOnClickListener
                //}

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Sin permiso BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Sin permiso BLUETOOTH_CONNECT")
                    return@setOnClickListener
                }

                val pairedDevices: Set<BluetoothDevice>? = btAdapter?.bondedDevices
                btDevice = pairedDevices?.find { it.name == deviceName }
                    ?: run {
                        Toast.makeText(this, "ESP32 no emparejado", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "ESP32 no emparejado")
                        return@setOnClickListener
                    }

                Log.d("MainActivity", "Lanzando Servicio")
                val intent = Intent(this, ServicioConexion::class.java)
                startService(intent)
            } else {
                Log.d("MainActivity", "Deteniendo Servicio")
                val intent = Intent(this, ServicioConexion::class.java)
                stopService(intent)
            }
        }

        btnMensaje.setOnClickListener {
            if (!hasPermissions()) {
                Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                solicitarPermiso()
                return@setOnClickListener
            }
            val intent = Intent("com.example.pruebaconexion.MENSAJE").apply {
                setPackage(packageName)
                putExtra("Mensaje", "Mensaje desde el botón")
            }
            Log.d("MainActivity", "Enviando broadcast desde btnMensaje")
            sendBroadcast(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.example.pruebaconexion.MENSAJE")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptorMensaje, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receptorMensaje, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receptorMensaje)
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun solicitarPermiso() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE)
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
}
