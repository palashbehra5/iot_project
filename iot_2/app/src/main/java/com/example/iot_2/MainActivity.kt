package com.example.iot_2

import android.content.Context
import android.content.res.Configuration
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.system.Os.socket
import android.text.format.Formatter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import kotlinx.coroutines.delay
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var state : String
    private var p2pAddress: String? = null
    private lateinit var devIPAddress : String
    private lateinit var senderState : String
    private var numPacketsSent = 0
    private var numPacketsReceived = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = wifiP2pManager.initialize(this, mainLooper, null)

        val btnSender = findViewById<Button>(R.id.btnSender)
        val btnReceiver = findViewById<Button>(R.id.btnReceiver)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

        val etIPAddress = findViewById<EditText>(R.id.etIPAddress)

        val tv1 = findViewById<TextView>(R.id.textView7)
        val tv2 = findViewById<TextView>(R.id.textView9)
        val tv3 = findViewById<TextView>(R.id.textView)
        val tvTuplesReceived = findViewById<TextView>(R.id.tvTuplesReceived)
        val tvTuplesSent = findViewById<TextView>(R.id.tvTuplesSent)
        val tvIPAddress = findViewById<TextView>(R.id.tvDeviceIPAddress)

        val ivResult = findViewById<ImageView>(R.id.ivResult)

        btnConnect.visibility = View.GONE
        btnStop.visibility = View.GONE
        btnRefresh.visibility = View.GONE
        ivResult.visibility = View.GONE
        tv1.visibility = View.GONE
        tv2.visibility = View.GONE
        tv3.visibility = View.GONE
        tvTuplesReceived.visibility = View.GONE
        tvTuplesSent.visibility = View.GONE
        tvIPAddress.visibility = View.GONE
        etIPAddress.visibility = View.GONE

        btnSender.setOnClickListener(){

            btnStop.isEnabled = false
            btnSender.isEnabled = false
            btnReceiver.isEnabled = false
            etIPAddress.visibility = View.VISIBLE
            btnConnect.visibility = View.VISIBLE
            btnStop.visibility = View.VISIBLE
            tvTuplesSent.visibility = View.VISIBLE
            tv2.visibility = View.VISIBLE
            state = "Sender"

        }

        btnReceiver.setOnClickListener(){

            btnSender.isEnabled = false
            btnReceiver.isEnabled = false
            btnRefresh.visibility = View.VISIBLE
            ivResult.visibility = View.VISIBLE
            tvTuplesReceived.visibility = View.VISIBLE
            tv1.visibility = View.VISIBLE
            tv3.visibility = View.VISIBLE
            tvIPAddress.visibility = View.VISIBLE
            state = "Receiver"

        }

        btnConnect.setOnClickListener(){

            btnConnect.isEnabled = false
            btnStop.isEnabled = true
            senderState = "Connected"

             /*

             TODO

             > Start sender side thread
             > Get GPS + TimeStamp
             > Create String
             > Pass as bytes
             > Socket : etIPAddress : 12345
             > Do while senderState is not "Stop"
             > Do this on a thread/ use coroutines

             */

            thread {

                val clientSocket = Socket(etIPAddress.text.toString(), 12345)

                // Holds tuples as strings
                val result = "Dummy"

                while (senderState == "Connected") {

                    // getGPS, getRSSI
                    // Create String "Result"

                    clientSocket.getOutputStream().write(result.toByteArray())
                    numPacketsSent += 1
                    tvTuplesSent.text = numPacketsSent.toString()
                    Thread.sleep(1000)

                }

                // Send EXIT Message
                clientSocket.getOutputStream().write("END".toByteArray())

            }.start()
        }

        btnStop.setOnClickListener(){

            btnStop.isEnabled = false
            senderState = "Stop"

        }

        btnRefresh.setOnClickListener(){

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            tvIPAddress.text = ipAddress

        }

    }

}