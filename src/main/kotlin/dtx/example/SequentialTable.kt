package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.table.Table

/**
 * An internal utility class that represents an integer with wrapping behavior.
 *
 * When the value exceeds the ceiling, it wraps back to the floor and triggers a callback.
 *
 * @property currentValue The current value of the integer.
 * @property wrapFloor The minimum value (inclusive) that the integer can have.
 * @property wrapCeil The maximum value (inclusive) that the integer can have.
 * @property onWrapFunc A callback function that is called when the value wraps.
 */
internal class WrappingInt(
    var currentValue: Int,
    val wrapFloor: Int,
    val wrapCeil: Int,
    val onWrapFunc: () -> Unit,
) {

    /**
     * Initializes the instance by ensuring the current value is within the valid range.
     */
    init {
        currentValue = currentValue.coerceIn(wrapFloor, wrapCeil)
    }

    /**
     * Manually triggers the wrap callback.
     */
    fun onWrap() {
        onWrapFunc()
    }

    /**
     * Increments the current value by 1.
     *
     * If the new value exceeds the ceiling, it wraps back to the floor and triggers the wrap callback.
     */
    fun inc() {
        currentValue += 1
        if (currentValue > wrapCeil) {
            onWrapFunc()
            currentValue = wrapFloor
        }
    }
}

/**
 * A [Table] implementation that selects entries in a sequential order.
 *
 * SequentialTable returns the result of rolling each of its values in sequence,
 * wrapping back to the beginning when it reaches the end. When the table wraps,
 * a reset callback can be triggered.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, can be used for identification and debugging.
 * @property entries The list of rollable items in the table.
 * @property onSelectFunc The function to call when a result is selected from the table.
 * @property onTableReset The function to call when the table wraps back to the beginning.
 */
public class SequentialTable<T, R>(
    public val tableName: String,
    entries: List<Rollable<T, R>>,
    public val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
    public val onTableReset: SequentialTable<T, R>.() -> Unit = { }
): Table<T, R> {

    /**
     * Whether this table is active and can produce results.
     */
    private var isActive: Boolean = true

    /**
     * A pointer to the current position in the values list.
     *
     * When the pointer wraps back to the beginning, the onTableReset function is called.
     */
    private val pointer = WrappingInt(0, 0, entries.size) {
        this.onTableReset()
    }

    /**
     * Whether roll modifiers should be ignored for this table.
     */
    public override val ignoreModifier: Boolean = true

    /**
     * The collection of entries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     */
    public override val tableEntries: List<Rollable<T, R>> = entries

    /**
     * Called when a [RollResult] is selected from this table.
     *
     * @param target The target object for which the selection was made.
     * @param result The resulting [RollResult] of the selection.
     */
    public override fun onSelect(target: T, result: RollResult<R>) {
        this.onSelectFunc(target, result)
    }

    /**
     * Performs a roll by selecting the next item in sequence and rolling it.
     *
     * If the table is not active, returns [RollResult.Nothing].
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
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

/**
 * Builder class for creating [SequentialTable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class SequentialTableBuilder<T, R> {

    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed Sequential Table"

    /**
     * The list of rollable items to include in the table.
     */
    public var tableValues: MutableList<Rollable<T, R>> = mutableListOf<Rollable<T, R>>()

    /**
     * The function to call when a result is selected from the table.
     */
    public var onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    /**
     * The function to call when the table wraps back to the beginning.
     */
    public var onResetFunc: SequentialTable<T, R>.() -> Unit = { }

    /**
     * Sets the name of the table.
     *
     * @param name The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(name: String): SequentialTableBuilder<T, R> {
        this.tableName = name
        return this
    }

    /**
     * Adds one or more [Rollable] instances to the table.
     *
     * @param rollables The rollables to add.
     * @return This builder instance for method chaining.
     */
    public fun add(vararg rollables: Rollable<T, R>): SequentialTableBuilder<T, R>  {
        this.tableValues.addAll(rollables)
        return this
    }

    /**
     * Adds one or more values to the table.
     *
     * Each value is wrapped in a [Rollable.Single].
     *
     * @param values The values to add.
     * @return This builder instance for method chaining.
     */
    public fun add(vararg values: R): SequentialTableBuilder<T, R> {
        values.forEach { singleValue ->
            tableValues.add(Rollable.Single(singleValue))
        }
        return this
    }

    /**
     * Adds a rollable created using a [SingleRollableBuilder] to the table.
     *
     * @param block The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): SequentialTableBuilder<T, R> {
        return add(SingleRollableBuilder<T, R>().apply(block).build())
    }

    /**
     * Builds and returns a [SequentialTable] instance based on the current configuration.
     *
     * @return [SequentialTable]
     */
    public fun build(): SequentialTable<T, R> {
        return SequentialTable(tableName, tableValues.map { it }, onSelectFunc, onResetFunc)
    }
}

public fun <T, R> sequentialTable(
    tableName: String = "Unnamed Sequential Table",
    block: SequentialTableBuilder<T, R>.() -> Unit
): SequentialTable<T, R> {
    return SequentialTableBuilder<T, R>()
        .apply { name(tableName) }
        .apply(block).build()
}