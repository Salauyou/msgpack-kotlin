package de.salauyou.msgpack

import org.msgpack.template.ListTemplate
import org.msgpack.template.MapTemplate
import org.msgpack.template.SetTemplate
import org.msgpack.template.Template
import org.msgpack.unpacker.Unpacker
import java.io.IOException

object KtCollectionTemplates {

    class KtListTemplate<E>(private val elementTemplate: Template<E>) : ListTemplate<E>(elementTemplate) {
        @Throws(IOException::class)
        override fun read(u: Unpacker, to: List<E>?, required: Boolean) = readItems(elementTemplate, u, required)?.toList()
    }

    class KtSetTemplate<E>(private val elementTemplate: Template<E>) : SetTemplate<E>(elementTemplate) {
        @Throws(IOException::class)
        override fun read(u: Unpacker, to: Set<E>?, required: Boolean) = readItems(elementTemplate, u, required)?.toSet()
    }

    private fun <E> readItems(template: Template<E>, u: Unpacker, required: Boolean): List<E>? {
        if (!required && u.trySkipNil()) {
            return null
        }
        val n = u.readArrayBegin()
        val items = mutableListOf<E>()
        for (i in 0 until n) {
            items.add(template.read(u, null))
        }
        u.readArrayEnd()
        return items
    }

    class KtMapTemplate<K, V>(
        private val keyTemplate: Template<K>,
        private val valueTemplate: Template<V>,
    ) : MapTemplate<K, V>(keyTemplate, valueTemplate) {

        @Throws(IOException::class)
        override fun read(u: Unpacker, to: Map<K, V>?, required: Boolean): Map<K, V>? {
            if (!required && u.trySkipNil()) {
                return null
            }
            val n = u.readMapBegin()
            val entries = mutableListOf<Pair<K, V>>()
            for (i in 0 until n) {
                entries.add(keyTemplate.read(u, null) to valueTemplate.read(u, null))
            }
            u.readMapEnd()
            return entries.toMap()
        }
    }
}