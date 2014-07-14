package com.zklapow.hoardherd.peer

import com.google.common.hash.{PrimitiveSink, Funnel}
import com.zklapow.hoardherd.hashing.RendezvousHash

import scala.reflect.ClassTag

class PeerPicker[T <: Peer[T]](initialPeers: List[T]) (implicit c: ClassTag[T])  {
  private val PEER_FUNNEL = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = {
      into.putBytes(from.getId.getBytes)
    }
  }

  private val STRING_FUNNEL = new Funnel[String] {
    override def funnel(from: String, into: PrimitiveSink): Unit = {
      into.putBytes(from.getBytes)
    }
  }

  private val rendezvousHash = new RendezvousHash[String, T](STRING_FUNNEL, PEER_FUNNEL, initialPeers)

  def pickPeer(key: String): Option[T] = rendezvousHash.get(key)

  def addPeer(peer: T) = rendezvousHash.add(peer)
  def removePeer(peer: T) = rendezvousHash.remove(peer)
  def size() = rendezvousHash.size()
}
