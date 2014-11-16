package com.zklapow.hoardherd

import com.google.common.cache.{CacheLoader, Cache}
import com.zklapow.hoardherd.peer.zmq.ZMQCacheServer

import scala.collection.mutable

object HoardHerd {
  var port = 5335
  var numWorkers = 5
  var serverStarted = false

  val servers = new mutable.HashMap[String, ZMQCacheServer]

  def newHoard(name: String, localAddress: String, peerAddresses: List[String], maxSize: Long = 1000, loader: (String) => Option[Array[Byte]]): Hoard = {
    val hoard = new Hoard(name, localAddress, peerAddresses, maxSize, loader)

    if (!servers.contains(name)) {
      this.synchronized {
        if (!servers.contains(name)) {
          startServer(hoard.localCache, loader)
        }
      }
    }

    return hoard
  }

  def startServer(cache: Cache[String, Array[Byte]], loader: (String) => Option[Array[Byte]]) {
    new ZMQCacheServer(Some(port), Some(numWorkers), cache, loader)

    serverStarted = true
  }

  def setPort(newPort: Int) = {
    port = newPort
  }

  def setNumWorkers(newNum: Int) = {
    numWorkers = newNum
  }
}
