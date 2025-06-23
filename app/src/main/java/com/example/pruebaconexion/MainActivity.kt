package com.example.pruebaconexion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_CODE = 1

    private lateinit var btnConnect: Button

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar y pedir permisos
        if (!hasPermissions()) {
            solicitarPermiso()
        }

        btnConnect = findViewById(R.id.btnConnect)
        val btnMensaje = findViewById<Button>(R.id.btnMensaje)

        btnConnect.setOnClickListener {
            if (!hasPermissions()) {
                Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                solicitarPermiso()
                return@setOnClickListener
            }

        }

        btnMensaje.setOnClickListener {
            if (!hasPermissions()) {
                Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                solicitarPermiso()
                return@setOnClickListener
            }

            Toast.makeText(this, "Lanzando Servicio", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ServicioConexion::class.java)
            startService(intent)
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

}
