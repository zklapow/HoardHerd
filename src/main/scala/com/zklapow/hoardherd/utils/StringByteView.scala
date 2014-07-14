package com.zklapow.hoardherd.utils

import com.zklapow.hoardherd.ByteView

class StringByteView(string: String) extends ByteView {
  override def toBytes: Array[Byte] = string.getBytes

  implicit def stringByteViewWrapper(string: String) = new StringByteView(string)
}

