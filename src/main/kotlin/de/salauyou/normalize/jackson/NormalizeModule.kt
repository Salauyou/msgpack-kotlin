package de.salauyou.normalize.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.Converter
import com.fasterxml.jackson.module.kotlin.PackageVersion
import de.salauyou.normalize.api.Normalize
import de.salauyou.normalize.api.Normalizer
import java.util.concurrent.atomic.AtomicInteger

class NormalizeModule(private val normalizer: Normalizer) :
    SimpleModule(NormalizeModule::class.java.name, PackageVersion.VERSION) {

    private val currentNormalizedLevel = ThreadLocal.withInitial { AtomicInteger(0) }

    override fun setupModule(context: SetupContext) {
        context.addBeanDeserializerModifier(NormalizeBeanDeserializerModifier())
        context.appendAnnotationIntrospector(NormalizeAnnotationIntrospector())
    }

    private inner class NormalizeAnnotationIntrospector : NopAnnotationIntrospector() {
        override fun findDeserializationConverter(a: Annotated): Converter<Any?, Any?> = NormalizingConverter(a.type)
    }

    private inner class NormalizingConverter(val javaType: JavaType) : Converter<Any?, Any?> {
        override fun convert(value: Any?): Any? = when {
            value == null -> null
            currentNormalizedLevel.get().get() > 0 -> normalizer.normalize(value)
            else -> value
        }

        override fun getInputType(typeFactory: TypeFactory) = javaType
        override fun getOutputType(typeFactory: TypeFactory) = javaType
    }

    private inner class NormalizeBeanDeserializerModifier : BeanDeserializerModifier() {
        override fun modifyDeserializer(
            config: DeserializationConfig,
            beanDesc: BeanDescription,
            delegate: JsonDeserializer<*>,
        ): JsonDeserializer<*> = NormalizingDeserializer(delegate, beanDesc.beanClass.markedNormalized())
    }

    private inner class NormalizingDeserializer(
        delegate: JsonDeserializer<*>,
        private val normalized: Boolean,
    ) : DelegatingDeserializer(delegate) {

        override fun deserialize(p: JsonParser, ctx: DeserializationContext): Any? {
            if (normalized) {
                currentNormalizedLevel.get().incrementAndGet()
            }
            val value = super.deserialize(p, ctx)
            if (normalized && currentNormalizedLevel.get().decrementAndGet() == 0) {
                currentNormalizedLevel.remove()
            }
            return value
        }

        override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>) =
            NormalizingDeserializer(newDelegatee, normalized)
    }

    private fun Class<*>.markedNormalized() = getAnnotation(Normalize::class.java) != null
}