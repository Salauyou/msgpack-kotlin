package de.salauyou.normalize.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.PackageVersion

class EmptyStringAsNullModule : SimpleModule(EmptyStringAsNullModule::class.java.name, PackageVersion.VERSION) {

    override fun setupModule(context: SetupContext) {
        context.addBeanDeserializerModifier(EmptyStringAsNullDeserializerModifier())
    }

    private inner class EmptyStringAsNullDeserializerModifier : BeanDeserializerModifier() {
        override fun modifyDeserializer(
            config: DeserializationConfig,
            beanDesc: BeanDescription,
            delegate: JsonDeserializer<*>,
        ): JsonDeserializer<*> = EmptyStringAsNullDeserializer(delegate)
    }

    private inner class EmptyStringAsNullDeserializer(
        delegate: JsonDeserializer<*>,
    ) : DelegatingDeserializer(delegate) {

        override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>) = EmptyStringAsNullDeserializer(newDelegatee)

        override fun deserialize(p: JsonParser, ctx: DeserializationContext): Any? {
            return when (val value = super.deserialize(p, ctx)) {
                "" -> null
                else -> value
            }
        }
    }
}