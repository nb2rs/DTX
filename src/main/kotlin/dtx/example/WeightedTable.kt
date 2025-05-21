package dtx.example

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.singleRollable
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import util.NoTransform
import kotlin.random.Random


public open class WeightedTable<T, R>(
    public val tableIdentifier: String,
    entries: List<WeightedRollable<T, R>>,
    override val ignoreModifier: Boolean = false,
    protected val rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    protected val getTargetDropRate: (T) -> Double = ::defaultGetBaseDropRate,
    protected open val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {

    init {

        require(entries.isNotEmpty()) {
            "WeightedTable[$tableIdentifier] entries must not be empty"
        }

        require(entries.distinctBy { it.weight }.size == entries.size) {
            "WeightedTable[$tableIdentifier] entries must not have duplicate weights"
        }
    }


    protected var weightSum: Double = entries.sumOf { it.weight }


    public override val tableEntries: List<WeightedRollable<T, R>> = entries.map(NoTransform())


    public override fun getBaseDropRate(target: T): Double {
        return getTargetDropRate(target)
    }


    override fun rollModifier(percentage: Double): Double {

        if (ignoreModifier) {
            return 1.0
        }

        return rollModifierFunc(percentage)
    }


    public override fun onSelect(target: T, result: RollResult<R>): Unit {
        return onSelectFunc(target, result)
    }

    protected fun checkLowEntries(): Pair<Boolean, WeightedRollable<T, R>> {

        if (tableEntries.isEmpty()) {
            return true to WeightedRollable.Empty()
        }

        if (tableEntries.size == 1) {

            val singleResult = tableEntries.first()

            return true to singleResult
        }

        return false to WeightedRollable.Empty()
    }

    protected fun <E: WeightedRollable<T, R>> getWeightedEntry(
        rollMod: Double,
        rolledWeight: Double,
        usingEntries: Collection<E>
    ): E {
        var rolledWeight = rolledWeight

        usingEntries.asSequence()
            .map { entry -> (entry.weight * rollMod) to entry }
            .forEach { (weight, item) ->

                rolledWeight -= weight

                if (rolledWeight <= 0.0) {
                    return item
                }
            }

        return usingEntries.last()
    }


    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val lowEntries = checkLowEntries()

        if (lowEntries.first) {

            val result = lowEntries.second.roll(target, otherArgs)
            onSelect(target, result)

            return result
        }

        val rollMod = rollModifier(getBaseDropRate(target))
        val rolledWeight = Random.nextDouble(0.0, weightSum)
        val pickedEntry = getWeightedEntry(rollMod, rolledWeight, tableEntries)
        val result = pickedEntry.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }


    public override fun toString(): String = "WeightedTable[$tableIdentifier]"
}


public open class WeightedTableBuilder<T, R> {


    public var tableName: String = "Unnamed Weighted Drop Table"


    public var ignoreModifier: Boolean = false


    public var onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect


    public var targetDropRateFunc: (T) -> Double = { 1.0 }


    public var rollModFunc: (Double) -> Double = ::defaultRollModifier


    private val entries: MutableList<WeightedRollable<T, R>> = mutableListOf()


    public fun onSelect(block: (T, RollResult<R>) -> Unit): WeightedTableBuilder<T, R> {

        onSelectFunc = block

        return this
    }


    public fun rollmodifier(block: (Double) -> Double): WeightedTableBuilder<T, R> {

        rollModFunc = block

        return this
    }


    public fun targetDropRate(block: (T) -> Double): WeightedTableBuilder<T, R> {

        targetDropRateFunc = block

        return this
    }


    public fun name(string: String): WeightedTableBuilder<T, R> {

        tableName = string

        return this
    }


    public fun ignoreModifier(ignore: Boolean): WeightedTableBuilder<T, R> {

        ignoreModifier = ignore

        return this
    }


    public infix fun Double.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R> {

        entries.add(WeightedRollableImpl(this, rollable))

        return this@WeightedTableBuilder
    }


    public inline infix fun Double.weight(entry: R): WeightedTableBuilder<T, R> {

        return weight(Rollable.Single(entry))
    }


    public infix fun Double.weight(block: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R> {
        return weight(singleRollable(block))
    }


    public inline infix fun Int.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R>{
        return toDouble() weight rollable
    }


    public inline infix fun Int.weight(item: R): WeightedTableBuilder<T, R> {
        return weight(Rollable.Single(item))
    }


    public infix fun Int.weight(block: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R> {
        return toDouble().weight(block)
    }


    public fun build(): WeightedTable<T, R> {
        return WeightedTable(
            tableIdentifier = tableName,
            entries = entries,
            ignoreModifier = ignoreModifier,
            rollModifierFunc = rollModFunc,
            getTargetDropRate = targetDropRateFunc,
            onSelectFunc = onSelectFunc
        )
    }
}


public fun <T, R> weightedTable(
    tableName: String = "Unnamed Weighted Table",
    block: WeightedTableBuilder<T, R>.() -> Unit
): WeightedTable<T, R> {

    val builder = WeightedTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
