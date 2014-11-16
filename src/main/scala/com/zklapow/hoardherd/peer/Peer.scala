package com.zklapow.hoardherd.peer

import com.google.common.cache.Cache
import com.google.protobuf.ByteString
import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}

trait Peer[T <: Peer[T]] extends Ordered[T] with AutoCloseable {
  def get(request: GetRequest, context: Option[Any] = None): GetResponse
  def getId: String

  override def compare(that: T): Int = {
    getId.compareTo(that.getId)
  }
}
