package com.zklapow.hoardherd.peer

import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}

trait Peer {
  def get(request: GetRequest, context: Option[Any] = None): GetResponse
}
