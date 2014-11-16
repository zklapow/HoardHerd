package com.zklapow.hoardherd

trait Hoard {
  def get(key: String): Option[Array[Byte]]
}
