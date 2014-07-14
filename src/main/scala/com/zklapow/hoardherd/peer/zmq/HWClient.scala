package com.zklapow.hoardherd.peer.zmq

import org.zeromq.ZMQ

object HWClient {

  def main(args: Array[String]) = {
    val context: ZMQ.Context = ZMQ.context(1)

    println("Connecting to server!")

    val socket: ZMQ.Socket = context.socket(ZMQ.REQ)
    socket.connect("tcp://localhost:5555")

    for (i <- 0 to 10) {
      val message = s"Hello $i"
      println(s"Sending $message")

      socket.send(message)
      val reply: Array[Byte] = socket.recv(0)
      println(s"Received: ${new String(reply, ZMQ.CHARSET)}")
    }

    socket.close()
    context.term()
  }
}
