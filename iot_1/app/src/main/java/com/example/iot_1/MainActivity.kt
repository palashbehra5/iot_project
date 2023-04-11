package com.example.iot_1

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import java.net.*

// TODO :
//  > Implementing RSSI and GPS fun
//  > Clean Organization
//  > Server Side programming

class MainActivity : AppCompatActivity() {

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var locationManager: LocationManager
    private lateinit var channel: WifiP2pManager.Channel
    private var connectedDevice: WifiP2pDevice? = null
    private var dataList = mutableListOf<String>()
    private lateinit var macAddress: String

    private lateinit var etMacAddress: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnSend: Button
    private lateinit var btnCompile: Button
    private lateinit var tvInfo: TextView

    var state = "Receiver"
    private val LOCATION_PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataList = mutableListOf<String>()
        val serverThread = ServerThread()
        serverThread.start()

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        channel = wifiP2pManager.initialize(this, Looper.getMainLooper(), null)


        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }

        etMacAddress = findViewById<EditText>(R.id.et_mac_address)
        btnConnect = findViewById<Button>(R.id.btn_connect)
        btnSend = findViewById<Button>(R.id.btn_send)
        btnCompile = findViewById<Button>(R.id.btn_compile)
        tvInfo = findViewById<TextView>(R.id.tv_info)

        markButtonDisable(btnSend)
        markButtonDisable(btnCompile)

        btnConnect.setOnClickListener {

            state = "Sender"
            macAddress = etMacAddress.text.toString()
            val device = deviceAddressToWifiP2pDevice(macAddress)
            if (device != null) {
                val config = WifiP2pConfig().apply {
                    deviceAddress = device.deviceAddress
                    groupOwnerIntent = 15 // Max value means this device prefers to be group owner
                }
                wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        tvInfo.text = "Connecting to ${device.deviceName}..."
                    }

                    override fun onFailure(reason: Int) {
                        tvInfo.text = "Failed to connect to ${device.deviceName}. Reason: ${getFailureReason(reason)}."
                    }
                })
            } else {
                tvInfo.text = "Device with MAC address $macAddress not found."
            }

            // After a successful connection, register the ConnectionInfoListener
            wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener)

        }

        btnSend.setOnClickListener {

            val socket = Socket(getIpAddressFromMacAddress(macAddress), 8888)
            val clientThread = ClientThread(socket, getGPSTuple(), getRSSI())
            clientThread.start()
        }

        btnCompile.setOnClickListener {
            val filename = "data.csv"
            val fileContent = dataList.joinToString("\n")
            val file = File(applicationContext.filesDir, filename)
            file.writeText(fileContent)
            Toast.makeText(applicationContext, "File saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        }

    }


    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        registerReceiver(connectionChangedReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(connectionChangedReceiver)
    }

    private val connectionChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    if (networkInfo?.isConnected == true) {
                        connectedDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                        tvInfo.text = "Connected to ${connectedDevice?.deviceName} (${connectedDevice?.deviceAddress})"
                        markButtonEnable(btnSend)
                    } else {
                        connectedDevice = null
                        tvInfo.text = "Disconnected from the device."
                        markButtonDisable(btnSend)
                    }
                }
            }
        }
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        if (state=="Receiver") {
            Thread(ServerThread()).start()
        }
        else {

            val socket = Socket(getIpAddressFromMacAddress(macAddress), 8888)
            Thread(ClientThread(socket, getGPSTuple(), getRSSI())).start()

        }
    }

    private fun getRSSI(): Int {
        return 100
    }

    private fun getFailureReason(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.ERROR -> "Error"
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
            WifiP2pManager.BUSY -> "Busy"
            else -> "Unknown error"
        }
    }

    private fun deviceAddressToWifiP2pDevice(address: String): WifiP2pDevice {
        return WifiP2pDevice().apply {
            deviceAddress = address
        }
    }

    private val wifiManager: WifiManager by lazy {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun markButtonDisable(button: Button) {
        button.isEnabled = false
        button.setTextColor(ContextCompat.getColor(button.context, R.color.white))
        button.setBackgroundColor(ContextCompat.getColor(button.context, android.R.color.darker_gray))
    }

    fun markButtonEnable(button: Button) {
        button?.isEnabled = true
        button?.setTextColor(ContextCompat.getColor(button.context, android.R.color.white))
        button?.setBackgroundColor(ContextCompat.getColor(button.context, android.R.color.holo_green_light))
    }

    fun getIpAddressFromMacAddress(macAddress: String): String? {
        try {
            val allInterfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in allInterfaces) {
                if (!intf.isUp || intf.isLoopback) {
                    continue
                }
                val addresses = intf.interfaceAddresses
                for (addr in addresses) {
                    if (addr.address is Inet4Address) {
                        val mac = intf.hardwareAddress
                        if (mac != null) {
                            val macStr = mac.joinToString(separator = ":") { String.format("%02X", it) }
                            if (macStr.equals(macAddress, ignoreCase = true)) {
                                return addr.address.hostAddress
                            }
                        }
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }

    inner class ClientThread(private val socket: Socket, private val gpsTuple: String, private val rssi: Int) : Thread() {
        override fun run() {
            try {
                val outputStream = socket.getOutputStream()
                val printWriter = PrintWriter(outputStream)

                // Send GPS and RSSI tuple to server
                val data = "$gpsTuple,$rssi"
                printWriter.println(data)
                printWriter.flush()

                socket.close()
            } catch (e: IOException) {
                // Handle exception here
            }
        }
    }

    inner class ServerThread : Thread() {

        override fun run() {
            try {
                val serverSocket = ServerSocket(8888)

                while (true) {
                    val clientSocket = serverSocket.accept()
                    val inputStream = clientSocket.getInputStream()
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))

                    // Receive data from client
                    val data = bufferedReader.readLine()

                    // Add server's GPS coordinates to data and add to list
                    val gpsTuple = getGPSTuple()
                    val newData = "$gpsTuple,$data"
                    dataList.add(newData)

                    clientSocket.close()
                }

                serverSocket.close()
            } catch (e: IOException) {
                // Handle exception here
            }
        }
    }

    private fun getGPSTuple(): String {
        // Get GPS coordinates and return as a tuple
        return "latitude,longitude"
    }

}



