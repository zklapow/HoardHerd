package com.zklapow.hoardherd

import java.io.{ObjectOutputStream, ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream}

import scala.collection.mutable

class HoardHerdCache {

  var hotCache = new mutable.HashMap[String, Array[Byte]]

  def get[T <: Serializable](key: String, loader: (String) => T): T = {
    if (hotCache.contains(key)) {
      val bytes = hotCache.get(key)
      val ois = new ObjectInputStream(new ByteArrayInputStream(bytes.get))
      val obj = ois.readObject().asInstanceOf[T]

      ois.close()
      return obj
    }

    val obj = loader(key)
    val output = new ByteArrayOutputStream()
    val os = new ObjectOutputStream(output)
    os.writeObject(obj)
    os.close()

    hotCache.put(key, output.toByteArray)

    return obj
  }

}
