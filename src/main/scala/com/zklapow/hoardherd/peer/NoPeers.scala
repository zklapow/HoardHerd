package com.zklapow.hoardherd.peer

class NoPeers extends PeerPicker {
  def pickPeer(key: String): Option[Peer] = None
}
