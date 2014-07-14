package com.zklapow.hoardherd

import com.google.common.cache.{CacheLoader, Cache}
import com.zklapow.hoardherd.peer.zmq.ZMQCacheServer

import scala.collection.mutable

object HoardHerd {
  var port = 5335
  var numWorkers = 5
  var serverStarted = false

  val hoards = new mutable.HashMap[String, CacheAndLoader]()

  def newHoard[T <: ByteView](name: String, localAddress: String, peerAddresses: List[String],
                              loader: (String) => Array[Byte], parser: (Array[Byte]) => T, maxSize: Long = 1000) {
    if (!serverStarted) {
      this.synchronized {
        if (!serverStarted) {
          startServer
        }
      }
    }
    val cacheAndLoader = new CacheAndLoader(loader, maxSize.toInt)
    val hoard = new Hoard[T](name, localAddress, peerAddresses, cacheAndLoader, parser, maxSize)
    hoards(name) = hoard.getCacheAndLoader
  }

  def startServer() {
    new ZMQCacheServer(Some(port), Some(numWorkers))

    serverStarted = true
  }

  def setPort(newPort: Int) = {
    port = newPort
  }

  def setNumWorkers(newNum: Int) = {
    numWorkers = newNum
  }
}
