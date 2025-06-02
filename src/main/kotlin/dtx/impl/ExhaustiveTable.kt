package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.table.AbstractTableBuilder
import dtx.table.Table
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

public interface ExhaustiveRollable<T, R>: Rollable<T, R> {

    public fun onExhaust(target: T): Unit { }

    public fun isExhausted(): Boolean

    public fun resetExhaustible(): Unit

    public fun remainingRolls(): Int
}

public interface WeightedExhaustiveRollable<T, R>: ExhaustiveRollable<T, R>, WeightedRollable<T, R> {

    override val weight: Double get() = remainingRolls().toDouble()
}

public open class ExhaustiveRollableImpl<T, R>(
    public val rollable: Rollable<T, R>,
    public val totalRolls: Int,
    public val onExhaustFunc: ExhaustiveRollable<T, R>.(T) -> Unit
): ExhaustiveRollable<T, R> {

    private var rollsRemaining: Int = totalRolls

    public override fun remainingRolls(): Int {
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

        val result = rollable.roll(target, otherArgs)
        rollsRemaining--

        onSelect(target, result)
        if (isExhausted()) {
            onExhaust(target)
        }

        return result
    }

    public override fun resetExhaustible() {
        rollsRemaining = totalRolls
    }
}

public class WeightedExhastiveRollableImpl<T, R>(
    rollable: Rollable<T, R>,
    totalRolls: Int,
    onExhaustFunc: WeightedExhaustiveRollable<T, R>.(T) -> Unit
): WeightedExhaustiveRollable<T, R>, ExhaustiveRollableImpl<T, R>(
    rollable,
    totalRolls,
    onExhaustFunc as ExhaustiveRollable<T, R>.(T) -> Unit
) {
    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return super<ExhaustiveRollableImpl>.roll(target, otherArgs)
    }
}

public fun <T, R> ExhaustiveRollable<T, R>.toWeightedExhaustiveRollable(): WeightedExhaustiveRollable<T, R> = object: WeightedExhaustiveRollable<T, R> {

    override fun isExhausted(): Boolean = this@toWeightedExhaustiveRollable.isExhausted()

    override fun resetExhaustible() = this@toWeightedExhaustiveRollable.resetExhaustible()

    override fun remainingRolls(): Int = this@toWeightedExhaustiveRollable.remainingRolls()

    override val rollable: Rollable<T, R> get() = this@toWeightedExhaustiveRollable
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

    public override fun build(): ExhaustiveRollable<T, R> {

        resultSelector?.let {
            return ExhaustiveRollableImpl<T, R>(Rollable.SingleByFun<T, R>(it), totalRolls, onExhaust)
        }

        result?.let {
            return ExhaustiveRollableImpl<T, R>(Rollable.Single<T, R>(it), totalRolls, onExhaust)
        }

        error("Cannot build ExhaustiveSingleRollable with null result and null resultSelector")
    }
}

public interface ExhaustiveTable<T, R>: Table<T, R>, ExhaustiveRollable<T, R> {

    public override val tableEntries: List<ExhaustiveRollable<T, R>>

    public val rollableEntries: List<ExhaustiveRollable<T, R>>
        get() = tableEntries.filterNot(ExhaustiveRollable<T, R>::isExhausted)

    override fun remainingRolls(): Int {
        return tableEntries.sumOf(ExhaustiveRollable<T, R>::remainingRolls)
    }

}

