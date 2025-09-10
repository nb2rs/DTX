package dtx.impl

import dtx.core.AbstractRollableBuilder
import dtx.core.AbstractRollableHooksBuilder
import dtx.core.ArgMap
import dtx.core.DefaultRollableHooks
import dtx.core.OnExhaust
import dtx.core.OnSelect
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.RollableHooks
import dtx.core.ShouldInclude
import dtx.core.Single
import dtx.core.SingleRollableBuilder
import dtx.core.TransformResult
import dtx.core.VetoRoll
import dtx.table.AbstractTableBuilder
import dtx.table.AbstractTableHooksBuilder
import dtx.table.Table
import dtx.table.TableHooks

public interface ExhaustiveRollable<T, R>: WeightedRollable<T, R>, ExhaustiveRollableHooks<T, R> {

    public val initialRolls: Int

    public var rolls: Int

    public override val weight: Double
        get() = rolls.toDouble()

    public override fun includeInRoll(onTarget: T): Boolean {
        return isExhausted()
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (isExhausted(this)) {
            return RollResult.Nothing()
        }

        if (vetoRoll(target)) {
            return onRollVetoed(target)
        }

        val result = selectResult(target, otherArgs)
        val transformed = transformResult(target, result)
        rolls -= 1
        if (isExhausted(this)) {
            onExhaust(target)
        }

        return transformed
    }
}

public interface ExhaustiveRollableHooks<T, R>: RollableHooks<T, R> {

    public fun onExhaust(target: T): Unit

    public fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean

    public fun resetExhaustible(rollable: ExhaustiveRollable<T, R>): Unit

    public fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>): Unit

    public companion object {
        public fun <T, R> Default(): ExhaustiveRollableHooks<T, R> {
            return DefaultExhaustiveRollableHooks as ExhaustiveRollableHooks<T, R>
        }
    }
}

internal data object DefaultExhaustiveRollableHooks: ExhaustiveRollableHooks<Any?, Any?>, RollableHooks<Any?, Any?> by DefaultRollableHooks {

    override fun onExhaust(target: Any?): Unit {
        return Unit
    }

    override fun isExhausted(rollable: ExhaustiveRollable<Any?, Any?>): Boolean {
        return rollable.rolls < 1
    }

    override fun resetExhaustible(rollable: ExhaustiveRollable<Any?, Any?>): Unit {
        rollable.rolls = rollable.initialRolls
    }

    override fun incrementExhaustible(rollable: ExhaustiveRollable<Any?, Any?>) {
        rollable.rolls -= 1
    }

}

public class ExhaustiveRollableHooksImpl<T, R>(
    private val baseRollableHooks: RollableHooks<T, R> = RollableHooks.Default<T, R>(),
    private val onExhaustFunc: (T) -> Unit = ExhaustiveRollableHooks.Default<T, R>()::onExhaust,
    private val isExhaustedFunc: ExhaustiveRollable<T, R>.() -> Boolean = ExhaustiveRollableHooks.Default<T, R>()::isExhausted,
    private val resetExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = ExhaustiveRollableHooks.Default<T, R>()::resetExhaustible,
    private val incrementExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = ExhaustiveRollableHooks.Default<T, R>()::incrementExhaustible,
): ExhaustiveRollableHooks<T, R>, RollableHooks<T, R> by baseRollableHooks {

    public override fun onExhaust(target: T): Unit {
        return onExhaustFunc(target)
    }

    public override fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean {
        return isExhaustedFunc(rollable)
    }

    public override fun resetExhaustible(rollable: ExhaustiveRollable<T, R>): Unit {
        return resetExhaustibleFunc(rollable)
    }

    public override fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>): Unit {
        return incrementExhaustibleFunc(rollable)
    }
}

public class ExhaustiveRollableImpl<T, R>(
    public override val rollable: Rollable<T, R>,
    public val hooks: ExhaustiveRollableHooks<T, R>,
    public override val initialRolls: Int,
    public override var rolls: Int = initialRolls
): ExhaustiveRollable<T, R>, ExhaustiveRollableHooks<T, R> by hooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    override fun includeInRoll(onTarget: T): Boolean {
        return rollable.includeInRoll(onTarget) && rolls > 0
    }
}

public fun <T, R> ExhaustiveRollable<T, R>.isExhausted(): Boolean {
    return isExhausted(this)
}

public open class ExhaustiveRollableHooksBuilder<T, R>: AbstractRollableHooksBuilder<T, R, ExhaustiveRollableHooks<T, R>, ExhaustiveRollableHooksBuilder<T, R>>() {

    public var onExhaustFunc: (T) -> Unit = { }
    public var isExhaustedFunc: ExhaustiveRollable<T, R>.() -> Boolean = { rolls <= 0 }
    public var resetExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = { error("resetExhaustible not set for $this") }
    public var incrementExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = { error("incrementExhaustible not set for $this") }

