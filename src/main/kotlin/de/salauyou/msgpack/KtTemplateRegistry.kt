package de.salauyou.msgpack

import de.salauyou.msgpack.KtCollectionTemplates.KtListTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtMapTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtSetTemplate
import org.msgpack.template.GenericCollectionTemplate
import org.msgpack.template.GenericMapTemplate
import org.msgpack.template.Template
import org.msgpack.template.TemplateRegistry
import org.msgpack.template.builder.TemplateBuilderChain
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

internal class KtTemplateRegistry : TemplateRegistry(null) {

    init {
        registerGeneric(Map::class.java, GenericMapTemplate(this, KtMapTemplate::class.java))
        registerGeneric(List::class.java, GenericCollectionTemplate(this, KtListTemplate::class.java))
        registerGeneric(Collection::class.java, GenericCollectionTemplate(this, KtListTemplate::class.java))
        registerGeneric(Set::class.java, GenericCollectionTemplate(this, KtSetTemplate::class.java))
    }

    override fun createTemplateBuilderChain(): TemplateBuilderChain {
        return KtTemplateBuilderChain(this)
    }

    override fun lookup(targetType: Type): Template<*> {
        val clearedType = when (targetType) {
            // type parameters of generic Kt collections are resolved as wildcards
            // e.g.: Map<K, V> -> java.util.Map<K, ? extends V>
            is WildcardType -> when {
                targetType.upperBounds.isEmpty() || targetType.upperBounds.size > 1 -> throw UnsupportedOperationException(
                    "Templates for no- or multiple-bounded generic wildcards not supported ($targetType)"
                )
                targetType.upperBounds.single() == Object::class.java -> throw UnsupportedOperationException(
                    "Templates for unbounded generic wildcards not supported ($targetType)"
                )
                else -> targetType.upperBounds.single()
            }
            is TypeVariable<*> -> when {
                targetType.bounds.size > 1 -> throw UnsupportedOperationException(
                    "Templates for multiple-bound generic type variables not supported ($targetType)"
                )
                targetType.bounds.single() == Object::class.java -> throw UnsupportedOperationException(
                    "Templates for unbounded type variables not supported ($targetType)"
                )
                else -> targetType.bounds.single()
            }
            else -> targetType
        }
        return super.lookup(clearedType)
    }
}