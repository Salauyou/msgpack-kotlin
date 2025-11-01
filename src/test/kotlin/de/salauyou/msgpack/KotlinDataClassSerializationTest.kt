package de.salauyou.msgpack

import de.salauyou.msgpack.KotlinDataClassTemplateBuilder.KtDataClassTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.msgpack.MessagePack
import org.msgpack.template.TemplateRegistry
import java.math.BigInteger

class KotlinDataClassSerializationTest {

    @Test
    fun `data class with nesting`() {
        val templateRegistry = TemplateRegistry(null)
        val templateBuilder = KotlinDataClassTemplateBuilder(templateRegistry)

        val templateNested = templateBuilder.buildTemplate<NestedData>(NestedData::class.java)
        assertTrue(templateNested is KtDataClassTemplate)
        templateRegistry.register(NestedData::class.java, templateNested)

        val template = templateBuilder.buildTemplate<SampleData>(SampleData::class.java)
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
    fun `data class with mutable collections`() {
        val templateRegistry = TemplateRegistry(null)
        val templateBuilder = KotlinDataClassTemplateBuilder(templateRegistry)

        val templateNested = templateBuilder.buildTemplate<NestedData>(NestedData::class.java)
        templateRegistry.register(NestedData::class.java, templateNested)

        val template = templateBuilder.buildTemplate<DataWithMutableCollections>(DataWithMutableCollections::class.java)
        assertTrue(template is KtDataClassTemplate)

        val msgPack = MessagePack()
        msgPack.register(NestedData::class.java, templateNested)
        msgPack.register(DataWithMutableCollections::class.java, template)

        val input = DataWithMutableCollections(
            list = arrayListOf("1", "2", "3"),
            map = hashMapOf(1 to NestedData("a", "b"), 2 to NestedData("", null)),
            deepMap = hashMapOf(
                100 to hashMapOf(101 to NestedData("101", "A"), 102 to NestedData("102", "B")),
                200 to hashMapOf(201 to NestedData("201", null)),
                300 to hashMapOf(),
            )
        )
        val binary = msgPack.write(input)
        val output = msgPack.read(binary, DataWithMutableCollections::class.java)

        assertEquals(input, output)
    }

    @Test
    fun `parameterized data class not supported`() {
        val templateRegistry = TemplateRegistry(null)
        val templateBuilder = KotlinDataClassTemplateBuilder(templateRegistry)

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

    data class DataWithMutableCollections(
        val list: ArrayList<String>,
        val map: HashMap<Int, NestedData>,
        val deepMap: HashMap<Int, HashMap<Int, NestedData>>,
    )

    data class GenericData<T>(
        val value: T,
    )

    class ClassWithParameterizedFields {
        val stringData : GenericData<String>? = null
    }
}