    public open fun onExhaust(block: (T) -> Unit): ExhaustiveRollableHooksBuilder<T, R> {

        onExhaustFunc = block

        return this
    }

    public open fun isExhausted(block: ExhaustiveRollable<T, R>.() -> Boolean): ExhaustiveRollableHooksBuilder<T, R> {

        isExhaustedFunc = block

        return this
    }

    public open fun resetExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveRollableHooksBuilder<T, R> {

        resetExhaustibleFunc = block

        return this
    }

    public open fun incrementExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveRollableHooksBuilder<T, R> {

        incrementExhaustibleFunc = block

        return this
    }

    init {
        construct {
            ExhaustiveRollableHooksImpl(
                baseRollableHooks = buildBaseRollableHooks(),
                onExhaustFunc = onExhaustFunc,
                isExhaustedFunc = isExhaustedFunc,
                resetExhaustibleFunc = resetExhaustibleFunc,
                incrementExhaustibleFunc = incrementExhaustibleFunc
            )
        }
    }
}

public open class ExhaustiveRollableBuilder<T, R>: AbstractRollableBuilder<
        T,
        R,
        ExhaustiveRollable<T, R>,
        ExhaustiveRollableHooks<T, R>,
        ExhaustiveRollableHooksBuilder<T, R>,
        ExhaustiveRollableBuilder<T, R>
>(
    createHookBuilder = { ExhaustiveRollableHooksBuilder<T, R>() }
) {

    private val wrappedRollableBuilder = SingleRollableBuilder<T, R>()

    public open var rolls: Int = 1

    init {
        construct {
            ExhaustiveRollableImpl(
                wrappedRollableBuilder.build(),
                hooks.build(),
                rolls
            )
        }
    }

    public override fun shouldInclude(block: ShouldInclude<T>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.shouldInclude(block)

        return this
    }

    public override fun vetoRoll(block: VetoRoll<T>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.vetoRoll(block)

        return this
    }

    public override fun transform(block: TransformResult<T, R>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.transform(block)

        return this
    }

    public override fun onRollCompleted(block: OnSelect<T, R>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.onRollCompleted(block)

        return this
    }

    public override fun selectResult(block: ExhaustiveRollable<T, R>.(T, ArgMap) -> RollResult<R>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.selectResult(block as (Rollable<T, R>.(T, ArgMap) -> RollResult<R>))

        return this
    }

    public open fun onExhaust(block: OnExhaust<T>): ExhaustiveRollableBuilder<T, R> {

        hooks.onExhaust(block)

        return this
    }

    public open fun rolls(amount: Int): ExhaustiveRollableBuilder<T, R> {

        rolls = amount

        return this
    }
}

public interface ExhaustiveTableHooks<T, R>: TableHooks<T, R>, ExhaustiveRollableHooks<T, R>

internal data class ExhaustiveTableHooksImpl<T, R>(
    val baseTableHooks: TableHooks<T, R>,
    val baseRollableHooks: ExhaustiveRollableHooks<T, R> = ExhaustiveRollableHooksImpl(),
): ExhaustiveTableHooks<T, R>, TableHooks<T, R> by baseTableHooks {

    override fun onExhaust(target: T): Unit {
        baseRollableHooks.onExhaust(target)
    }

    override fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean {
        return baseRollableHooks.isExhausted(rollable)
    }

    override fun resetExhaustible(rollable: ExhaustiveRollable<T, R>): Unit {
        return baseRollableHooks.resetExhaustible(rollable)
    }

    override fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>): Unit {
        return baseRollableHooks.incrementExhaustible(rollable)
    }
}

public open class ExhaustiveTableHooksBuilder<T, R>: AbstractTableHooksBuilder<T, R, ExhaustiveTableHooks<T, R>, ExhaustiveTableHooksBuilder<T, R>>() {

    protected val internalHooks: ExhaustiveRollableHooksBuilder<T, R> = ExhaustiveRollableHooksBuilder<T, R>()

    public open fun onExhaust(block: (T) -> Unit): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.onExhaust(block)

        return this
    }

    public open fun isExhausted(block: ExhaustiveRollable<T, R>.() -> Boolean): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.isExhausted(block)

        return this
    }

    public open fun resetExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.resetExhaustible(block)

        return this
    }

    public open fun incrementExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.incrementExhaustible(block)

        return this
    }

    init {
        construct {
            ExhaustiveTableHooksImpl(
                baseTableHooks = buildBaseTableHooks(),
                baseRollableHooks = internalHooks.build()
            )
        }
    }
}

public interface ExhaustiveTable<T, R>: Table<T, R>, ExhaustiveRollable<T, R> {

    override val tableEntries: Collection<ExhaustiveRollable<T, R>>

