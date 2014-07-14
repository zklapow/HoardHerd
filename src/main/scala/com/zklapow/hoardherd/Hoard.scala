package com.zklapow.hoardherd

import java.util.concurrent.Callable

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.zklapow.hoardherd.peer.PeerPicker
import com.zklapow.hoardherd.peer.zmq.ZMQPeer
import com.zklapow.hoardherd.proto.GetRequest

class Hoard[T <: ByteView](name: String, localAddress: String, peerAddresses: List[String],
                           cacheAndLoader: CacheAndLoader, parser: (Array[Byte]) => T, maxSize: Long = 1000) {

  def getLoader = cacheAndLoader.getLoader
  def getCache = cacheAndLoader.cache
  def getCacheAndLoader = cacheAndLoader

  val hotCache = CacheBuilder.newBuilder().maximumSize(maxSize).recordStats().build()

  val peerPicker = new PeerPicker[ZMQPeer](peerAddresses.map((address: String) => {
    if (address.equalsIgnoreCase(localAddress)) {
      new ZMQPeer(address, 0, true)
    } else {
      new ZMQPeer(address, 5)
    }
  }))

  def get(key: String): T = {
    val peer = peerPicker.pickPeer(key)
    if (peer.isDefined && peer.get.isLocal) {
      val byteResult = getCache.get(key, new Callable[Array[Byte]] {
        override def call(): Array[Byte] = {
          getLoader(key)
        }
      })

      parser(byteResult)
    } else {
      val response = peer.get.get(new GetRequest(name, key))

      parser(response.`value`.get.toByteArray)
    }
  }
}
