package com.zklapow.hoardherd.peer

import com.zklapow.hoardherd.proto.{GetResponse, GetRequest}

class NoPeers extends PeerPicker[NoPeer](List()) {
  override def pickPeer(key: String): Option[NoPeer] = None
}

class NoPeer extends Peer[NoPeer] {
  override def get(request: GetRequest, context: Option[Any]): GetResponse = new GetResponse()

  override def getId: String = ""

  override def close(): Unit = {}
}
