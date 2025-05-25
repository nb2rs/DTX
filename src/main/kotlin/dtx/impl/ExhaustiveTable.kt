package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.table.Table
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

public interface ExhaustiveRollable<T, R>: Rollable<T, R> {

    public fun onExhaust(target: T): Unit { }

    public fun isExhausted(): Boolean

    public fun resetExhaustible(): Unit
}

public data class ExhaustiveRollableEntry<T, R>(
    public val entry: Rollable<T, R>,
    public val totalRolls: Int,
    public val onExhaustFunc: ExhaustiveRollableEntry<T, R>.(T) -> Unit = { }
): ExhaustiveRollable<T, R> {

    private var rollsRemaining: Int = totalRolls

    public fun getRemainingRolls(): Int {
        return rollsRemaining
    }

    public override fun onExhaust(target: T): Unit {
        onExhaustFunc(target)
    }

    public override fun isExhausted(): Boolean {
        return rollsRemaining <= 0
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        rollsRemaining--
        val result = entry.roll(target, otherArgs)

        if (rollsRemaining == 0) {
            onExhaust(target)
        }

        return result
    }

    public override fun resetExhaustible() {
        rollsRemaining = totalRolls
    }
}

internal class PositiveInt(
    initialValue: Int,
    val throwOnError: Boolean = false
): ReadWriteProperty<Any?, Int> {

    var intValue = if (initialValue < 0) {

        if (throwOnError) {
            error("Negative intValue for PositiveInt")
        } else {
            1
        }
    } else {
        initialValue
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return intValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {

        if (value < 0) {

            if (throwOnError) {
                error("SetValue - Negative intValue for PositiveInt")
            }

            intValue = 0
        }

        intValue = value
    }

}

public class ExhaustiveSingleRollableBuilder<T, R>: SingleRollableBuilder<T, R>() {

    public var onExhaust: ExhaustiveRollable<T, R>.(T) -> Unit = ExhaustiveRollable<T, R>::onExhaust
    public var totalRolls: Int by PositiveInt(1, throwOnError = false)

    public fun totalRolls(value: Int): ExhaustiveSingleRollableBuilder<T, R>  {

        totalRolls = value

        return this
    }

    public fun onExhaust(block: ExhaustiveRollable<T, R>.(T) -> Unit): ExhaustiveSingleRollableBuilder<T, R> {

        onExhaust = block

        return this
    }

    public override fun build(): Rollable<T, R> {

        resultSelector?.let {
            return ExhaustiveRollableEntry<T, R>(Rollable.SingleByFun<T, R>(it), totalRolls, onExhaust)
        }

        result?.let {
            return ExhaustiveRollableEntry<T, R>(Rollable.Single<T, R>(it), totalRolls, onExhaust)
        }

        error("Cannot build ExhaustiveSingleRollable with null result and null resultSelector")
    }
}

public open class ExhaustiveTable<T, R>(
    public val tableName: String,
    initialItems: List<ExhaustiveRollableEntry<T, R>>,
    private val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
    private val onExhaustFunc: ExhaustiveTable<T, R>.() -> Unit = { }
): Table<T, R>, ExhaustiveRollable<T, R> {

    override val ignoreModifier: Boolean = true

    override val tableEntries: List<ExhaustiveRollableEntry<T, R>> = initialItems.map { it.copy() }

    override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    public fun reset(): Unit {
        tableEntries.forEach(ExhaustiveRollableEntry<T, R>::resetExhaustible)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val rollableItems = tableEntries.filter { it.getRemainingRolls() > 0 }

        val rolled = rollableItems.random()
        val result = rolled.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }

    public override fun onExhaust(target: T) {
        onExhaustFunc()
    }

    public override fun isExhausted(): Boolean {
        return tableEntries.all { it.isExhausted() }
    }

    public override fun resetExhaustible() {
        tableEntries.forEach { it.resetExhaustible() }
    }
}

public open class ExhaustiveTableBuilder<T, R> {

    public var tableName: String = "Unnamed Exhaustive Table"

    protected val items: MutableList<ExhaustiveRollableEntry<T, R>> = mutableListOf<ExhaustiveRollableEntry<T, R>>()

    public var onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    public var onExhaust: ExhaustiveTable<T, R>.() -> Unit = { }

    public fun onSelect(block: (T, RollResult<R>) -> Unit): ExhaustiveTableBuilder<T, R> {

        onSelect = block

        return this
    }

    public fun onExhaust(block: ExhaustiveTable<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> {

        onExhaust = block

        return this
    }

    public fun name(string: String): ExhaustiveTableBuilder<T, R> {

        tableName = string

        return this
    }

    public infix fun Int.rolls(entry: ExhaustiveRollableEntry<T, R>): ExhaustiveTableBuilder<T, R> {

        items.add(entry.copy(totalRolls = this))

        return this@ExhaustiveTableBuilder
    }

    public infix fun Int.rolls(entry: Rollable<T, R>): ExhaustiveTableBuilder<T, R> {
        return rolls(ExhaustiveRollableEntry(entry, this))
    }

    public infix fun Int.rolls(item: R): ExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.Single(item))
    }

    public inline infix fun Int.rolls(block: ExhaustiveSingleRollableBuilder<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> {
        return rolls(ExhaustiveSingleRollableBuilder<T, R>().apply(block).build())
    }

    public open fun build(): ExhaustiveTable<T, R> {
        return ExhaustiveTable(tableName, items, onSelect, onExhaust)
    }
}

public inline fun <T, R> uniformExhaustiveTable(
    tableName: String = "Unnamed Exhaustive Table",
    block: ExhaustiveTableBuilder<T, R>.() -> Unit
): ExhaustiveTable<T, R> {

    val builder = ExhaustiveTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}

public class WeightedExhaustiveTable<T, R>(
    tableName: String = "Unnamed Exhaustive Table",
    initialItems: List<ExhaustiveRollableEntry<T, R>>,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
): ExhaustiveTable<T, R>(tableName, initialItems, onSelectFunc) {

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val rollableItems = tableEntries
            .filter { it.getRemainingRolls() > 0 }

        val weightedItems = rollableItems
            .map { it.getRemainingRolls().toDouble() to it }
            .sortedBy { it.first }

        var rolledWeight = Random.nextDouble(0.0, weightedItems.sumOf { it.first })

        weightedItems.forEach { (weight, item) ->

            rolledWeight -= weight

            if (rolledWeight <= 0.0) {

                val result = item.roll(target, otherArgs)
                onSelect(target, result)

                return result
            }
        }

        val lastItem = weightedItems.last()
        val result = lastItem.second.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }
}

public class WeightedExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R>() {

    override fun build(): WeightedExhaustiveTable<T, R> {
        return WeightedExhaustiveTable<T, R>(tableName, items, onSelect)
    }
}

public inline fun <T, R> weightedExhaustiveTable(
    tableName: String = "Unnamed Weighted Exhaustive Table",
    block: ExhaustiveTableBuilder<T, R>.() -> Unit
): WeightedExhaustiveTable<T, R> {

    val builder = WeightedExhaustiveTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()

}