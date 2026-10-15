package com.example.core.model

data class Square(
    val file: Int, // 0..7 (a..h)
    val rank: Int  // 0..7 (1..8)
) {
    val index: Int get() = rank * 8 + file

    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    val isLight: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromIndex(index: Int): Square {
            require(index in 0..63) { "Invalid square index: $index" }
            return Square(file = index % 8, rank = index / 8)
        }

        fun fromAlgebraic(name: String): Square {
            require(name.length == 2) { "Invalid square string: $name" }
            val file = name[0].lowercaseChar() - 'a'
            val rank = name[1].digitToInt() - 1
            require(file in 0..7 && rank in 0..7) { "Coordinates out of bounds: $name" }
            return Square(file, rank)
        }
    }
}
