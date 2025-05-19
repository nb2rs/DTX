package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.table.Table
import util.SparseMatrix
import kotlin.random.Random

/**
 * A [Table] implementation that selects entries from a two-dimensional matrix.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, can be used for identification and debugging.
 * @property onSelectFun The function to call when a result is selected from the table.
 */
public class MatrixTable<T, R>(
    public val tableName: String,
    matrix: SparseMatrix<Rollable<T, R>>,
    public val onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {

    /**
     * The internal matrix containing the [Rollable] items.
     *
     * This is a copy of the input matrix to ensure that modifications to the original
     * matrix don't affect this table.
     */
    private val internalMatrix = matrix.copy(defaultValue = Rollable.Empty<T, R>())

    /**
     * Called when a [RollResult] is selected from this table.
     *
     * @param target The target object for which the selection was made.
     * @param result The resulting [RollResult] of the selection.
     */
    public override fun onSelect(target: T, result: RollResult<R>): Unit {
        onSelectFun(target, result)
    }

    /**
     * Whether roll modifiers should be ignored for this table.
     */
    public override val ignoreModifier: Boolean = true

    /**
     * The collection of entries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     * This returns all non-default entries from the matrix.
     */
    public override val tableEntries: Collection<Rollable<T, R>> 
        get() {
            val entries = mutableListOf<Rollable<T, R>>()
            internalMatrix.nonDefaults().forEach { (_, _, value) ->
                entries.add(value)
            }
            return entries
        }

    /**
     * Performs a roll by randomly selecting a cell from the matrix and rolling the [Rollable] at that position.
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val pickedRow = Random.nextLong(internalMatrix.rows.toLong() + 1L).toInt()
        val pickedCol = Random.nextLong(internalMatrix.columns.toLong() + 1L).toInt()

        val entry = internalMatrix[pickedRow, pickedCol]
        val result = entry.roll(target, otherArgs)

        onSelect(target, result)

        return result
    }
}

/**
 * Builder class for creating [MatrixTable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class MatrixTableBuilder<T, R> {

    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed Matrix Table"

    /**
     * The maximum row index in the matrix.
     */
    private var maxRow: Int = 0

    /**
     * The maximum column index in the matrix.
     */
    private var maxColumn: Int = 0

    /**
     * The map of items to include in the matrix, keyed by their row and column coordinates.
     */
    private val items = mutableMapOf<Pair<Int, Int>, Rollable<T, R>>()

    /**
     * The function to call when a result is selected from the table.
     */
    public var onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    /**
     * Sets the name of the table.
     *
     * @param name The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(name: String): MatrixTableBuilder<T, R> = apply {
        this.tableName = name
    }

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): MatrixTableBuilder<T, R> = apply {
        this.onSelectFun = block
    }

    /**
     * Adds a [Rollable] to the matrix at the specified position.
     *
     * @param row The row index.
     * @param column The column index.
     * @param value The rollable to add.
     * @return This builder instance for method chaining.
     */
    public fun addItemAt(row: Int, column: Int, value: Rollable<T, R>): MatrixTableBuilder<T, R> = apply {
        if (row > maxRow) {
            maxRow = row
        }
        if (column > maxColumn) {
            maxColumn = column
        }
        items[Pair(row, column)] = value
    }

    /**
     * Adds a value to the matrix at the specified position.
     *
     * The value is wrapped in a [Rollable.Single].
     *
     * @param row The row index.
     * @param column The column index.
     * @param value The value to add.
     * @return This builder instance for method chaining.
     */
    public fun addItemAt(
        row: Int,
        column: Int,
        value: R
    ): MatrixTableBuilder<T, R> {
        return addItemAt(row, column, Rollable.Single<T, R>(value))
    }

    /**
     * Adds a rollable created using a [SingleRollableBuilder] to the matrix at the specified position.
     *
     * @param row The row index.
     * @param column The column index.
     * @param block The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public fun addItemAt(
        row: Int,
        column: Int,
        block: SingleRollableBuilder<T, R>.() -> Unit
    ): MatrixTableBuilder<T, R> {
        return addItemAt(row, column, SingleRollableBuilder<T, R>().apply(block).build())
    }

    /**
     * Builds and returns a [MatrixTable] instance based on the current configuration.
     *
     * @return [MatrixTable]
     */
    public fun build(): MatrixTable<T, R> {

        val matrix = SparseMatrix<Rollable<T, R>>(
            rows = maxRow,
            columns = maxColumn,
            defaultValue = Rollable.Empty<T, R>(),
            returnDefaultOnOob = true
        )

        items.forEach { (coords, value) ->

            val (row, column) = coords
            matrix[row, column] = value
        }

        return MatrixTable(tableName, matrix, onSelectFun)
    }
}


/**
 * Creates a [MatrixTable] using a builder.
 *
 * @param tableName The name of the table.
 * @param block The builder lambda for the [MatrixTable].
 * @return [MatrixTable]
 * */
public inline fun <T, R> matrixTable(
    tableName: String = "Unnamed Matrix Table",
    block: MatrixTableBuilder<T, R>.() -> Unit
): MatrixTable<T, R> {
    return MatrixTableBuilder<T, R>()
        .apply { name(tableName) }
        .apply(block).build()
}
