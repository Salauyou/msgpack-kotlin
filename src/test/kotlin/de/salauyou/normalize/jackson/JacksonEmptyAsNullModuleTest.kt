package de.salauyou.normalize.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JacksonEmptyAsNullModuleTest {

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(EmptyAsNullModule())
        .enable(SerializationFeature.INDENT_OUTPUT)

    @Test
    fun `object with string value as non-null deserialization`() {
        val input = StringData("abc")
        val json = objectMapper.writeValueAsString(input)
        val output = objectMapper.readValue(json, StringData::class.java)
        assertEquals(StringData("abc"), output)
    }

    @Test
    fun `object with empty-string value as null deserialization`() {
        val input = StringData("")
        val json = objectMapper.writeValueAsString(input)
        val output = objectMapper.readValue(json, StringData::class.java)
        assertNull(output)
    }

    @Test
    fun `empty string as null deserialization`() {
        val input = BeanWithNestedBean(strValue1 = "abc", strValue2 = "", nested = StringData("123"))
        val json = objectMapper.writeValueAsString(input)
        val output = objectMapper.readValue(json, BeanWithNestedBean::class.java)
        assertEquals(BeanWithNestedBean(strValue1 = "abc", strValue2 = null, nested = StringData("123")), output)
    }

    @Test
    fun `nested object with empty-string value as null deserialization`() {
        val input = BeanWithNestedBean(strValue1 = "abc", strValue2 = "123", nested = StringData(""))
        val json = objectMapper.writeValueAsString(input)
        val output = objectMapper.readValue(json, BeanWithNestedBean::class.java)
        assertEquals(BeanWithNestedBean(strValue1 = "abc", strValue2 = "123", nested = null), output)
    }

    @Test
    fun `object with nested empty-string values as null deserialization`() {
        val input = BeanWithNestedBean(strValue1 = "", strValue2 = "", nested = StringData(""))
        val json = objectMapper.writeValueAsString(input)
        val output = objectMapper.readValue(json, BeanWithNestedBean::class.java)
        assertNull(output)
    }

    data class StringData(
        val value: String?,
    )

    data class BeanWithNestedBean(
        val strValue1: String?,
        val strValue2: String?,
        val nested: StringData?,
    )
}