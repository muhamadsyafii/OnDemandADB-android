package com.syafii.adb_engine.data.source.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature

class RsaKeyManager {
  private val KEY_ALIAS = "adb_rust_client_key"

  fun signToken(token: ByteArray): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    if (!keyStore.containsAlias(KEY_ALIAS)) {
      val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
      kpg.initialize(
        KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
          .setDigests(KeyProperties.DIGEST_SHA1)
          .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
          .setKeySize(2048).build()
      )
      kpg.generateKeyPair()
    }

    val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
    return Signature.getInstance("SHA1withRSA").apply {
      initSign(entry.privateKey)
      update(token)
    }.sign()
  }
}