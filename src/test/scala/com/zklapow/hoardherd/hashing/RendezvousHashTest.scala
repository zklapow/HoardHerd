package com.zklapow.hoardherd.hashing

import com.google.common.collect.ComparisonChain
import com.google.common.hash.{PrimitiveSink, Funnel}
import org.junit.Test
import org.fest.assertions.Assertions._

import scala.util.Random

class RendezvousHashTest {

  private val stringFunnel = new Funnel[String] {
    def funnel(from: String, into: PrimitiveSink) {
      into.putBytes(from.getBytes)
    }
  }

  private val orderedStringFunnel = new Funnel[OrderedString] {
    override def funnel(from: OrderedString, into: PrimitiveSink): Unit = {
      into.putBytes(from.getValue.getBytes())
    }
  }

  @Test def testEmpty(): Unit = {
    val rhash = create()
    assertThat(rhash.get("key").isEmpty).isTrue()
  }

  @Test def testInitialNodes(): Unit = {
    val rhash = new RendezvousHash[String, OrderedString](stringFunnel, orderedStringFunnel, List("node1", "node2"))

    val node: String = rhash.get("key").get
    assertThat(node).isNotEmpty
  }

  @Test def testConsistentAfterRemove(): Unit = {
    val rhash = create()
    for (i <- new Range(0, 1000, 1)) {
      rhash.add(new OrderedString("node " + i.toString))
    }

    val node: String = rhash.get("key").get
    assertThat(node).isEqualTo(rhash.get("key").get.getValue)

    for (i <- Range(0, 250, 1)) {
      val remove = "node" + new Random().nextInt(1000)
      if (remove.equals(node)) {
        rhash.remove(remove)
      }
    }

    assertThat(node).isEqualTo(rhash.get("key").get.getValue)
  }

  @Test def testChangeAfterDelete(): Unit = {
    val rhash = create()

    rhash.add("node1")
    rhash.add("node2")

    val node = rhash.get("key").get

    rhash.remove(node)
    assertThat(rhash.size()).isEqualTo(1)
    assertThat(node.getValue).isNotEqualTo(rhash.get("key").get.getValue)
  }

  def create(): RendezvousHash[String, OrderedString] = {
    new RendezvousHash[String, OrderedString](stringFunnel, orderedStringFunnel, List())
  }

  class OrderedString(value: String) extends Ordered[OrderedString] {

    def getValue = value

    override def compare(that: OrderedString): Int = {
      ComparisonChain.start().compare(value, that.getValue).result()
    }
  }

  implicit def orderedStringUnwrapper(orderedString: OrderedString): String = orderedString.getValue
  implicit def orderedStringWrapper(string: String): OrderedString = new OrderedString(string)
}
