package com.zklapow.hoardherd.peer.zmq

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}
import java.util.concurrent.atomic.AtomicBoolean

import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.protobuf.ByteString
import com.zklapow.hoardherd.ByteView
import com.zklapow.hoardherd.proto.GetResponse
import org.zeromq.ZMQ
import org.zeromq.ZMQ.Poller

import scala.collection.mutable

object ZMQCacheServer {
  val READY = "READY"
}

class ZMQCacheServer[T <: ByteView](cache: LoadingCache[String, T], port: Option[Int], numWorkers: Option[Int]) extends Runnable {
  val id = UUID.randomUUID().toString
  val workerSocketAddr = s"ipc://hoardherd-server-$id.ipc"

  val frontendPort = port.getOrElse(5555)
  var thread: Option[Thread] = None
  var isRunning: AtomicBoolean = new AtomicBoolean(true)

  var socketContext: Option[ZMQ.Context] = None
  var frontendSocket: Option[ZMQ.Socket] = None
  var backendSocket: Option[ZMQ.Socket] = None
  var workerService: Option[ExecutorService] = None

  def start() = {
    if (thread.isEmpty) {
      thread = Some(new Thread(this))
      thread.get.start()
    }
  }

  def stop() = {
    isRunning.getAndSet(false)

    frontendSocket.foreach((socket: ZMQ.Socket) => socket.close())
    backendSocket.foreach((socket: ZMQ.Socket) => socket.close())
    socketContext.foreach((context: ZMQ.Context) => context.close())
    workerService.foreach((service: ExecutorService) => service.shutdown())

    thread.foreach((thread: Thread) => {
      thread.join(5000)

      println(s"$id: shutdown")
    })
  }

  override def run(): Unit = {
    val context = ZMQ.context(1)
    socketContext = Some(context)

    val backend = context.socket(ZMQ.ROUTER)
    val frontend = context.socket(ZMQ.ROUTER)

    backend.bind(workerSocketAddr)
    frontend.bind(s"tcp://*:$frontendPort")

    backendSocket = Some(backend)
    frontendSocket = Some(frontend)

    println(s"ZMQ Server started on port $frontendPort")

    val numWorkers = this.numWorkers.getOrElse(1)
    val executor = Executors.newFixedThreadPool(
      numWorkers,
      new ThreadFactoryBuilder()
        .setNameFormat("hoardherd-server-worker-%d")
        .setDaemon(false)
        .build()
    )

    workerService = Some(executor)

    for (i <- 0 to numWorkers) {
      executor.submit(new CacheWorker(cache))
    }

    val workers = new mutable.Queue[String]()

    while (isRunning.get()) {
      val items: Poller = new Poller(2)

      items.register(backend, Poller.POLLIN)

      if (workers.size > 0) {
        items.register(frontend, Poller.POLLIN)
      }

      if (items.poll() < 0) {
        return
      }

      // We got a result from a worker thread
      if (items.pollin(0)) {
        workers.enqueue(backend.recvStr())

        var empty = backend.recvStr()
        assert(empty.length == 0)

        val clientAddr = backend.recvStr()
        if (!clientAddr.equals(ZMQCacheServer.READY)) {
          empty = backend.recvStr()
          assert(empty.length == 0)

          val reply = backend.recvStr()
          frontend.sendMore(clientAddr)
          frontend.sendMore("")
          frontend.send(reply)
        }
      }

      // A client request some value
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

  class CacheWorker(cache: LoadingCache[String, T]) extends Runnable {
    val id = UUID.randomUUID().toString

    override def run(): Unit = {
      val context = ZMQ.context(1)

      println(s"$logPrefix Connecting...")
      val socket = context.socket(ZMQ.REQ)
      socket.setIdentity(id.getBytes)
      socket.connect(workerSocketAddr)
      println(s"$logPrefix Connected!")

      socket.send(ZMQCacheServer.READY)
      println(s"$logPrefix READY")

      while (!Thread.currentThread().isInterrupted) {
        val address = socket.recvStr()
        val empty = socket.recvStr()
        assert(empty.length == 0)

        val request = socket.recvStr()
        println(s"[worker-$id] Serving request for: $request")

        val result = cache.get(request)
        println(s"[worker-$id] Sending: $result")

        val response = GetResponse.newBuilder
          .setValue(ByteString.copyFrom(result.toBytes))
          .build

        socket.sendMore(address)
        socket.sendMore("")
        socket.send(response.toByteString.toByteArray)
      }

      socket.close()
      context.term()
    }

    def logPrefix = s"[worker-$id]"
  }
}
