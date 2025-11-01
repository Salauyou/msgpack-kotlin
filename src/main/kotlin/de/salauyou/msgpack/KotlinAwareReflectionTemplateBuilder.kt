package de.salauyou.msgpack

import org.msgpack.MessageTypeException
import org.msgpack.packer.Packer
import org.msgpack.template.AbstractTemplate
import org.msgpack.template.FieldOption
import org.msgpack.template.Template
import org.msgpack.template.TemplateRegistry
import org.msgpack.template.builder.DefaultFieldEntry
import org.msgpack.template.builder.FieldEntry
import org.msgpack.template.builder.ReflectionTemplateBuilder
import org.msgpack.unpacker.Unpacker
import java.io.IOException
import java.lang.reflect.Field
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class KotlinAwareReflectionTemplateBuilder(registry: TemplateRegistry) : ReflectionTemplateBuilder(registry) {

    override fun toFieldEntries(targetClass: Class<*>, from: FieldOption) : Array<FieldEntry> {
        if (targetClass.kotlin.isData) {
            val kClass = targetClass.kotlin
            // TODO: extract from superclasses as well
            val fieldsByName = targetClass.getDeclaredFields().associateBy { it.name }
            // extract only fields corresponding to args in main constructor in their order in constructor
            val fields: List<Field> = kClass.constructors.first().parameters.map {
                fieldsByName[it.name]!!
            }
            return fields.map { DefaultFieldEntry(it, from) }.toTypedArray()
        } else {
            return super.toFieldEntries(targetClass, from)
        }
    }

    override fun <T : Any> buildTemplate(targetClass: Class<T>, entries: Array<FieldEntry>): Template<T> {
        val kClass = (targetClass as Class<*>).kotlin as KClass<T>
        if (kClass.isData) {
            val membersByName = kClass.members.associateBy { it.name }
            val getters = entries.map { membersByName[it.name]!! }
            val fieldTemplates: List<Template<Any>> = entries.map { registry.lookup(it.genericType) }
            return KotlinDataReflectionClassTemplate(kClass.constructors.first(), getters, fieldTemplates)
        } else {
            return super.buildTemplate(targetClass, entries)
        }
    }

    internal class KotlinDataReflectionClassTemplate<T : Any>(
        private val constructor: KFunction<T>,
        private val getters: List<KCallable<Any?>>,
        private val templates: List<Template<Any>>,
    ): AbstractTemplate<T>() {

        override fun write(pk: Packer, v: T?, required: Boolean) {
            try {
                if (v == null) {
                    pk.writeNil()
                    return
                }
                pk.writeArrayBegin(templates.size)
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

        override fun read(u: Unpacker, to: T?, required: Boolean): T? {
            try {
                if (u.trySkipNil()) {
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
}