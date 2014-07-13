package com.zklapow.hoardherd.peer

trait PeerPicker {
  def pickPeer(key: String): Option[Peer]
}
