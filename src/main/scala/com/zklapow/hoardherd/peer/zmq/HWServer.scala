package com.zklapow.hoardherd.peer.zmq

import org.zeromq.ZMQ

object HWServer {

  def main(args: Array[String]): Unit = {
    val context = ZMQ.context(1)

    val socket = context.socket(ZMQ.REP)
    socket.bind("tcp://*:5555")

    println("Server started!")

    while (!Thread.currentThread().isInterrupted) {
      val reply: Array[Byte] = socket.recv(0)
      val replyString = new String(reply, ZMQ.CHARSET)

      println(s"Received: $replyString")

      val request = s"$replyString world!"
      socket.send(request)

      Thread.sleep(100)
    }

    socket.close()
    context.term()
  }
}
