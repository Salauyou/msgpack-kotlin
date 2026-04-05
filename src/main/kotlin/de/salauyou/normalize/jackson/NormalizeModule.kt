package de.salauyou.normalize.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.PackageVersion
import de.salauyou.normalize.api.Normalizer

class NormalizeModule(private val normalizer: Normalizer)
    : SimpleModule(NormalizeModule::class.java.name, PackageVersion.VERSION) {

    override fun setupModule(context: SetupContext) {
        context.addBeanDeserializerModifier(NormalizeBeanDeserializerModifier())
        context.appendAnnotationIntrospector(NormalizeAnnotationIntrospector(normalizer))
    }
}

fun ObjectMapper.registerNormalizeModule(normalizer: Normalizer): ObjectMapper =
    registerModule(NormalizeModule(normalizer))