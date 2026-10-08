package com.yishenghuang.skry.domain

/** Exact Hamming-radius candidate index; no approximate nearest-neighbour omissions. */
object DuplicateGrouping {
    data class Photo(val id: String, val hash: String, val quality: Float)
    data class Pick(val id: String, val suggested: Boolean, val starred: Boolean)

    fun group(photos: List<Photo>, threshold: Int = 8): List<Pick> {
        require(threshold in 0..63)
        val valid = photos.mapNotNull { photo -> photo.hash.toULongOrNull(16)?.let { photo to it } }
        // At most threshold changed bits implies at least one unchanged band.
        val bands = threshold + 1
        val indexes = List(bands) { HashMap<ULong, MutableList<Int>>() }
        fun band(hash: ULong, index: Int): ULong {
            val start = index * 64 / bands
            val width = (index + 1) * 64 / bands - start
            val mask = if (width == 64) ULong.MAX_VALUE else (1uL shl width) - 1uL
            return (hash shr start) and mask
        }
        valid.forEachIndexed { index, (_, hash) ->
            indexes.forEachIndexed { b, map -> map.getOrPut(band(hash, b)) { mutableListOf() }.add(index) }
        }
        val assigned = BooleanArray(valid.size)
        val picks = mutableListOf<Pick>()
        valid.forEachIndexed { seedIndex, (_, hash) ->
            if (assigned[seedIndex]) return@forEachIndexed
            val candidates = hashSetOf<Int>()
            indexes.forEachIndexed { b, map -> map[band(hash, b)]?.let(candidates::addAll) }
            val members = candidates.asSequence().filter {
                !assigned[it] && (hash xor valid[it].second).countOneBits() <= threshold
            }.sorted().toList()
            if (members.size > 1) {
                val best = members.maxBy { valid[it].first.quality }
                members.forEach {
                    assigned[it] = true
                    picks += Pick(valid[it].first.id, it != best, it == best)
                }
            }
        }
        return picks
    }
}
