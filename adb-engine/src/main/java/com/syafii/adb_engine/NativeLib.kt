package com.syafii.adb_engine

class NativeLib {

  /**
   * A native method that is implemented by the 'adb_engine' native library,
   * which is packaged with this application.
   */
  external fun stringFromJNI(): String

  companion object {
    // Used to load the 'adb_engine' library on application startup.
    init {
      System.loadLibrary("adb_engine")
    }
  }
}