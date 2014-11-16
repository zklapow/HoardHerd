package com.zklapow.hoardherd

import com.google.protobuf.ByteString
import org.junit.Test

import scala.collection.mutable
import scala.util.Random

import org.fest.assertions.Assertions._

class ZMQHoardTest {

  @Test def testZMQHoard(): Unit = {
    val numHoards = new Random().nextInt(10)

    var addresses: List[String] = List()
    for (i <- 0 to numHoards) {
      val port = 6500 + i
      addresses = addresses :+ s"tcp://127.0.0.1:$port"
    }

    val hoards: mutable.MutableList[ZMQHoard] = new mutable.MutableList[ZMQHoard]
    for (i <- 0 to numHoards) {
      val port = 6500 + i
      hoards += ZMQHoard.getZMQHoard(i.toString, addresses, 1000, (key: String) => Some(ByteString.copyFromUtf8(key).toByteArray), port, 1)
    }

    for (i <- 0 to 50) {
      val hoard1 = hoards.get(new Random().nextInt(numHoards))
      val hoard2 = hoards.get(new Random().nextInt(numHoards))

      val res1 = hoard1.get.get(i.toString)
      val res2 = hoard2.get.get(i.toString)

      assertThat(res1.get).isEqualTo(res2.get)
      Thread.sleep(new Random().nextInt(100))
    }

    ZMQHoard.stopAll()
  }
}
