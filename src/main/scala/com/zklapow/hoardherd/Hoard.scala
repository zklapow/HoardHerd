package com.zklapow.hoardherd

import com.google.common.cache.{Cache, CacheBuilder}
import com.zklapow.hoardherd.peer.PeerPicker
import com.zklapow.hoardherd.peer.zmq.ZMQPeer
import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}

class Hoard(name: String, localAddress: String, peerAddresses: List[String], maxSize: Long = 1000, loader: (String) => Option[Array[Byte]]) {

  val hotCache: Cache[String, Array[Byte]] = CacheBuilder.newBuilder().maximumSize(Math.floor(maxSize/2).toLong).recordStats().build()
  val localCache: Cache[String, Array[Byte]] = CacheBuilder.newBuilder().maximumSize(Math.floor(maxSize/2).toLong).recordStats().build()

  val peerPicker = new PeerPicker[ZMQPeer](peerAddresses.map((address: String) => {
      new ZMQPeer(address, 5)
  }))

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
}
