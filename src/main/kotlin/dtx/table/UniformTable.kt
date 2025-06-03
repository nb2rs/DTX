package dtx.table

import dtx.core.ArgMap
import dtx.core.OnSelect
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll
import dtx.core.singleRollable

public class UniformTable<T, R>(
    public val tableName: String = "",
    public override val tableEntries: List<Rollable<T, R>>,
    private val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    private val onSelectFunc: OnSelect<T, R> = ::defaultOnSelect
): Table<T, R> {

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        val result = tableEntries.random().roll(target, otherArgs)
        onSelectFunc(target, result)

        return result
    }

    public override fun toString(): String = "UniformTable[$tableName]"
}

public open class UniformTableBuilder<T, R>: AbstractTableBuilder<T, R, UniformTable<T, R>, Rollable<T, R>, UniformTableBuilder<T, R>>() {

    override val entries: MutableList<Rollable<T, R>> = mutableListOf<Rollable<T, R>>()

    public fun add(vararg valuesToAdd: R): UniformTableBuilder<T, R> {
        
        valuesToAdd.forEach {
            entries.add(Rollable.Single(it))
        }
        
        return this
    }

    public fun add(vararg valuesToAdd: Rollable<T, R>): UniformTableBuilder<T, R> {
        
        valuesToAdd.forEach(entries::add)
        
        return this
    }


    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): UniformTableBuilder<T, R> {
        
        add(singleRollable(block))
        
        return this
    }

    public override fun build(): UniformTable<T, R> {
        return UniformTable<T, R>(
            tableName = tableName,
            tableEntries = entries,
            onSelectFunc = onSelectFunc
        )
    }
}

public fun <T, R> uniformTable(
    tableName: String = "Unnamed Uniform Table",
    block: UniformTableBuilder<T, R>.() -> Unit
): UniformTable<T, R> {

    val builder = UniformTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
