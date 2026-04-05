package de.salauyou.normalize.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import de.salauyou.normalize.api.Normalize
import java.util.concurrent.atomic.AtomicInteger

class NormalizeBeanDeserializerModifier : BeanDeserializerModifier() {
    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>,
    ): JsonDeserializer<*> = when {
        beanDesc.beanClass.getAnnotation(Normalize::class.java) != null -> NormalizingDeserializer(deserializer, true)
        else -> NormalizingDeserializer(deserializer, false)
    }

    private inner class NormalizingDeserializer(
        delegate: JsonDeserializer<*>,
        private val setNormalized: Boolean,
    ) : DelegatingDeserializer(delegate) {
        override fun deserialize(p: JsonParser, ctx: DeserializationContext): Any? {
            if (setNormalized) {
                NORMALIZED_STACK.get().incrementAndGet()
            }
            val value = super.deserialize(p, ctx)
            if (setNormalized && NORMALIZED_STACK.get().decrementAndGet() == 0) {
                NORMALIZED_STACK.remove()
            }
            return value
        }

        override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>) = NormalizingDeserializer(newDelegatee, setNormalized)
    }

    companion object {
        internal val NORMALIZED_STACK = ThreadLocal.withInitial { AtomicInteger(0) }
    }
}