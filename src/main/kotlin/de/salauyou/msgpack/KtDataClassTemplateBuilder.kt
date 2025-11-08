package de.salauyou.msgpack

import org.msgpack.MessageTypeException
import org.msgpack.annotation.Message
import org.msgpack.annotation.MessagePackMessage
import org.msgpack.packer.Packer
import org.msgpack.template.AbstractTemplate
import org.msgpack.template.FieldList
import org.msgpack.template.Template
import org.msgpack.template.TemplateRegistry
import org.msgpack.template.builder.AbstractTemplateBuilder
import org.msgpack.template.builder.FieldEntry
import org.msgpack.unpacker.Unpacker
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor

internal class KtDataClassTemplateBuilder(registry: TemplateRegistry) : AbstractTemplateBuilder(registry) {

    override fun matchType(targetType: Type, forceBuild: Boolean) = targetType.ktClass<Any>().let {
        (it.hasAnnotation<Message>() || it.hasAnnotation<MessagePackMessage>()) && it.isData
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> buildTemplate(targetType: Type): Template<T> {
        if (targetType is ParameterizedType) {
            throw UnsupportedOperationException("Parameterized Kotlin data classes not supported")
        }
        val ktClass = targetType.ktClass<T>()
        val constructor = ktClass.primaryConstructor
            ?: throw UnsupportedOperationException("No primary constructor")  // should not happen
        val propertiesByName = ktClass.members.filter { it is KProperty<*> }.associateBy { it.name }
        val templates = constructor.parameters.map { registry.lookup(it.type.javaType) }
        val getters = constructor.parameters.map { propertiesByName[it.name]!! }
        val requiredFlags = constructor.parameters.map { !it.type.isMarkedNullable }
        return KtDataClassTemplate(ktClass, constructor, templates, getters, requiredFlags)
    }

    override fun <T : Any> buildTemplate(targetClass: Class<T>, fieldList: FieldList): Template<T> {
        throw UnsupportedOperationException("Template with limited fields not supported for Kotlin data classes")
    }

    override fun <T : Any> buildTemplate(targetClass: Class<T>, entries: Array<FieldEntry>): Template<T> {
        throw UnsupportedOperationException("Not used in this implementation")
    }

    internal class KtDataClassTemplate<T : Any>(
        private val ktClass: KClass<T>,
        private val constructor: KFunction<T>,
        private val templates: List<Template<Any>>,
        private val getters: List<KCallable<Any?>>,
        private val requiredFlags: List<Boolean>,
    ) : AbstractTemplate<T>() {

        @Throws(IOException::class)
        override fun write(pk: Packer, v: T?, required: Boolean) {
            try {
                if (v == null) {
                    pk.writeNil()
                    return
                }
                pk.writeArrayBegin(getters.size)
                templates.forEachIndexed { i, tmpl ->
                    tmpl.write(pk, getters[i].call(v), requiredFlags[i])
                }
                pk.writeArrayEnd()
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw MessageTypeException(e)
            }
        }

        @Throws(IOException::class)
        override fun read(u: Unpacker, to: T?, required: Boolean): T? {
            try {
                if (!required && u.trySkipNil()) {
                    return null
                }
                u.readArrayBegin()
                val values = templates.mapIndexed { i, tmpl ->
                    tmpl.read(u, null, requiredFlags[i])
                }
                u.readArrayEnd()
                try {
                    return constructor.call(*values.toTypedArray())
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to instantiate $ktClass with constructor arguments $values", e)
                }
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw MessageTypeException(e)
            }
        }
    }

    private fun <T : Any> Type.ktClass(): KClass<T> = when (this) {
        is ParameterizedType -> (rawType.javaClass as Class<T>).kotlin
        else -> (this as Class<T>).kotlin
    }
}