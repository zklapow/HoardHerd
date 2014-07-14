package com.zklapow.hoardherd.peer.zmq

import java.util.UUID

import org.zeromq.ZMQ

import scala.util.Random

object MultiWorkerClient {

  private val SERVER_ADDR = "tcp://localhost:5555"

  def main(args: Array[String]) {
    val numreqs = new Random().nextInt(200)
    for (i <- 0 to numreqs) {
      new Thread(new ClientTask).start()
    }
  }

  class ClientTask extends Runnable {
    val id = UUID.randomUUID().toString

    override def run(): Unit = {
      val context = ZMQ.context(1)

      val socket = context.socket(ZMQ.REQ)
      socket.setIdentity(id.getBytes)

      println(s"[client-$id] Connecting")
      socket.connect(SERVER_ADDR)
      println(s"[client-$id] Connected")

      socket.send("GIVE ME DATA")
      val reply = socket.recvStr()
      println(s"[client-$id] Got reply: $reply")

      socket.close()
      context.term()
    }
  }

}
