package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.singleRollable
import dtx.table.Table
import dtx.util.SparseMatrix
import kotlin.random.Random

public class MatrixTable<T, R>(
    public val tableName: String,
    matrix: SparseMatrix<Rollable<T, R>>,
    public val onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {


    private val internalMatrix = matrix.copy(defaultValue = Rollable.Empty<T, R>())


    public override fun onSelect(target: T, result: RollResult<R>): Unit {
        onSelectFun(target, result)
    }


    public override val ignoreModifier: Boolean = true


    public override val tableEntries: Collection<Rollable<T, R>> 
        get() {

            val entries = mutableListOf<Rollable<T, R>>()

            internalMatrix.nonDefaults().forEach { (_, _, value) ->
                entries.add(value)
            }

            return entries
        }


    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val pickedRow = Random.nextLong(internalMatrix.rows.toLong() + 1L).toInt()
        val pickedCol = Random.nextLong(internalMatrix.columns.toLong() + 1L).toInt()
        val entry = internalMatrix[pickedRow, pickedCol]
        val result = entry.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }
}

public class MatrixTableBuilder<T, R> {


    public var tableName: String = "Unnamed Matrix Table"


    private var maxRow: Int = 0


    private var maxColumn: Int = 0


    private val items = mutableMapOf<Pair<Int, Int>, Rollable<T, R>>()


    public var onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect


    public fun name(name: String): MatrixTableBuilder<T, R> {

        tableName = name

        return this
    }


    public fun onSelect(block: (T, RollResult<R>) -> Unit): MatrixTableBuilder<T, R> {

        onSelectFun = block

        return this
    }


    public fun addItemAt(row: Int, column: Int, value: Rollable<T, R>): MatrixTableBuilder<T, R> = apply {

        if (row > maxRow) {
            maxRow = row
        }

        if (column > maxColumn) {
            maxColumn = column
        }

        items[Pair(row, column)] = value
    }


    public fun addItemAt(
        row: Int,
        column: Int,
        value: R
    ): MatrixTableBuilder<T, R> {
        return addItemAt(row, column, Rollable.Single<T, R>(value))
    }


    public fun addItemAt(
        row: Int,
        column: Int,
        block: SingleRollableBuilder<T, R>.() -> Unit
    ): MatrixTableBuilder<T, R> {
        return addItemAt(row, column, singleRollable(block))
    }


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



public inline fun <T, R> matrixTable(
    tableName: String = "Unnamed Matrix Table",
    block: MatrixTableBuilder<T, R>.() -> Unit
): MatrixTable<T, R> {

    val builder = MatrixTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
