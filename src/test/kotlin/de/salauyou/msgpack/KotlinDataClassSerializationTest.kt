package de.salauyou.msgpack

import de.salauyou.msgpack.KotlinAwareReflectionTemplateBuilder.KotlinDataReflectionClassTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.msgpack.MessagePack
import org.msgpack.template.TemplateRegistry
import java.math.BigInteger

class KotlinDataClassSerializationTest {

    @Test
    fun `kotlin data classes serialized with custom templates`() {
        val templateRegistry = TemplateRegistry(null)
        val msgpackBuilder = KotlinAwareReflectionTemplateBuilder(templateRegistry)

        val templateNested = msgpackBuilder.buildTemplate<NestedData>(NestedData::class.java)
        assertTrue(templateNested is KotlinDataReflectionClassTemplate)
        templateRegistry.register(NestedData::class.java, templateNested)
        val template = msgpackBuilder.buildTemplate<SampleData>(SampleData::class.java)
        assertTrue(template is KotlinDataReflectionClassTemplate)

        val msgPack = MessagePack()
        msgPack.register(NestedData::class.java, templateNested)
        msgPack.register(SampleData::class.java, template)

        val input = SampleData("1", null, BigInteger.TEN, NestedData("a", "b"), NestedData("c", null))
        val binary = msgPack.write(input)
        val output = msgPack.read(binary, SampleData::class.java)

        assertEquals(input, output)
    }

    data class SampleData(
        val string: String,
        val stringNullable: String?,
        val bigint: BigInteger,
        val nested: NestedData,
        val nestedNullable: NestedData?,
    )

    data class NestedData(
        val str: String,
        val strNullable: String?,
    )
}