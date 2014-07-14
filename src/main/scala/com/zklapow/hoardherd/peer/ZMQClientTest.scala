package com.zklapow.hoardherd.peer

import com.zklapow.hoardherd.peer.zmq.ZMQPeer
import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}

object ZMQClientTest {
  def main(args: Array[String]) {
    val peer = ZMQPeer.create("tcp://localhost:5556", 5)
    val resp = peer.get(new GetRequest("", "key"))
    println(s"Got response: $resp")
  }
}
