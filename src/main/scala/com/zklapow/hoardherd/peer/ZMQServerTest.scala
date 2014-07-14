package com.zklapow.hoardherd.peer

import java.util.UUID

import com.google.common.cache.{CacheLoader, CacheBuilder, LoadingCache}
import com.zklapow.hoardherd.ByteView
import com.zklapow.hoardherd.peer.zmq.ZMQCacheServer

object ZMQServerTest {
  def main(args: Array[String]) {
    val cache: LoadingCache[String, StringByteView] = CacheBuilder.newBuilder()
      .recordStats()
      .maximumSize(10485760L).build(new CacheLoader[String, StringByteView] {
      override def load(key: String): StringByteView = {
        new StringByteView(UUID.randomUUID().toString)
      }
    })

    val cacheServer = new ZMQCacheServer[StringByteView](cache, Some(5556), Some(5))
    cacheServer.start()
  }

  class StringByteView(string: String) extends ByteView {
    override def toBytes: Array[Byte] = string.getBytes
  }
}
