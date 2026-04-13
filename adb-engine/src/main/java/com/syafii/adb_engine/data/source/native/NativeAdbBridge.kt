package com.syafii.adb_engine.data.source.native

object NativeAdbBridge {
  // Memuat library hasil kompilasi Rust
  init { System.loadLibrary("adbengine") }

  external fun buildConnectMessage(maxPayloadSize: Int): ByteArray
  external fun buildAuthSignatureMessage(signature: ByteArray): ByteArray
  external fun buildOpenMessage(localId: Int, destination: String): ByteArray
}