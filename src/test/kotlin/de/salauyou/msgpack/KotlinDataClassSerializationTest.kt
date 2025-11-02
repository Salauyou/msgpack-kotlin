package de.salauyou.msgpack

import de.salauyou.msgpack.KtDataClassTemplateBuilder.KtDataClassTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.msgpack.MessagePack
import org.msgpack.template.GenericCollectionTemplate
import org.msgpack.template.GenericMapTemplate
import org.msgpack.template.TemplateRegistry
import java.math.BigInteger
import de.salauyou.msgpack.KtCollectionTemplates.KtListTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtMapTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtSetTemplate

class KotlinDataClassSerializationTest {

    @Test
    fun `data class with nesting`() {
        val registry = TemplateRegistry(null)
        val builder = KtDataClassTemplateBuilder(registry)

        val templateNested = builder.buildTemplate<NestedData>(NestedData::class.java)
        assertTrue(templateNested is KtDataClassTemplate)
        registry.register(NestedData::class.java, templateNested)

        val template = builder.buildTemplate<SampleData>(SampleData::class.java)
        assertTrue(template is KtDataClassTemplate)

        val msgPack = MessagePack()
        msgPack.register(NestedData::class.java, templateNested)
        msgPack.register(SampleData::class.java, template)

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
        val registry = TemplateRegistry(null)
        val builder = KtDataClassTemplateBuilder(registry)

        registry.registerGeneric(Map::class.java, GenericMapTemplate(registry, KtMapTemplate::class.java))
        registry.registerGeneric(List::class.java, GenericCollectionTemplate(registry, KtListTemplate::class.java))
        registry.registerGeneric(Collection::class.java, GenericCollectionTemplate(registry, KtListTemplate::class.java))
        registry.registerGeneric(Set::class.java, GenericCollectionTemplate(registry, KtSetTemplate::class.java))

        val templateNested = builder.buildTemplate<NestedData>(NestedData::class.java)
        registry.register(NestedData::class.java, templateNested)

        val template = builder.buildTemplate<DataWithCollections>(DataWithCollections::class.java)
        assertTrue(template is KtDataClassTemplate)

        val msgPack = MessagePack()
        msgPack.register(NestedData::class.java, templateNested)
        msgPack.register(DataWithCollections::class.java, template)

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
        val templateRegistry = TemplateRegistry(null)
        val templateBuilder = KtDataClassTemplateBuilder(templateRegistry)

        val genericType = ClassWithParameterizedFields::class.java.getDeclaredField("stringData").genericType

        assertThrows<UnsupportedOperationException> {
            templateBuilder.buildTemplate<GenericData<String>>(genericType)
        }
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

    @JvmSuppressWildcards
    data class DataWithCollections(
        val list: List<String>,
        val set: Set<NestedData?>,
        val map: Map<NestedData?, NestedData?>,
        val deepMap: Map<Int, Map<Int, NestedData>?>,
    )

    data class GenericData<T>(
        val value: T,
    )

    class ClassWithParameterizedFields {
        val stringData : GenericData<String>? = null
    }
}