    override var rolls: Int
        get() = tableEntries.sumOf(ExhaustiveRollable<T, R>::rolls)
        set(value) { }
}

public open class ExhaustiveTableImpl<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: MutableCollection<ExhaustiveRollable<T, R>>,
    hooks: ExhaustiveTableHooks<T, R>
): ExhaustiveTable<T, R>, ExhaustiveTableHooks<T, R> by hooks {

    public override val rollable: Rollable<T, R> = this

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        if (vetoRoll(target)) {
            return RollResult.Nothing()
        }

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        val selectableEntries = tableEntries.filterNot(ExhaustiveRollable<T, R>::isExhausted)

        val selectedEntry = selectableEntries.random()

        val result = selectedEntry.roll(target, otherArgs)

        if (isExhausted()) {
            onExhaust(target)
        }

        return result
    }

    override val initialRolls: Int
        get() = tableEntries.sumOf { it.initialRolls }

    override var rolls: Int
        get() = tableEntries.sumOf { it.rolls }
        set(value) { }

}

public abstract class ExhaustiveTableBuilder<T, R, Table: ExhaustiveTable<T, R>>: AbstractTableBuilder<
        T,
        R,
        ExhaustiveRollable<T, R>,
        Table,
        ExhaustiveTableHooks<T, R>,
        ExhaustiveTableHooksBuilder<T, R>,
        ExhaustiveTableBuilder<T, R, Table>
>(createHookBuilder = { ExhaustiveTableHooksBuilder<T, R>() }) {

    override val entries: MutableCollection<ExhaustiveRollable<T, R>> = mutableListOf()

    public open infix fun Int.rolls(rollable: Rollable<T, R>): ExhaustiveTableBuilder<T, R, Table> {

        addEntry(ExhaustiveRollableImpl(rollable, ExhaustiveRollableHooksImpl(), this))

        return this@ExhaustiveTableBuilder
    }

    public open infix fun Int.rolls(block: ExhaustiveRollableBuilder<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R, Table> {

        val builder = ExhaustiveRollableBuilder<T, R>()

        builder.apply { rolls(this@rolls) }
        builder.apply(block)

        return addEntry(builder.build())
    }

    public open infix fun Int.rolls(result: R): ExhaustiveTableBuilder<T, R, Table> {
        return rolls(Single(result))
    }

    public open fun onExhaust(block: OnExhaust<T>): ExhaustiveTableBuilder<T, R, Table> {

        hooks.onExhaust(block)

        return this
    }
}

public open class UniformExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R, ExhaustiveTableImpl<T, R>>() {

    init {
        construct {
            ExhaustiveTableImpl<T, R>(
                tableIdentifier = tableIdentifier,
                tableEntries = entries,
                hooks = hooks.build()
            )
        }
    }

    override val entries: MutableCollection<ExhaustiveRollable<T, R>> = mutableListOf()
}

public interface WeightedExhaustiveTable<T, R>: WeightedTable<T, R>, ExhaustiveTable<T, R> {

    public override val tableEntries: List<ExhaustiveRollable<T, R>>
}

public open class WeightedExhaustiveTableImpl<T, R>(
    tableIdentifier: String,
    public override val tableEntries: List<ExhaustiveRollable<T, R>>,
    internal val hooks: ExhaustiveTableHooks<T, R>
): WeightedExhaustiveTable<T, R>, WeightedTableImpl<T, R>(
    tableIdentifier, tableEntries, hooks
) {

    override val initialRolls: Int = 1

    override val rollable: Rollable<T, R> = this

    override fun onExhaust(target: T) {
        return hooks.onExhaust(target)
    }

    override fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean {
        return hooks.isExhausted(rollable)
    }

    override fun resetExhaustible(rollable: ExhaustiveRollable<T, R>) {
        return hooks.resetExhaustible(rollable)
    }

    override fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>) {
        return hooks.incrementExhaustible(rollable)
    }

    override fun includeInRoll(onTarget: T): Boolean {
        return isExhausted()
    }
}

public open class WeightedExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R, WeightedExhaustiveTable<T, R>>() {

    init {
        construct {
            WeightedExhaustiveTableImpl<T, R>(
                tableIdentifier = tableIdentifier,
                entries.toList(),
                hooks.build()
            )
        }
    }

}

public fun <T, R> uniformExhaustiveTable(
    tableName: String = "Unnamed Uniform Exhaustive Table",
    block: UniformExhaustiveTableBuilder<T, R>.() -> Unit
): ExhaustiveTable<T, R> {

    val builder = UniformExhaustiveTableBuilder<T, R>()

    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}

public fun <T, R> weightedExhaustiveTable(
    tableName: String = "Unnamed Weighted Exhaustive Table",
    block: WeightedExhaustiveTableBuilder<T, R>.() -> Unit
): WeightedTable<T, R> {

    val builder = WeightedExhaustiveTableBuilder<T, R>()

    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}