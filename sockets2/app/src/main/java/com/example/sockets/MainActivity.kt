package com.example.sockets

import android.content.ContentValues.TAG
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var flag = 1
    private var serverThread: Thread? = null
    private var server: ServerSocket? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvPacketsReceived = findViewById<TextView>(R.id.tvPacketsReceived)

        // Initialize the server variable
        server = ServerSocket(8888)

        var count = 0

        if (serverThread == null) {
            serverThread = thread{

                while(true){

                    println("Accepting!!!!")
                    val socket = server!!.accept()
                    val response = socket.getInputStream().readAllBytes().toString()
                    count+=1
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        tvPacketsReceived.text = count.toString()
                    }
                    socket.close()

                    if(response=="END") break

                }

                val context = applicationContext
                val message = "$count packets received"
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, message, duration)
                toast.show()
                flag = 0

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.close()
    }

    fun readAllBytes(inputStream: InputStream): String {
        val buffer =  {
            val initialSize = 8192
            var bytesRead: Int
            val data = ByteArray(initialSize)
            val output = StringBuilder()
            while (inputStream.read(data, 0, initialSize).also { bytesRead = it } != -1) {
                output.append(String(data, 0, bytesRead, StandardCharsets.UTF_8))
            }
            output.toString()
        }
        return buffer.toString()
    }

}
