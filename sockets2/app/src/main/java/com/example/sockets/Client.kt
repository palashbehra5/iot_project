package com.example.sockets

import java.net.Socket
import kotlin.concurrent.thread

class ClientThread : Thread() {

    override fun run() {
        val clientSocket = Socket("localhost",44123)
        var count = 0
        val result = "Dummy"

        while (count<10) {

            // getGPS, getRSSI
            // Create String "Result"
            count+=1
            clientSocket.getOutputStream().write(result.toByteArray())
            Thread.sleep(1000)

        }

        // Send EXIT Message
        clientSocket.getOutputStream().write("END".toByteArray())

        clientSocket.close()
    }

}

// Call this function from your activity to start the client thread
fun startClient() {
    val clientThread = ClientThread()
    clientThread.start()
}

fun main(){

    startClient()

}