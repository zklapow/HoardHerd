package com.zklapow.hoardherd.peer.zmq

import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

import com.zklapow.hoardherd.peer.Peer
import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}
import org.zeromq.ZMQ

object ZMQPeer {
  def create(peerAddress: String, queueSize: Int): ZMQPeer = {
    val peer = new ZMQPeer(peerAddress, queueSize)
    for (i <- 0 to queueSize) {
      val client = new peer.ZMQClient(peerAddress)
      client.connect()

      peer.clientQueue.offer(client)
    }

    peer
  }
}

class ZMQPeer(peerAddress: String, queueSize: Int, local: Boolean = false) extends Peer[ZMQPeer] {

  def getAddress = peerAddress
  def getId = getAddress
  def isLocal = local

  val clients: List[ZMQClient] = List()
  val clientQueue = new ArrayBlockingQueue[ZMQClient](queueSize)

  override def get(request: GetRequest, context: Option[Any]): GetResponse = {
    val client: ZMQClient = clientQueue.poll()
    try {
      client.get(request)
    } finally {
      clientQueue.offer(client)
    }
  }

  override def close(): Unit = {
    for (client <- clients) {
      client.close()
    }
  }

  class ZMQClient(address: String) {
    val id = UUID.randomUUID().toString
    val context = ZMQ.context(1)
    val socket = context.socket(ZMQ.REQ)

    def connect() {
      // Set up the socket
      println(s"[client-$id] Connecting")
      socket.setIdentity(id.getBytes())
      socket.connect(address)
      println(s"[client-$id] Connected")
    }

    def get(request: GetRequest): GetResponse = {
      socket.send(request.toByteArray)
      val reply = socket.recvStr()
      val response = GetResponse.parseFrom(reply.getBytes)
      println(s"[client-$id] Got reply: $response")

      response
    }

    def close() = {
      socket.close()
      context.term()
    }
  }

}
