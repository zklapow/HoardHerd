package com.zklapow.hoardherd.peer

import com.google.common.cache.{CacheBuilder, Cache}
import com.google.protobuf.ByteString
import com.zklapow.hoardherd.ByteView
import com.zklapow.hoardherd.peer.zmq.{ZMQPeer, ZMQCacheServer}
import com.zklapow.hoardherd.proto.{GetRequest}
import org.junit.Test

import org.fest.assertions.Assertions._

class ZMQPeerTest {

  val simpleLoader = (key: String) => Some(key.toBytes)

  @Test
  def testPeering() {
    var servers: List[ZMQCacheServer] = List()
    var addresses: List[String] = List()
    var caches: List[Cache[String, Array[Byte]]] = List()
    for (i <- 0 to 5) {
      val cache: Cache[String, Array[Byte]] = CacheBuilder.newBuilder().maximumSize(100).recordStats().build()
      servers = servers :+ startServer(6500 + i, cache, simpleLoader)
      addresses = addresses :+ s"tcp://localhost:${6500 + i}"
      caches = caches :+ cache
    }

    val peerPicker = new PeerPicker[ZMQPeer](addresses.map((address: String) => ZMQPeer.create(address, 5)))

    for (i <- 0 to 200) {
      val peer = peerPicker.pickPeer(i.toString)
      println(s"[peer ${peer.get.getAddress}] Requesting $i")
      assertThat(peer.isDefined).isTrue()

      val request = GetRequest(i.toString, i.toString)
      val response = peer.get.get(request)

      val string = new String(response.`value`.getOrElse(ByteString.copyFrom(new Array[Byte](0))).toByteArray)
      println(s"[peer ${peer.get.getAddress}] Got value: $string")
      Thread.sleep(100)
    }

    for (server <- servers) {
      server.stop()
    }
  }

  def startServer(port: Int, cache: Cache[String, Array[Byte]], loader: (String) => Option[Array[Byte]]): ZMQCacheServer = {
    val cacheServer = new ZMQCacheServer(Some(port), Some(5), cache, loader)
    cacheServer.start()

    cacheServer
  }

  class StringByteView(string: String) extends ByteView {
    override def toBytes: Array[Byte] = string.getBytes
  }

  implicit def stringByteViewWrapper(string: String): StringByteView = new StringByteView(string)
}
