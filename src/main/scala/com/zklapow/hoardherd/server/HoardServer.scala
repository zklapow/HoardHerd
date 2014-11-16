package com.zklapow.hoardherd.server

import com.google.common.cache.Cache

abstract class HoardServer(cache: Cache[String, Array[Byte]], loader: (String) => Option[Array[Byte]]) extends Runnable {
}
