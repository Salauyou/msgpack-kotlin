package de.salauyou.normalize.msgpack

import de.salauyou.normalize.api.Normalizer
import org.msgpack.MessagePack

class KtMessagePack(normalizer: Normalizer?) : MessagePack(KtTemplateRegistry(normalizer)) {
    constructor() : this(null)
}