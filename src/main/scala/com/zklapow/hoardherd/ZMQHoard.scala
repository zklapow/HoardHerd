package com.zklapow.hoardherd

import com.google.common.cache.{Cache, CacheBuilder}
import com.zklapow.hoardherd.peer.PeerPicker
import com.zklapow.hoardherd.peer.zmq.ZMQPeer
import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}
import com.zklapow.hoardherd.server.ZMQCacheServer

import scala.collection.mutable

object ZMQHoard {

  val hoards = new mutable.HashMap[String, ZMQHoard]

  def getZMQHoard(name: String, peerAddresses: List[String], maxSize: Long = 1000,
                  loader: (String) => Option[Array[Byte]], port: Int = 5005, numWorkers: Int = 5): ZMQHoard = {
    if (!hoards.contains(name)) {
      this.synchronized {
        if (!hoards.contains(name)) {
          val hoard = new ZMQHoard(name, peerAddresses, maxSize, loader, port, numWorkers)
          hoard.run()
          hoards.put(name, hoard)

          return hoard
        }
      }
    }

    return hoards.get(name).get
  }

  def stopAll(): Unit = {
    for (hoard <- hoards.valuesIterator) {
      hoard.stop()
    }
  }
}

class ZMQHoard(name: String, peerAddresses: List[String], maxSize: Long = 1000, loader: (String) => Option[Array[Byte]],
               port: Int = 5005, numWorkers: Int = 5, numClients: Int = 1) extends Hoard with Runnable {

  val hotCache: Cache[String, Array[Byte]] = CacheBuilder.newBuilder().maximumSize(Math.floor(maxSize/2).toLong).recordStats().build()
  val localCache: Cache[String, Array[Byte]] = CacheBuilder.newBuilder().maximumSize(Math.floor(maxSize/2).toLong).recordStats().build()

  val peers = peerAddresses.map((address: String) => {
    ZMQPeer.create(address, numClients)
  })

  val peerPicker = new PeerPicker[ZMQPeer](peers)

  var server: Option[ZMQCacheServer] = None

  def get(key: String): Option[Array[Byte]] = {
    val request = new GetRequest(name, key)
    val peer = peerPicker.pickPeer(key)

    var getResponse: Option[GetResponse] = Option.empty
    if (peer.isDefined) {
      getResponse = Option.apply(peer.get.get(request))
    }

    if (getResponse.isDefined && getResponse.get.`value`.isDefined) {
      return Option.apply(getResponse.get.`value`.get.toByteArray)
    }

    return Option.empty
  }

  override def run(): Unit = {
    server = Some(new ZMQCacheServer(Some(port), Some(numWorkers), localCache, loader))
    server.get.start()
  }

  def stop(): Unit = {
    if (server.isDefined) {
      server.get.stop()
    }

    for (peer <- peers) {
      peer.close()
    }
  }
}
