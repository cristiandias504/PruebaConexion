package com.example.pruebaconexion

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.util.UUID

class ServicioConexion : Service() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val deviceName = "ESP32"
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar SPP
    private var btSocket: BluetoothSocket? = null
    private lateinit var btDevice: BluetoothDevice
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private var contador = 0
    private var tiempoInicio = 0L

    private var servicioActivo = false
    private var conexionEstablecida = false

    override fun onCreate() {
        super.onCreate()
        crearCanal()
        iniciarComoForeground()
        connectToDevice()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Canal Principal"
            val descripcion = "Canal para notificaciones de ejemplo"
            val importancia = NotificationManager.IMPORTANCE_DEFAULT
            val canal = NotificationChannel("canal_id", nombre, importancia).apply {
                description = descripcion
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    private fun iniciarComoForeground() {
        val notification = NotificationCompat.Builder(this, "canal_id")
            .setContentTitle("Conexión activa")
            .setContentText("Servicio Bluetooth en ejecución")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun connectToDevice() {
        Thread {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Sin permiso BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }

            try {
                val pairedDevices: Set<BluetoothDevice>? = btAdapter?.bondedDevices
                btDevice = pairedDevices?.find { it.name == deviceName }
                    ?: run {
                        Toast.makeText(this, "ESP32 no emparejado", Toast.LENGTH_SHORT).show()
                        return@Thread
                    }

                btSocket = btDevice.createRfcommSocketToServiceRecord(sppUUID)
                btSocket?.connect()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Conectado Desde el Servicio", Toast.LENGTH_SHORT).show()
                }
                conexionEstablecida = true
                iniciarMensajePeriodico()

            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("Bluetooth", "Error: ", e)
            }
        }.start()
    }

    private fun iniciarMensajePeriodico() {
        if (!servicioActivo){
            if(conexionEstablecida){
                servicioActivo = true
                handler = Handler(Looper.getMainLooper())
                runnable = object : Runnable {
                    override fun run() {
                        enviarMensaje()
                        handler.postDelayed(this, 10_000) // cada 30 segundos
                    }
                }
                tiempoInicio = System.currentTimeMillis()

                handler.post(runnable)
            }
        }
    }

    private fun enviarMensaje(){
        if (btSocket != null && btSocket!!.isConnected) {
            try {
                btSocket?.outputStream?.write("Hola desde el Servicio\n".toByteArray())
            } catch (e: IOException){
                Toast.makeText(this, "Error al enviar mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Bluetooth", "Error al enviar", e)
            }
        } else {
            Toast.makeText(this, "Dispositivo No Conectado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        servicioActivo = false
        contador = 0
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}