package dtx.impl

import dtx.core.ArgMap
import dtx.core.OnSelect
import dtx.core.ResultSelector
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.ShouldRoll
import dtx.core.SingleRollableBuilder
import dtx.core.defaultShouldRoll
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
    private val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    private val onSelectFunc: OnSelect<T, R> = ::defaultOnSelect,
    private val onExhaustFunc: ExhaustiveRollable<T, R>.(T) -> Unit = { },
): ExhaustiveRollable<T, R> {

    private var rollsRemaining: Int = totalRolls

    public override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    public override fun remainingRolls(): Int {
        return rollsRemaining
    }

    public override fun onExhaust(target: T): Unit {
        return onExhaustFunc(target)
    }

    public override fun isExhausted(): Boolean {
        return rollsRemaining <= 0
    }

    public override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

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

public class WeightedExhaustiveRollableImpl<T, R>(
    rollable: Rollable<T, R>,
    totalRolls: Int,
    shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    onSelectFunc: OnSelect<T, R> = ::defaultOnSelect,
    onExhaustFunc: WeightedExhaustiveRollable<T, R>.(T) -> Unit
): WeightedExhaustiveRollable<T, R>, ExhaustiveRollableImpl<T, R>(
    rollable, totalRolls, shouldRollFunc, onSelectFunc,
    onExhaustFunc as ExhaustiveRollable<T, R>.(T) -> Unit
) {
    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return super<ExhaustiveRollableImpl>.roll(target, otherArgs)
    }
}

public fun <T, R> ExhaustiveRollable<T, R>.toWeightedExhaustiveRollable(byRollable: Rollable<T, R>): WeightedExhaustiveRollable<T, R> = object: WeightedExhaustiveRollable<T, R> {

    override fun shouldRoll(target: T): Boolean {
        return this@toWeightedExhaustiveRollable.shouldRoll(target)
    }

    override fun isExhausted(): Boolean {
        return this@toWeightedExhaustiveRollable.isExhausted()
    }

    override fun resetExhaustible(): Unit {
        return this@toWeightedExhaustiveRollable.resetExhaustible()
    }

    override fun remainingRolls(): Int {
        return this@toWeightedExhaustiveRollable.remainingRolls()
    }

    override val rollable = byRollable
}

public class ExhaustiveSingleRollableBuilder<T, R>: SingleRollableBuilder<T, R>() {

    public var onExhaustFunc: ExhaustiveRollable<T, R>.(T) -> Unit = { }
    public var totalRolls: Int = 1

    public fun totalRolls(value: Int): ExhaustiveSingleRollableBuilder<T, R>  {

        totalRolls = value

        return this
    }

    public fun onExhaust(block: ExhaustiveRollable<T, R>.(T) -> Unit): ExhaustiveSingleRollableBuilder<T, R> {

        onExhaustFunc = block

        return this
    }

    public override fun build(): ExhaustiveRollable<T, R> {

        resultSelector?.let {
            return ExhaustiveRollableImpl<T, R>(Rollable.SingleByFun<T, R>(it), totalRolls, shouldRollFunc, onSelectFunc, onExhaustFunc)
        }

        result?.let {
            return ExhaustiveRollableImpl<T, R>(Rollable.Single<T, R>(it), totalRolls, shouldRollFunc, onSelectFunc, onExhaustFunc)
        }

        error("ExhaustiveSingleRollable must have at least result or resultSelector defined")
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
    private val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    private val onSelectFunc: OnSelect<T, R> = ::defaultOnSelect,
    private val onExhaustFunc: ExhaustiveTableImpl<T, R>.() -> Unit = { }
): ExhaustiveTable<T, R> {

    override val tableEntries: List<ExhaustiveRollable<T, R>> = initialItems.map { it }

    override val rollableEntries: List<ExhaustiveRollable<T, R>>
        get() = tableEntries.filterNot(ExhaustiveRollable<T, R>::isExhausted)

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    override fun remainingRolls(): Int {
        return tableEntries.sumOf { it.remainingRolls() }
    }

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

    public override fun onExhaust(target: T): Unit {
        return onExhaustFunc()
    }

    public override fun isExhausted(): Boolean {
        return tableEntries.all { it.isExhausted() }
    }

    public override fun resetExhaustible(): Unit {
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

    public open infix fun Int.rolls(entry: Rollable<T, R>): ExhaustiveTableBuilder<T, R> {
        return rolls(ExhaustiveRollableImpl(entry, this) { })
    }

    public open infix fun Int.rolls(item: R): ExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.Single(item))
    }

