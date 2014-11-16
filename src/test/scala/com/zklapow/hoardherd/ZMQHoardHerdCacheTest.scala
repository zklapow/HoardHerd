package com.zklapow.hoardherd

import org.junit.Test
import org.fest.assertions.Assertions._

class ZMQHoardHerdCacheTest {

  @Test def testLoad(): Unit = {
    val cache = new HoardHerdCache

    val loader = (key: String) => {
      new TestObject(key)
    } : TestObject

    val testObject = cache.get("test", loader)
    val testObjectFromCache = cache.get("test", loader)

    assertThat(testObject.name).isEqualToIgnoringCase("test")
    assertThat(testObjectFromCache.name).isEqualToIgnoringCase(testObject.name)
  }

  class TestObject(var name: String) extends Serializable {

  }
}
