package de.salauyou.msgpack

import org.msgpack.template.TemplateRegistry
import org.msgpack.template.builder.TemplateBuilderChain

internal class KtTemplateBuilderChain(registry: TemplateRegistry) : TemplateBuilderChain(registry) {

    override fun reset(registry: TemplateRegistry, cl: ClassLoader?) {
        templateBuilders.add(KtDataClassTemplateBuilder(registry))  // put first in chain
        super.reset(registry, cl)
    }
}