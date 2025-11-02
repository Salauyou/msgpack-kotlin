package de.salauyou.msgpack

import org.msgpack.MessageTypeException
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
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.javaType

internal class KtDataClassTemplateBuilder(registry: TemplateRegistry) : AbstractTemplateBuilder(registry) {

    override fun matchType(targetType: Type, forceBuild: Boolean) = targetType.ktClass<Any>().isData

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> buildTemplate(targetType: Type): Template<T> {
        if (targetType is ParameterizedType) {
            throw UnsupportedOperationException("Parameterized Kotlin data classes not supported")
        }
        val ktClass = targetType.ktClass<T>()
        val membersByName = ktClass.members.associateBy { it.name }
        val mainConstructor = ktClass.constructors.first()
        val templates = mainConstructor.parameters.map { registry.lookup(it.type.javaType) }
        val getters = mainConstructor.parameters.map { membersByName[it.name]!! }
        return KtDataClassTemplate(mainConstructor, templates, getters)
    }

    override fun <T : Any> buildTemplate(targetClass: Class<T>, fieldList: FieldList): Template<T> {
        throw UnsupportedOperationException("Template with limited fields not supported for Kotlin data classes")
    }

    override fun <T : Any> buildTemplate(targetClass: Class<T>, entries: Array<FieldEntry>): Template<T> {
        throw UnsupportedOperationException("Not used in this implementation")
    }

    internal class KtDataClassTemplate<T : Any>(
        private val constructor: KFunction<T>,
        private val templates: List<Template<Any>>,
        private val getters: List<KCallable<Any?>>,
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
                    when (val obj = getters[i].call(v)) {
                        null -> pk.writeNil()
                        else -> tmpl.write(pk, obj, true)
                    }
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
                val values = templates.map {
                    when {
                        u.trySkipNil() -> null
                        else -> it.read(u, null, false)
                    }
                }
                u.readArrayEnd()
                return constructor.call(*values.toTypedArray())
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