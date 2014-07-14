package com.zklapow.hoardherd.peer

import java.util.UUID

import com.google.common.cache.{CacheLoader, CacheBuilder, LoadingCache}
import com.zklapow.hoardherd.ByteView
import com.zklapow.hoardherd.peer.zmq.{ZMQPeer, ZMQCacheServer}
import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}
import org.junit.Test

import org.fest.assertions.Assertions._

class ZMQPeerTest {

  @Test
  def testPeering() {
    var servers: List[ZMQCacheServer[StringByteView]] = List()
    var addresses: List[String] = List()
    for (i <- 0 to 5) {
      servers = servers :+ startServer(6500 + i)
      addresses = addresses :+ s"tcp://localhost:${6500 + i}"
    }

    val peerPicker = new PeerPicker[ZMQPeer](addresses.map((address: String) => ZMQPeer.create(address, 5)))

    for (i <- 0 to 200) {
      val peer = peerPicker.pickPeer(i.toString)
      assertThat(peer.isDefined).isTrue()

      val request = GetRequest("", i.toString)
      val response = peer.get.get(request)

      val string = new String(response.`value`.get.toByteArray)
      println(s"[peer ${peer.get.getAddress}] Got value: $string")
    }

    for (server <- servers) {
      server.stop()
    }
  }

  def startServer(port: Int): ZMQCacheServer[StringByteView] = {
    val cache: LoadingCache[String, StringByteView] = CacheBuilder.newBuilder()
      .recordStats()
      .maximumSize(10485760L).build(new CacheLoader[String, StringByteView] {
        override def load(key: String): StringByteView = {
          new StringByteView(UUID.randomUUID().toString)
        }
      })

    val cacheServer = new ZMQCacheServer[StringByteView](cache, Some(port), Some(5))
    cacheServer.start()

    cacheServer
  }

  class StringByteView(string: String) extends ByteView {
    override def toBytes: Array[Byte] = string.getBytes
  }

  implicit def stringByteViewWrapper(string: String) = new StringByteView(string)
}