    public open infix fun Int.rollsBy(selector: ResultSelector<T, R>): ExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.SingleByFun(selector))
    }

    public open infix fun Int.rolls(block: ExhaustiveSingleRollableBuilder<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> {

        val built = ExhaustiveSingleRollableBuilder<T, R>()
            .apply { this.totalRolls = this@rolls }
            .apply(block)
            .build()

        return rolls(built)
    }

    public override fun build(): ExhaustiveTableImpl<T, R> {
        return ExhaustiveTableImpl(tableName, entries, shouldRollFunc, onSelectFunc, onExhaustFunc)
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
    shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    onSelectFunc: OnSelect<T, R> = ::defaultOnSelect,
    onExhaustFunc: WeightedExhaustiveTableImpl<T, R>.() -> Unit = { },
): WeightedTable<T, R>, ExhaustiveTableImpl<T, R>(
    tableName,
    tableEntries,
    shouldRollFunc,
    onSelectFunc,
    onExhaustFunc as ExhaustiveTableImpl<T, R>.() -> Unit
) {

    override val maxRoll: Double get() = tableEntries.sumOf { it.remainingRolls() }.toDouble()

    override val rollableEntries: List<WeightedExhaustiveRollable<T, R>>
        get() = super<ExhaustiveTableImpl>.rollableEntries as List<WeightedExhaustiveRollable<T, R>>

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        var rolledWeight = Random.nextDouble(0.0, maxRoll)
        val rollMod = rollModifier(target, getBaseDropRate(target))
        val weightedItems = rollableEntries
            .sortedBy(ExhaustiveRollable<T, R>::remainingRolls)
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

    private val _entries: MutableList<WeightedExhaustiveRollable<T, R>> = mutableListOf()

    public override val entries: MutableList<ExhaustiveRollable<T, R>> get() = super.entries

    public fun addEntry(entry: WeightedExhaustiveRollable<T, R>): WeightedExhaustiveTableBuilder<T, R> {

        _entries.add(entry)

        return this
    }

    public infix fun Int.rolls(entry: WeightedExhaustiveRollable<T, R>): WeightedExhaustiveTableBuilder<T, R> {
        return addEntry(entry)
    }

    public override infix fun Int.rolls(entry: Rollable<T, R>): WeightedExhaustiveTableBuilder<T, R> {
        return rolls(WeightedExhaustiveRollableImpl(entry, this) { })
    }

    public override infix fun Int.rolls(item: R): WeightedExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.Single(item))
    }

    public override infix fun Int.rollsBy(selector: ResultSelector<T, R>): WeightedExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.SingleByFun(selector))
    }

    public override infix fun Int.rolls(block: ExhaustiveSingleRollableBuilder<T, R>.() -> Unit): WeightedExhaustiveTableBuilder<T, R> {

        val builder = ExhaustiveSingleRollableBuilder<T, R>()
            .apply { this.totalRolls = this@rolls }
            .apply(block)

        builder.build()

        val rollable = if (builder.resultSelector == null) {
            // single result isnt null
            Rollable.Single<T, R>(builder.result!!, builder.shouldRollFunc, onSelectFunc)
        } else {
            Rollable.SingleByFun<T, R>(builder.resultSelector!!, builder.shouldRollFunc, onSelectFunc)
        }

        val new = WeightedExhaustiveRollableImpl<T, R>(
            rollable,
            builder.totalRolls,
            builder.shouldRollFunc,
            builder.onSelectFunc,
            builder.onExhaustFunc
        )

        return rolls(new)
    }

    override fun build(): WeightedExhaustiveTableImpl<T, R> {
        return WeightedExhaustiveTableImpl<T, R>(
            tableName,
            _entries,
            shouldRollFunc,
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
