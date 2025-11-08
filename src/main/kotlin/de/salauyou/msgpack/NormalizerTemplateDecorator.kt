package de.salauyou.msgpack

import de.salauyou.msgpack.api.Normalizer
import org.msgpack.packer.Packer
import org.msgpack.template.AbstractTemplate
import org.msgpack.template.Template
import org.msgpack.unpacker.Unpacker

class NormalizerTemplateDecorator(
    private val delegate: Template<Any>,
    private val normalizer: Normalizer,
): Template<Any> {

    override fun write(pk: Packer, v: Any?) = delegate.write(pk, v)

    override fun write(pk: Packer, v: Any?, required: Boolean) = delegate.write(pk, v, required)

    override fun read(u: Unpacker, to: Any?): Any? {
        return delegate.read(u, to)?.let {
            normalizer.normalize(it)
        }
    }

    override fun read(u: Unpacker, to: Any?, required: Boolean): Any? {
        return delegate.read(u, to, required)?.let {
            normalizer.normalize(it)
        }
    }
}