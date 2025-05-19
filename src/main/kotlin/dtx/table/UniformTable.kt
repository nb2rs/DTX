package dtx.table

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.Rollable.Companion.defaultOnSelect

/**
 * A table implementation that selects entries with theoretically equal probability.
 *
 * UniformTable randomly selects one of its entries with equal probability
 * and returns the result of rolling that value.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, used for identification and debugging.
 * @property entries The list of possible entries that can be selected from this table.
 * @property onSelectFunc The lambda to call when any result is selected.
 */
public class UniformTable<T, R>(
    public val tableName: String = "",
    public val entries: List<Rollable<T, R>>,
    public val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {

    /**
     * The collection of entries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     */
    public override val tableEntries: Collection<Rollable<T, R>> = entries

    /**
     * Returns the base drop rate for this table, which is always 1.0, as a uniform selection can't be weighted.
     *
     * @param target The target object for which to get the base drop rate.
     * @return 1.0
     */
    public override fun getBaseDropRate(target: T): Double {
        return 1.0
    }

    /**
     * Indicates that roll modifiers should be ignored for this table.
     */
    public override val ignoreModifier: Boolean = true

    /**
     * Performs a roll operation by randomly selecting one of the entries
     * and rolling it to produce a result.
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val result = entries.random().roll(target, otherArgs)
        onSelectFunc(target, result)

        return result
    }

    /**
     * Returns a string representation of this table.
     *
     * @return [String]
     */
    public override fun toString(): String = "UniformTable[$tableName]"
}

/**
 * Builder class for creating UniformTable instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class UniformTableBuilder<T, R> {

    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed Uniform Table"

    /**
     * The function to call when a result is selected from the table.
     */
    public var onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    /**
     * The list of [Rollable] entries that can be selected from the table.
     */
    public val tableEntries: MutableList<Rollable<T, R>> = mutableListOf<Rollable<T, R>>()

    /**
     * Sets the name of the table.
     *
     * @param string The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(string: String): UniformTableBuilder<T, R> {
        tableName = string
        return this
    }

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): UniformTableBuilder<T, R> {
        onSelect = block
        return this
    }

    /**
     * Adds one or more values to the table.
     *
     * Each value is wrapped in a Rollable.Single to allow for ease-of-use
     *
     * @param valuesToAdd The values to add.
     * @return This builder instance for method chaining.
     */
    public fun add(vararg valuesToAdd: R): UniformTableBuilder<T, R> {
        valuesToAdd.forEach {
            tableEntries.add(Rollable.Single(it))
        }
        return this
    }

    /**
     * Adds one or more Rollable instances to the table.
     *
     * @param valuesToAdd The Rollable instances to add.
     * @return This builder instance for method chaining.
     */
    public fun add(vararg valuesToAdd: Rollable<T, R>): UniformTableBuilder<T, R> {
        valuesToAdd.forEach(tableEntries::add)
        return this
    }

    /**
     * Adds a Rollable instance built using a SingleRollableBuilder.
     *
     * @param block The configuration block for the SingleRollableBuilder.
     * @return This builder instance for method chaining.
     */
    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): UniformTableBuilder<T, R> {
        add(SingleRollableBuilder<T, R>().apply(block).build())
        return this
    }

    /**
     * Builds and returns a UniformTable based on the provided inputs.
     *
     * @return [UniformTable]
     */
    public fun build(): UniformTable<T, R> {
        return UniformTable<T, R>(
            entries = tableEntries.map { it },
            tableName = tableName,
            onSelectFunc = onSelect
        )
    }
}

/**
 * Creates a UniformTable with the specified [Rollable] values as the entries to the table.
 *
 * @param values The [Rollable]s to include in the table.
 * @param tableName The name of the table.
 * @param onSelect The function to call when a result is selected from the table.
 * @return [UniformTable]
 */
public fun <T, R> uniformTable(
    vararg values: Rollable<T, R>,
    tableName: String,
    onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): UniformTable<T, R> {
    return UniformTable<T, R>(tableName, values.toList(), onSelect)
}

/**
 * Creates a [UniformTable] with the specified values.
 *
 * Each value is wrapped in a [Rollable.Single].
 *
 * @param entries The entries to wrap and include in the table.
 * @param tableName The name of the table.
 * @param onSelect The function to call when a result is selected from the table.
 * @return [UniformTable]
 */
public fun <T, R> uniformTable(
    vararg entries: R,
    tableName: String,
    onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): UniformTable<T, R> {
    return UniformTable<T, R>(tableName, entries.map { Rollable.Single(it) }, onSelect)
}

/**
 * Creates a [UniformTable] using [UniformTableBuilder].
 *
 * @param block The configuration block for the UniformTableBuilder.
 * @return [UniformTable].
 */
public fun <T, R> uniformTable(
    block: UniformTableBuilder<T, R>.() -> Unit
): UniformTable<T, R> {
    return UniformTableBuilder<T, R>().apply(block).build()
}
