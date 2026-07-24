package de.salauyou.normalize.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.PackageVersion
import java.util.Deque
import java.util.LinkedList

class EmptyAsNullModule : SimpleModule(EmptyAsNullModule::class.java.name, PackageVersion.VERSION) {

    private val deserializationStack: ThreadLocal<Deque<StackItem>> = ThreadLocal.withInitial { LinkedList() }

    override fun setupModule(context: SetupContext) {
        context.addBeanDeserializerModifier(EmptyAsNullDeserializerModifier())
    }

    private inner class EmptyAsNullDeserializerModifier : BeanDeserializerModifier() {
        override fun modifyDeserializer(
            config: DeserializationConfig,
            beanDesc: BeanDescription,
            delegate: JsonDeserializer<*>,
        ): JsonDeserializer<*> = EmptyAsNullDeserializer(delegate)
    }

    private inner class EmptyAsNullDeserializer(
        delegate: JsonDeserializer<*>,
    ) : DelegatingDeserializer(delegate) {

        override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>) = EmptyAsNullDeserializer(newDelegatee)

        override fun deserialize(p: JsonParser, ctx: DeserializationContext): Any? {
            val stack = deserializationStack.get()
            val currentLevel = stack.peekLast()
            val nestedLevel = StackItem(false, true)
            stack.addLast(nestedLevel) // for nested bean deserializer it will be "current" level
            val value = super.deserialize(p, ctx)
            val result = when {
                value == "" -> null
                nestedLevel.hasValues && nestedLevel.allNulls -> null
                else -> value
            }
            currentLevel?.hasValues = true
            if (result != null) {
                currentLevel?.allNulls = false
            }
            stack.removeLast()
            if (stack.isEmpty()) {
                deserializationStack.remove()
            }
            return result
        }
    }

    private inner class StackItem(var hasValues: Boolean, var allNulls: Boolean)
}