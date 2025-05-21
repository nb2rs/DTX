package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.singleRollable
import dtx.table.Table


internal class WrappingInt(
    var currentValue: Int,
    val wrapFloor: Int,
    val wrapCeil: Int,
    val onWrapFunc: () -> Unit,
) {


    init {
        currentValue = currentValue.coerceIn(wrapFloor, wrapCeil)
    }


    fun onWrap() {
        onWrapFunc()
    }


    fun inc() {

        currentValue += 1

        if (currentValue > wrapCeil) {
            onWrapFunc()
            currentValue = wrapFloor
        }
    }
}


public class SequentialTable<T, R>(
    public val tableName: String,
    entries: List<Rollable<T, R>>,
    public val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
    public val onTableReset: SequentialTable<T, R>.() -> Unit = { }
): Table<T, R> {


    private var isActive: Boolean = true


    private val pointer = WrappingInt(0, 0, entries.size) {
        this.onTableReset()
    }


    public override val ignoreModifier: Boolean = true


    public override val tableEntries: List<Rollable<T, R>> = entries


    public override fun onSelect(target: T, result: RollResult<R>) {
        this.onSelectFunc(target, result)
    }


    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (this.isActive) {

            val selected = this.tableEntries[this.pointer.currentValue]
            this.pointer.currentValue.inc()
            val result = selected.roll(target, otherArgs)
            this.onSelect(target, result)

            return result
        }

        return RollResult.Nothing()
    }
}


public class SequentialTableBuilder<T, R> {


    public var tableName: String = "Unnamed Sequential Table"


    public var tableValues: MutableList<Rollable<T, R>> = mutableListOf<Rollable<T, R>>()


    public var onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect


    public var onResetFunc: SequentialTable<T, R>.() -> Unit = { }


    public fun name(name: String): SequentialTableBuilder<T, R> {

        this.tableName = name

        return this
    }


    public fun add(vararg rollables: Rollable<T, R>): SequentialTableBuilder<T, R>  {

        this.tableValues.addAll(rollables)

        return this
    }


    public fun add(vararg values: R): SequentialTableBuilder<T, R> {

        values.forEach { singleValue ->
            tableValues.add(Rollable.Single(singleValue))
        }

        return this
    }


    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): SequentialTableBuilder<T, R> {
        return add(singleRollable(block))
    }


    public fun build(): SequentialTable<T, R> {
        return SequentialTable(tableName, tableValues.map { it }, onSelectFunc, onResetFunc)
    }
}

public fun <T, R> sequentialTable(
    tableName: String = "Unnamed Sequential Table",
    block: SequentialTableBuilder<T, R>.() -> Unit
): SequentialTable<T, R> {

    val builder = SequentialTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.block()

    return builder.build()

}