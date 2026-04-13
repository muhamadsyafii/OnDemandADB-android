/*
 * Created by Muhamad Syafii
 * 13/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.syafii.adb_engine.data.protocol

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class AdbStreamManager {
  private val streams = ConcurrentHashMap<Int, Int>()
  private val idGenerator = AtomicInteger(1)

  fun createStream(): Int {
    val localId = idGenerator.getAndIncrement()
    streams[localId] = 0
    return localId
  }

  fun registerRemoteId(localId: Int, remoteId: Int) {
    streams[localId] = remoteId
  }

  fun closeStreamByLocalId(localId: Int) {
    streams.remove(localId)
  }
}