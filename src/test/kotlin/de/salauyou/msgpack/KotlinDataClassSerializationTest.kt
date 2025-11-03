package de.salauyou.msgpack

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.msgpack.MessageTypeException
import org.msgpack.annotation.Message
import java.math.BigInteger

class KotlinDataClassSerializationTest {

    @Test
    fun `data class with nesting`() {
        val msgPack = KtMessagePack()
        val input = SampleData(
            string = "1",
            stringNullable = null,
            bigint = BigInteger.TEN,
            nested = NestedData("a", "b"),
            nestedNullable = NestedData("c", null),
        )
        val binary = msgPack.write(input)
        val output = msgPack.read(binary, SampleData::class.java)
        assertEquals(input, output)
    }

    @Test
    fun `data class with collections`() {
        val msgPack = KtMessagePack()
        val input = DataWithCollections(
            list = listOf("1", "2", "3"),
            set = setOf(NestedData("1", "2"), NestedData("A", "B"), null),
            map = mapOf(null to NestedData("1", "2"), NestedData("A", "B") to null),
            deepMap = mapOf(
                100 to mapOf(101 to NestedData("101", "A"), 102 to NestedData("102", "B")),
                200 to mapOf(201 to NestedData("201", null)),
                300 to mapOf(),
                400 to null,
            )
        )
        val binary = msgPack.write(input)
        val output = msgPack.read(binary, DataWithCollections::class.java)
        assertEquals(input, output)
    }

    @Test
    fun `parameterized data class not supported`() {
        val msgPack = KtMessagePack()
        val input = GenericData("1")
        assertThrows<MessageTypeException> {
            msgPack.write(input)
        }.also {
            assertTrue(it.cause is UnsupportedOperationException)
            assertEquals("Templates for unbounded type variables not supported (T)", it.cause!!.message)
        }
    }

    @Message
    data class SampleData(
        val string: String,
        val stringNullable: String?,
        val bigint: BigInteger,
        val nested: NestedData,
        val nestedNullable: NestedData?,
    )

    @Message
    data class NestedData(
        val str: String,
        val strNullable: String?,
    )

    @Message
    data class DataWithCollections(
        val list: List<String>,
        val set: Set<NestedData?>,
        val map: Map<NestedData?, NestedData?>,
        val deepMap: Map<Int, Map<Int, NestedData>?>,
    )

    @Message
    data class GenericData<T>(
        val value: T,
    )

    class ClassWithParameterizedFields {
        val stringData : GenericData<String>? = null
    }
}