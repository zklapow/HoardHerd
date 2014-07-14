package com.zklapow.hoardherd.peer.zmq

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import org.zeromq.ZMQ
import org.zeromq.ZMQ.Poller

import scala.collection.mutable
import scala.util.Random

object MultiWorkerServer {

  private val READY = "READY"

  private val NUM_WORKER_THREADS = 10
  private val WORKER_SOCKET_ADDR = "ipc://backend.ipc"
  private val CLIENT_SOCKET_ADDR = "tcp://*:5555"

  val index = new AtomicInteger(0)

  def main(args: Array[String]) {
    val context = ZMQ.context(1)

    val backend = context.socket(ZMQ.ROUTER)
    val frontend = context.socket(ZMQ.ROUTER)

    backend.bind(WORKER_SOCKET_ADDR)
    frontend.bind(CLIENT_SOCKET_ADDR)

    println("Starting Server!")

    for (i <- 0 to NUM_WORKER_THREADS) {
      new Thread(new Worker).start()
    }

    val workers = new mutable.Queue[String]()

    while (!Thread.currentThread().isInterrupted) {
      val items: Poller = new Poller(2)

      items.register(backend, Poller.POLLIN)

      if (workers.size > 0) {
        items.register(frontend, Poller.POLLIN)
      }

      if (items.poll() < 0) {
        throw new InterruptedException
      }

      // We got a result from a worker thread
      if (items.pollin(0)) {
        workers.enqueue(backend.recvStr())

        var empty = backend.recvStr()
        assert(empty.length == 0)

        val clientAddr = backend.recvStr()
        if (!clientAddr.equals(READY)) {
          empty = backend.recvStr()
          assert(empty.length == 0)

          val reply = backend.recvStr()
          frontend.sendMore(clientAddr)
          frontend.sendMore("")
          frontend.send(reply)
        }
      }

      if (items.pollin(1)) {
        val clientAddr = frontend.recvStr()
        val empty = frontend.recvStr()
        assert(empty.length == 0)

        val request = frontend.recvStr()
        val workerAddr = workers.dequeue()

        backend.sendMore(workerAddr)
        backend.sendMore("")
        backend.sendMore(clientAddr)
        backend.sendMore("")
        backend.send(request)
      }
    }
  }

  class Worker extends Runnable {
    val id = UUID.randomUUID().toString

    override def run(): Unit = {
      val context = ZMQ.context(1)

      val socket = context.socket(ZMQ.REQ)
      socket.setIdentity(id.getBytes)

      println(s"[worker-$id] Connecting")
      socket.connect(WORKER_SOCKET_ADDR)
      println(s"[worker-$id] Connected")

      socket.send(READY)

      println(s"[worker-$id] READY")

      while (!Thread.currentThread().isInterrupted) {
        val address = socket.recvStr()
        val empty = socket.recvStr()
        assert(empty.length == 0)

        val request = socket.recvStr()
        println(s"[worker-$id] Serving request: $request")

        val result: Int = index.incrementAndGet()
        println(s"[worker-$id] Sending: $result")

        // Do some "work"
        Thread.sleep(new Random().nextInt(1000))

        socket.sendMore(address)
        socket.sendMore("")
        socket.send(s"${String.valueOf(result)}")
      }

      socket.close()
      context.term()
    }
  }
}
