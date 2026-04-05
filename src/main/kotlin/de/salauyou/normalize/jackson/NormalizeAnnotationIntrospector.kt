package de.salauyou.normalize.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.Converter
import de.salauyou.normalize.api.Normalizer
import de.salauyou.normalize.jackson.NormalizeBeanDeserializerModifier.Companion.NORMALIZED_STACK

class NormalizeAnnotationIntrospector(private val normalizer: Normalizer) : NopAnnotationIntrospector() {

    override fun findDeserializationConverter(a: Annotated): Converter<Any?, Any?> {
        return NormalizingConverter(a.type)
    }

    private inner class NormalizingConverter<T>(val javaType: JavaType) : Converter<T?, T?> {
        override fun convert(value: T?): T? = when {
            value == null -> null
            NORMALIZED_STACK.get().get() > 0 -> normalizer.normalize(value) as T?
            else -> value
        }
        override fun getInputType(typeFactory: TypeFactory) = javaType
        override fun getOutputType(typeFactory: TypeFactory) = javaType
    }
}