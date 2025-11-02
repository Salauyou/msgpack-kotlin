package de.salauyou.msgpack

import de.salauyou.msgpack.KtCollectionTemplates.KtListTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtMapTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtSetTemplate
import org.msgpack.template.GenericCollectionTemplate
import org.msgpack.template.GenericMapTemplate
import org.msgpack.template.Template
import org.msgpack.template.TemplateRegistry
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

class KtTemplateRegistry : TemplateRegistry(null) {

    init {
        registerGeneric(Map::class.java, GenericMapTemplate(this, KtMapTemplate::class.java))
        registerGeneric(List::class.java, GenericCollectionTemplate(this, KtListTemplate::class.java))
        registerGeneric(Collection::class.java, GenericCollectionTemplate(this, KtListTemplate::class.java))
        registerGeneric(Set::class.java, GenericCollectionTemplate(this, KtSetTemplate::class.java))
    }

    override fun lookup(targetType: Type): Template<*> {
        val clearedType = when (targetType) {
            // type parameters of generic Kt collections are resolved as wildcards:
            // List<E> -> List<? extends E>, Map<K, V> -> Map<K, ? extends V>, ...
            is WildcardType -> when {
                targetType.upperBounds.size == 1 -> targetType.upperBounds.single()
                else -> throw UnsupportedOperationException(
                    "Templates for lower- and multiple-bound generic wildcards not supported ($targetType)"
                )
            }
            is TypeVariable<*> -> when {
                targetType.bounds.size == 1 -> targetType.bounds.single()
                else -> throw UnsupportedOperationException(
                    "Templates for multiple-bound generic type variables not supported ($targetType)"
                )
            }
            else -> targetType
        }
        return super.lookup(clearedType)
    }
}