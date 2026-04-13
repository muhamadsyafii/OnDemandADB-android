/*
 * Created by Muhamad Syafii
 * 13/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.syafii.adb_engine.data.source.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.util.concurrent.atomic.AtomicBoolean

class AdbSocketManager {
  private var socket: Socket? = null
  private var inputStream: InputStream? = null
  private var outputStream: OutputStream? = null
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val isConnected = AtomicBoolean(false)

  private val _incomingData = MutableSharedFlow<ByteArray>(replay = 50)
  val incomingData: SharedFlow<ByteArray> = _incomingData

  suspend fun connect(ip: String, port: Int, useTls: Boolean) = withContext(Dispatchers.IO) {
    if (useTls) {
      // Bypass TLS khusus untuk localhost (Android 11+)
      val trustAll = arrayOf(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers() = arrayOf<java.security.cert.X509Certificate>()
      })
      val sslContext = SSLContext.getInstance("TLSv1.3").apply { init(null, trustAll, null) }
      val sslSocket = sslContext.socketFactory.createSocket(ip, port) as javax.net.ssl.SSLSocket
      sslSocket.startHandshake()
      socket = sslSocket
    } else {
      socket = Socket(ip, port).apply { soTimeout = 0 }
    }

    inputStream = socket?.getInputStream()
    outputStream = socket?.getOutputStream()
    isConnected.set(true)
    startListening()
  }

  private fun startListening() = scope.launch {
    val buffer = ByteArray(8192)
    try {
      while (isActive && isConnected.get()) {
        val bytesRead = inputStream?.read(buffer) ?: break
        if (bytesRead == -1) break
        _incomingData.emit(buffer.copyOfRange(0, bytesRead))
      }
    } finally { disconnect() }
  }

  suspend fun sendData(data: ByteArray) = withContext(Dispatchers.IO) {
    outputStream?.write(data)
    outputStream?.flush()
  }

  fun disconnect() {
    if (!isConnected.compareAndSet(true, false)) return
    try { socket?.close() } catch (e: Exception) {}
  }
}