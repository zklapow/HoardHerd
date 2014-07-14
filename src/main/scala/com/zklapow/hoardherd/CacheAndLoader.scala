package com.zklapow.hoardherd

import com.google.common.cache.{CacheBuilder, CacheLoader, Cache}

class CacheAndLoader(loader: (String) => Array[Byte], maxSize: Int) {
  def getLoader = loader

  val cache: Cache[String, Array[Byte]] = CacheBuilder.newBuilder()
    .maximumSize(maxSize)
    .build(new CacheLoader[String, Array[Byte]]() {
      override def load(key: String): Array[Byte] = {
        loader(key)
      }
    })
}
