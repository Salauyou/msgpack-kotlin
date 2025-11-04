package de.salauyou.msgpack

import de.salauyou.msgpack.api.Normalize
import de.salauyou.msgpack.api.Normalizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.msgpack.annotation.Message

class NormalizeOnDeserializationTest {

    @Test
    fun `normalization applied on nested properties and collection items`() {
        val msgPack = KtMessagePack(TestNormalizer())
        val input = SampleData(
            string = "1",
            int = 1,
            nested = NestedData(
                string = "10",
                int = 10,
                map = mapOf(
                    "10A" to NestedData("100", 100, mapOf()),
                    "10B" to NestedData("110", 110, null),
                    "10C" to null,
                    null to NestedData("130", 130, mapOf(null to null)),
                )
            ),
            map = mapOf(
                "2A" to NestedData("20", 20, mapOf()),
                "2B" to null,
                null to NestedData("30", 30, null),
            )
        )
        val bytes = msgPack.write(input)
        val output = msgPack.read(bytes, SampleData::class.java)
        val expected = SampleData(
            string = "1",
            int = 1,
            nested = NestedData(
                string = "-10",
                int = -10,
                map = mapOf(
                    "-10A" to NestedData("-100", -100, null),
                    "-10B" to NestedData("-110", -110, null),
                    "-10C" to null,
                    null to NestedData("-130", -130, mapOf(null to null)),
                )
            ),
            map = mapOf(
                "2A" to NestedData("-20", -20, null),
                "2B" to null,
                null to NestedData("-30", -30, null),
            )
        )
        assertEquals(expected, output)
    }

    private class TestNormalizer : Normalizer {
        override fun normalize(value: Any) = when {
            value is String -> "-$value"
            value is Int -> -value
            value is Map<*, *> && value.isEmpty() -> null
            else -> value
        }
    }

    @Message
    data class SampleData(
        val string : String,
        val int: Int,
        val nested: NestedData,
        val map: Map<String?, NestedData?>,
    )

    @Message
    @Normalize
    data class NestedData(
        val string: String,
        val int: Int,
        val map: Map<String?, NestedData?>?,
    )
}