public open class ExhaustiveTableImpl<T, R>(
    public val tableName: String,
    initialItems: List<ExhaustiveRollable<T, R>>,
    private val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
    private val onExhaustFunc: ExhaustiveTableImpl<T, R>.() -> Unit = { }
): ExhaustiveTable<T, R> {

    override val tableEntries: List<ExhaustiveRollable<T, R>> = initialItems.map { it }

    override val rollableEntries: List<ExhaustiveRollable<T, R>>
        get() = tableEntries.filterNot(ExhaustiveRollable<T, R>::isExhausted)

    override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    override fun remainingRolls(): Int = tableEntries.sumOf { it.remainingRolls() }

    public fun reset(): Unit {
        tableEntries.forEach(ExhaustiveRollable<T, R>::resetExhaustible)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        val rollableItems = rollableEntries

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

public open class ExhaustiveTableBuilder<T, R>: AbstractTableBuilder<T, R, ExhaustiveTableImpl<T, R>, ExhaustiveRollable<T, R>, ExhaustiveTableBuilder<T, R>>() {

    override val entries: MutableList<ExhaustiveRollable<T, R>> = mutableListOf<ExhaustiveRollable<T, R>>()

    public var onExhaustFunc: ExhaustiveTableImpl<T, R>.() -> Unit = { }

    public fun onExhaust(block: ExhaustiveTableImpl<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> {

        onExhaustFunc = block

        return this
    }

    public infix fun Int.rolls(entry: ExhaustiveRollable<T, R>): ExhaustiveTableBuilder<T, R> {
        return addEntry(entry)
    }

    public infix fun Int.rolls(entry: Rollable<T, R>): ExhaustiveTableBuilder<T, R> {
        return rolls(ExhaustiveRollableImpl(entry, this) { })
    }

    public infix fun Int.rolls(item: R): ExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.Single(item))
    }

    public inline infix fun Int.rolls(block: ExhaustiveSingleRollableBuilder<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> {

        val built = ExhaustiveSingleRollableBuilder<T, R>()
            .apply { this.totalRolls = this@rolls }
            .apply(block)
            .build()

        return rolls(built)
    }

    public override fun build(): ExhaustiveTableImpl<T, R> {
        return ExhaustiveTableImpl(tableName, entries, onSelectFunc, onExhaustFunc)
    }
}

public inline fun <T, R> uniformExhaustiveTable(
    tableName: String = "Unnamed Exhaustive Table",
    block: ExhaustiveTableBuilder<T, R>.() -> Unit
): ExhaustiveTableImpl<T, R> {

    val builder = ExhaustiveTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}

public class WeightedExhaustiveTableImpl<T, R>(
    tableName: String = "Unnamed Exhaustive Table",
    override val tableEntries: List<WeightedExhaustiveRollable<T, R>>,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
    onExhaustFunc: WeightedExhaustiveTableImpl<T, R>.() -> Unit = { },
): WeightedTable<T, R>, ExhaustiveTableImpl<T, R>(
    tableName,
    tableEntries,
    onSelectFunc,
    onExhaustFunc as ExhaustiveTableImpl<T, R>.() -> Unit
) {

    override val maxRoll: Double get() = tableEntries.sumOf { it.remainingRolls() }.toDouble()

    override val rollableEntries: List<WeightedExhaustiveRollable<T, R>>
        get() = super<ExhaustiveTableImpl>.rollableEntries as List<WeightedExhaustiveRollable<T, R>>

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        val weightedItems = rollableEntries
            .sortedBy(ExhaustiveRollable<T, R>::remainingRolls)

        var rolledWeight = Random.nextDouble(0.0, maxRoll)

        val rollMod = rollModifier(getBaseDropRate(target))
        val pickedEntry = getWeightedEntry<WeightedExhaustiveRollable<T, R>>(rollMod, rolledWeight, weightedItems)
        val result = pickedEntry.roll(target, otherArgs)
        onSelect(target, result)

        if (isExhausted()) {
            onExhaust(target)
        }

        return result
    }
}

public class WeightedExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R>() {

    override fun build(): WeightedExhaustiveTableImpl<T, R> {
        return WeightedExhaustiveTableImpl<T, R>(
            tableName,
            entries.map { it.toWeightedExhaustiveRollable() },
            onSelectFunc,
            onExhaustFunc
        )
    }
}

public inline fun <T, R> weightedExhaustiveTable(
    tableName: String = "Unnamed Weighted Exhaustive Table",
    block: ExhaustiveTableBuilder<T, R>.() -> Unit
): WeightedExhaustiveTableImpl<T, R> {

    val builder = WeightedExhaustiveTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
