package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.state.KeyValue

class MapKeyValue(vararg initial: Pair<String, String>) : KeyValue {
    val map = mutableMapOf(*initial)
    override fun get(k: String) = map[k]
    override fun put(k: String, v: String?) {
        if (v == null) map.remove(k) else map[k] = v
    }
}
