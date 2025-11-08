package de.salauyou.msgpack

import de.salauyou.msgpack.KtCollectionTemplates.KtListTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtMapTemplate
import de.salauyou.msgpack.KtCollectionTemplates.KtSetTemplate
import de.salauyou.msgpack.api.Normalize
import de.salauyou.msgpack.api.Normalizer
import org.msgpack.MessageTypeException
import org.msgpack.template.GenericCollectionTemplate
import org.msgpack.template.GenericMapTemplate
import org.msgpack.template.Template
import org.msgpack.template.TemplateRegistry
import org.msgpack.template.builder.TemplateBuilderChain
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.*
import kotlin.reflect.full.hasAnnotation

internal class KtTemplateRegistry(private val normalizer: Normalizer?) : TemplateRegistry(null) {

    private val lookupStack = Stack<LookupItem>()
    private val lookupCache = HashMap<Type, Template<*>>()  // no access to TemplateRegistry.cache

    init {
        registerGeneric(Map::class.java, GenericMapTemplate(this, KtMapTemplate::class.java))
        registerGeneric(List::class.java, GenericCollectionTemplate(this, KtListTemplate::class.java))
        registerGeneric(Collection::class.java, GenericCollectionTemplate(this, KtListTemplate::class.java))
        registerGeneric(Set::class.java, GenericCollectionTemplate(this, KtSetTemplate::class.java))
    }

    override fun createTemplateBuilderChain(): TemplateBuilderChain {
        return KtTemplateBuilderChain(this)
    }

    @Synchronized
    override fun lookup(targetType: Type): Template<*> {
        lookupCache[targetType]?.let {
            return it
        }
        val clearedType = when (targetType) {
            // type parameters of generic Kt collections are resolved as wildcards
            // e.g.: Map<K, V> -> java.util.Map<K, ? extends V>
            is WildcardType -> when {
                targetType.upperBounds.isEmpty() || targetType.upperBounds.size > 1 -> throw UnsupportedOperationException(
                    "Templates for no- or multiple-bounded generic wildcards not supported ($targetType)."
                )
                targetType.upperBounds.single() == Object::class.java -> throw UnsupportedOperationException(
                    "Templates for unbounded generic wildcards not supported ($targetType)."
                )
                else -> targetType.upperBounds.single()
            }
            is TypeVariable<*> -> when {
                targetType.bounds.size > 1 -> throw UnsupportedOperationException(
                    "Templates for multiple-bound generic type variables not supported ($targetType)."
                )
                targetType.bounds.single() == Object::class.java -> throw UnsupportedOperationException(
                    "Templates for unbounded type variables not supported ($targetType)."
                )
                else -> targetType.bounds.single()
            }
            else -> targetType
        }
        // check whether requested type is normalized
        val isTypeNormalized = when (clearedType) {
            is Class<*> -> clearedType.kotlin.hasAnnotation<Normalize>()
            is ParameterizedType -> (clearedType.rawType as Class<*>).kotlin.hasAnnotation<Normalize>()
            else -> false
        }
        // find/create template and decorate if needed
        try {
            lookupStack.push(LookupItem(targetType, isTypeNormalized))
            var template = super.lookup(clearedType)
            if (normalizer != null && lookupStack.any { it.normalized }) {
                template = NormalizerTemplateDecorator(template, normalizer)
            }
            lookupStack.pop()
            if (lookupStack.isEmpty()) {
                // store templates only for root classes
                lookupCache[targetType] = template
            }
            return template
        } catch (e: Exception) {
            val stack = lookupStack.map { it.type }
            lookupStack.clear()
            if (e is MessageTypeException && stack.isNotEmpty()) {
                throw MessageTypeException("${e.message} Lookup stack: $stack", e.cause)
            } else {
                throw e
            }
        }
    }

    private class LookupItem(val type: Type, val normalized: Boolean)
}