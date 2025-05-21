package dtx.table

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.singleRollable


public class UniformTable<T, R>(
    public val tableName: String = "",
    public val entries: List<Rollable<T, R>>,
    public val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {


    public override val tableEntries: Collection<Rollable<T, R>> = entries


    public override fun getBaseDropRate(target: T): Double {
        return 1.0
    }


    public override val ignoreModifier: Boolean = true


    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val result = entries.random().roll(target, otherArgs)
        onSelectFunc(target, result)

        return result
    }


    public override fun toString(): String = "UniformTable[$tableName]"
}


public class UniformTableBuilder<T, R> {


    public var tableName: String = "Unnamed Uniform Table"


    public var onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect


    public val tableEntries: MutableList<Rollable<T, R>> = mutableListOf<Rollable<T, R>>()


    public fun name(string: String): UniformTableBuilder<T, R> {
        
        tableName = string
        
        return this
    }


    public fun onSelect(block: (T, RollResult<R>) -> Unit): UniformTableBuilder<T, R> {
        
        onSelect = block
        
        return this
    }


    public fun add(vararg valuesToAdd: R): UniformTableBuilder<T, R> {
        
        valuesToAdd.forEach {
            tableEntries.add(Rollable.Single(it))
        }
        
        return this
    }


    public fun add(vararg valuesToAdd: Rollable<T, R>): UniformTableBuilder<T, R> {
        
        valuesToAdd.forEach(tableEntries::add)
        
        return this
    }


    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): UniformTableBuilder<T, R> {
        
        add(singleRollable(block))
        
        return this
    }


    public fun build(): UniformTable<T, R> {
        return UniformTable<T, R>(
            entries = tableEntries.map { it },
            tableName = tableName,
            onSelectFunc = onSelect
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
