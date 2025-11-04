package de.salauyou.msgpack

import de.salauyou.msgpack.api.Normalizer
import org.msgpack.MessagePack

class KtMessagePack(normalizer: Normalizer?) : MessagePack(KtTemplateRegistry(normalizer)) {
    constructor() : this(null)
}