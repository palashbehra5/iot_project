package com.example.sockets

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.ServerSocket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var flag = 1

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvPacketsReceived = findViewById<TextView>(R.id.tvPacketsReceived)

        val server = ServerSocket(12345)
        var count = 0

        // Launching background thread
        GlobalScope.launch{

            while(true){

                val socket = server.accept()
                val response = socket.getInputStream().readAllBytes().toString()
                count+=1
                tvPacketsReceived.text = count.toString()
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

        // Busy Waiting
        while(flag==1){}
    }
}