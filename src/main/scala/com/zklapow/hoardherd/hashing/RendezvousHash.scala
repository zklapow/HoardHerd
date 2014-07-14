package com.zklapow.hoardherd.hashing

import com.google.common.hash.{Hashing, Funnel}

import scala.collection.immutable.HashSet
import scala.reflect.ClassTag

class RendezvousHash[K, V <: Ordered[V]](keyFunnel: Funnel[K], valueFunnel: Funnel[V], initial: Iterable[V]) (implicit vtag: ClassTag[V]) {
  private val hashFunction = Hashing.murmur3_128()

  private var values = new HashSet[V]() ++ initial
  private var sorted = sort()

  def sort(): Array[V] = {
    val tmp: Array[V] = values.toArray[V]

    tmp.sorted
  }

  def add(node: V) = {
    this.synchronized {
      values += node
      sorted = sort()
    }
  }

  def remove(node: V) = {
    this.synchronized {
      values -= node
      sorted = sort()
    }
  }

  def size(): Int = {
    values.size
  }

  def get(key: K): Option[V] = {
    var maxValue = Long.MinValue
    var max: Option[V] = None
    for (value <- sorted) {
      val hash = hashFunction.newHasher()
        .putObject(key, keyFunnel)
        .putObject(value, valueFunnel)
        .hash().asLong()

      if (hash > maxValue) {
        max = Option.apply(value)
        maxValue = hash
      }
    }

    max
  }
}
