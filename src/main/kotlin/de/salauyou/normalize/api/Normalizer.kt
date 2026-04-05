package de.salauyou.normalize.api

interface Normalizer {
    fun normalize(value: Any): Any?
}