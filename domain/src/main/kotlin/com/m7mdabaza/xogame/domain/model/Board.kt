package com.m7mdabaza.xogame.domain.model

/**
 * An immutable square board of [size] × [size] cells. Every change returns a
 * new board.
 */
class Board private constructor(
    val size: Int,
    private val cells: List<Player?>,
) {

    init {
        require(size > 0) { "Board size must be positive, was $size" }
        require(cells.size == size * size) { "Expected ${size * size} cells, got ${cells.size}" }
    }

    /** True if [cell] lies on the board. */
    operator fun contains(cell: Cell): Boolean =
        cell.row in 0 until size && cell.col in 0 until size

    /** The mark at [cell], or null if it is empty. */
    operator fun get(cell: Cell): Player? = cells[indexFor(cell)]

    /** The mark at ([row], [col]), or null if it is empty. */
    operator fun get(row: Int, col: Int): Player? = get(Cell(row, col))

    fun isEmpty(cell: Cell): Boolean = get(cell) == null

    /** A copy of this board with [player] placed on the empty [cell]. */
    fun with(cell: Cell, player: Player): Board {
        require(isEmpty(cell)) { "$cell is already taken by ${get(cell)}" }
        return Board(size, cells.toMutableList().apply { set(indexFor(cell), player) })
    }

    /** A copy of this board with [cell] cleared. */
    fun without(cell: Cell): Board =
        Board(size, cells.toMutableList().apply { set(indexFor(cell), null) })

    /** All cells, row by row. */
    val allCells: List<Cell>
        get() = List(size * size) { Cell(it / size, it % size) }

    val emptyCells: List<Cell>
        get() = allCells.filter { cells[it.row * size + it.col] == null }

    val isFull: Boolean
        get() = cells.none { it == null }

    val isEmpty: Boolean
        get() = cells.all { it == null }

    private fun indexFor(cell: Cell): Int {
        require(cell in this) { "$cell is outside a ${size}x$size board" }
        return cell.row * size + cell.col
    }

    override fun equals(other: Any?): Boolean =
        other is Board && other.size == size && other.cells == cells

    override fun hashCode(): Int = 31 * size + cells.hashCode()

    /** Rows of `X`, `O` and `.`, separated by `/`, e.g. `X.O/.X./..O`. */
    override fun toString(): String =
        cells.chunked(size).joinToString("/") { row -> row.joinToString("") { it?.name ?: "." } }

    companion object {
        fun empty(size: Int): Board = Board(size, List(size * size) { null })
    }
}
