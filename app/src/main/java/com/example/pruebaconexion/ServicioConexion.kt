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

    private val deviceName = "ESP32"
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar SPP
    private var btSocket: BluetoothSocket? = null
    private lateinit var btDevice: BluetoothDevice
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

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

                if (btAdapter == null) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Bluetooth no está disponible", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@Thread
                }

                if (!btAdapter.isEnabled) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Activa el Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

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
                // Crear y enviar el broadcast
                val intentBroadcast = Intent("com.example.pruebaconexion.MENSAJE").apply {
                    setPackage(packageName)
                    putExtra("Mensaje", "Mensaje enviado desde el Servicio")
                }

                Log.d("ServicioConexion", "Enviando broadcast...")
                sendBroadcast(intentBroadcast)

                conexionEstablecida = true
                iniciarMensajePeriodico()
                iniciarRecepcionMensajes()

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
                        handler.postDelayed(this, 10_000) // cada 10 segundos
                    }
                }
                Log.d("ServicioConexion", "Handler Iniciado")

                tiempoInicio = System.currentTimeMillis()

                handler.post(runnable)
            }
        }
    }

    private fun enviarMensaje(){
        if (btSocket != null && btSocket!!.isConnected) {
            try {
                btSocket?.outputStream?.write("Hola desde el Servicio\n".toByteArray())
                Log.d("ServicioConexion", "Mensaje enviado al ESP32")
            } catch (e: IOException){
                Log.e("ServicioConexion", "Error al enviar", e)

            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Dispositivo No Conectado", Toast.LENGTH_SHORT).show()
            }
            Log.d("ServicioConexion", "Dispositivo No Conectado")
        }
    }

    private fun iniciarRecepcionMensajes() {
        Thread {
            try {
                val inputStream = btSocket?.inputStream
                val buffer = ByteArray(1024)
                val mensajeAcumulado = StringBuilder()

                while (btSocket != null && btSocket!!.isConnected) {
                    val byte = inputStream?.read() ?: -1
                    if (byte != -1) {
                        val char = byte.toChar()

                        if (char == '\n') {
                            val mensajeCompleto = mensajeAcumulado.toString().trim()
                            Log.d("Bluetooth", "Mensaje recibido: $mensajeCompleto")

                            // Enviar a la actividad si quieres
                            //val intent = Intent("com.example.pruebaconexion.MENSAJE").apply {
                            //    putExtra("Mensaje", mensajeCompleto)
                            //}
                            //sendBroadcast(intent)

                            mensajeAcumulado.clear() // reinicia para el siguiente mensaje
                        } else {
                            mensajeAcumulado.append(char)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al recibir datos", e)
            }
        }.start()
    }



    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        servicioActivo = false
        super.onDestroy()

        // Cierra el socket Bluetooth
        try {
            btSocket?.close()
            Log.d("ServicioConexion", "BluetoothSocket cerrado")
        } catch (e: IOException) {
            Log.e("ServicioConexion", "Error al cerrar el socket", e)
        }

        Log.d("ServicioConexion", "Finalizando Servicio")

        val intentBroadcast = Intent("com.example.pruebaconexion.MENSAJE").apply {
            setPackage(packageName)
            putExtra("Mensaje", "Conexión Bluetooth finalizada")
        }

        sendBroadcast(intentBroadcast)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}