package de.salauyou.msgpack.api

interface Normalizer {
    fun normalize(value: Any): Any?
}