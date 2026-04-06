package de.salauyou.normalize.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import de.salauyou.normalize.api.Normalizer

fun ObjectMapper.registerNormalizeModule(normalizer: Normalizer): ObjectMapper =
    registerModule(NormalizeModule(normalizer))