package de.salauyou.normalize.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.salauyou.normalize.api.Normalize
import de.salauyou.normalize.api.Normalizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JacksonDeserializationTest {

    @Test
    fun `normalized data class with nesting`() {
        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerNormalizeModule(TestNormalizer())
            .enable(SerializationFeature.INDENT_OUTPUT)

        val input = SampleData(
            stringObj = StringData("1"),
            int = 1,
            nested = NestedData(
                stringObj = StringData("10"),
                string = "10",
                int = 10,
                map = mapOf(
                    "10A" to NestedData(StringData("100"), "100", 100, mapOf()),
                    "10B" to NestedData(StringData("110"), "110", 110, null),
                    "10C" to null,
                    "10D" to NestedData(StringData("130"), "130", 130, mapOf("D" to null)),
                )
            ),
            map = mapOf(
                "2A" to NestedData(StringData("20"), "20", 20, mapOf()),
                "2B" to null,
                "2C" to NestedData(StringData("30"), "30", 30, null),
            ),
            string = StringData("1000"),
        )
        val json = objectMapper.writeValueAsString(input)
        val output = objectMapper.readValue(json, SampleData::class.java)
        val expected = SampleData(
            stringObj = StringData("1"),
            int = 1,
            nested = NestedData(
                stringObj = StringData("-10"),
                string = "-10",
                int = -10,
                map = mapOf(
                    "10A" to NestedData(StringData("-100"), "-100", -100, null),
                    "10B" to NestedData(StringData("-110"), "-110", -110, null),
                    "10C" to null,
                    "10D" to NestedData(StringData("-130"), "-130", -130, mapOf("D" to null)),
                )
            ),
            map = mapOf(
                "2A" to NestedData(StringData("-20"), "-20", -20, null),
                "2B" to null,
                "2C" to NestedData(StringData("-30"), "-30", -30, null),
            ),
            string = StringData("1000")
        )
        assertEquals(expected, output)
    }

    data class SampleData(
        val stringObj: StringData, // should not be normalized
        val int: Int,
        val nested: NestedData,
        val map: Map<String, NestedData?>,
        val string: StringData, // should not be normalized
    )

    @Normalize
    data class NestedData(
        val stringObj: StringData, // should be normalized
        val string: String,
        val int: Int,
        val map: Map<String, NestedData?>?, // TODO: normalize map keys
    )

    data class StringData(
        val value: String,
    )

    private class TestNormalizer : Normalizer {
        override fun normalize(value: Any) = when {
            value is String -> "-$value"
            value is Int -> -value
            value is Map<*, *> && value.isEmpty() -> null
            else -> value
        }
    }
}