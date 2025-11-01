package de.salauyou.msgpack

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.msgpack.MessagePack

class SampleTest {

    @Test
    fun `msgpack is accessible`() {
        val msgpack = MessagePack()
        assertNotNull(msgpack)
    }
}