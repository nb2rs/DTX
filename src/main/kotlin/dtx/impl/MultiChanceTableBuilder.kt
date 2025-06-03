package dtx.impl

import dtx.core.ResultSelector
import dtx.core.Rollable
import dtx.table.AbstractTableBuilder

public open class MultiChanceTableBuilder<T, R>: AbstractTableBuilder<T, R, MultiChanceTable<T, R>, ChanceRollable<T, R>, MultiChanceTableBuilder<T, R>>() {

    override val entries: MutableList<ChanceRollable<T, R>> = mutableListOf()

    public infix fun Percent.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {

        addEntry(ChanceRollableImpl(value, rollable))

        return this@MultiChanceTableBuilder
    }

    public infix fun Percent.chance(entry: R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.Single(entry))
    }

    public infix fun Percent.chance(entryBlock: ResultSelector<T, R>): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.SingleByFun(entryBlock))
    }

    public infix fun Int.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {
        return percent.chance(rollable = rollable)
    }

    public infix fun Int.chance(entry: R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.Single(entry))
    }

    public infix fun Int.chance(entryBlock: ResultSelector<T, R>): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.SingleByFun(entryBlock))
    }

    override fun build(): MultiChanceTable<T, R> {
        return MultiChanceTableImpl(
            tableName,
            entries,
            shouldRollFunc,
            getRollModFunc,
            getDropRateFunc,
            onSelectFunc
        )
    }
}

public inline fun <T, R> multiChanceTable(
    tableName: String = "Unnamed Multi Chance Table",
    block: MultiChanceTableBuilder<T, R>.() -> Unit
): MultiChanceTable<T, R> {

    val builder = MultiChanceTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
