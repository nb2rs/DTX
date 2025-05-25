package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.flattenToList
import dtx.core.singleRollable
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import dtx.util.NoTransform
import kotlin.random.Random

public interface ChanceRollable<T, R>: Rollable<T, R> {

    public val chance: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return chance
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    private data object Empty: ChanceRollable<Any?, Any?> {

        override val chance: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()
    }

    public companion object {

        public fun <T, R> Empty(): ChanceRollable<T, R> {
            return Empty as ChanceRollable<T, R>
        }
    }
}

internal data class ChanceRollableImpl<T, R>(
    override val chance: Double,
    override val rollable: Rollable<T, R>
): ChanceRollable<T, R>



public open class MultiChanceTable<T, R>(
    public val tableName: String,
    entries: List<ChanceRollable<T, R>>,
    override val ignoreModifier: Boolean = false,
    protected val rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    protected open val getBaseDropRateFunc: (T) -> Double = ::defaultGetBaseDropRate,
    public open val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {

    init {

        require(entries.isNotEmpty()) {
            "table[$tableName] entries must not be empty"
        }
    }


    protected val maxRollChance: Double = 100.0 + Double.MIN_VALUE


    public override val tableEntries: List<ChanceRollable<T, R>> = entries.map(NoTransform())


    public override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }


    public override fun rollModifier(percentage: Double): Double {
        return rollModifierFunc(percentage)
    }


    public override fun getBaseDropRate(target: T): Double {
        return getBaseDropRateFunc(target)
    }


    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val pickedEntries = mutableListOf<ChanceRollable<T, R>>()
        val modifier = rollModifier(getBaseDropRate(target))

        tableEntries.forEach { entry ->

            if (entry.chance == 100.0) {

                pickedEntries.add(entry)

                return@forEach
            }

            val roll = Random.nextDouble(0.0, maxRollChance)

            if (roll * modifier <= entry.chance) {
                pickedEntries.add(entry)
            }
        }

        val results = pickedEntries.map { it.roll(target, otherArgs) }.flattenToList()

        onSelect(target, results)

        return results
    }


    public override fun toString(): String = "MultiChanceTable[$tableName]"
}


public class MultiChanceTableBuilder<T, R> {


    public var tableName: String = "Unnamed MultiChance Table"


    public var ignoreModifier: Boolean = false


    private val entries = mutableListOf<ChanceRollable<T, R>>()


    public var targetDropRate: (T) -> Double = { 1.0 }


    public var onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect


    public var rollModifier: (Double) -> Double = ::defaultRollModifier


    public fun name(string: String): MultiChanceTableBuilder<T, R> {
        tableName = string

        return this
    }


    public fun onSelect(block: (T, RollResult<R>) -> Unit): MultiChanceTableBuilder<T, R> {

        onSelect = block

        return this
    }


    public fun targetDropRate(block: (T) -> Double): MultiChanceTableBuilder<T, R> {

        targetDropRate = block

        return this
    }


    public fun rollModifier(block: (Double) -> Double): MultiChanceTableBuilder<T, R> {

        rollModifier = block

        return this
    }


    public infix fun Percent.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {

        entries.add(ChanceRollableImpl(this.value, rollable))

        return this@MultiChanceTableBuilder
    }


    public infix fun Percent.chance(item: R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.Single(item))
    }


    public infix fun Percent.chance(block: SingleRollableBuilder<T, R>.() -> Unit): MultiChanceTableBuilder<T, R> {
        return chance(singleRollable(block))
    }


    public fun build(): MultiChanceTable<T, R> {
        return MultiChanceTable(
            tableName = tableName,
            entries = entries,
            ignoreModifier = ignoreModifier,
            rollModifierFunc = rollModifier,
            getBaseDropRateFunc = targetDropRate,
            onSelectFunc = onSelect
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
