package dtx.impl.conditional

import dtx.core.AbstractRollableBuilder
import dtx.core.ArgMap
import dtx.core.NothingRollable
import dtx.core.ResultSelector
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleByFun

public interface ConditionalRollable<T, R>: Rollable<T, R>, ConditionalRollableHooks<T, R> {

    public val conditions: Set<Pair<RollCondition<T>, Rollable<T, R>>>
    public val fallbackRollable: Rollable<T, R>

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        val firstMatchedCondition = conditions.firstOrNull { (condition, result) ->
            condition(target, otherArgs)
        }

        if (firstMatchedCondition != null) {
            onConditionMatched(target, otherArgs, firstMatchedCondition.first)
            return firstMatchedCondition.second.roll(target, otherArgs)
        }

        onNoConditionMatched(target, otherArgs)
        return fallbackRollable.roll(target, otherArgs)
    }
}

public open class ConditionalRollableImpl<T, R>(
    override val conditions: Set<Pair<RollCondition<T>, Rollable<T, R>>>,
    override val fallbackRollable: Rollable<T, R>,
    conditionalHooks: ConditionalRollableHooks<T, R> = ConditionalRollableHooks.Default(),
): ConditionalRollable<T, R>, ConditionalRollableHooks<T, R> by conditionalHooks

public open class ConditionalRollableBuilder<T, R>:
    AbstractRollableBuilder<
            T,
            R,
            ConditionalRollable<T, R>,
            ConditionalRollableHooks<T, R>,
            ConditionalRollableHooksBuilder<T, R>,
            ConditionalRollableBuilder<T, R>
>( { ConditionalRollableHooksBuilder() } ) {

    public val rollableConditions: MutableSet<Pair<RollCondition<T>, Rollable<T, R>>> = mutableSetOf()

    public var fallbackRollable: Rollable<T, R> = NothingRollable()

    public open fun condition(condition: RollCondition<T>, result: Rollable<T, R>): ConditionalRollableBuilder<T, R> {
        rollableConditions.add(condition to result)
        return this
    }

    public open fun condition(result: Rollable<T, R>, condition: RollCondition<T>): ConditionalRollableBuilder<T, R> {
        return condition(condition, result)
    }

    public open fun fallback(result: Rollable<T, R>): ConditionalRollableBuilder<T, R> {
        fallbackRollable = result
        return this
    }

    public open fun condition(condition: RollCondition<T>, result: R): ConditionalRollableBuilder<T, R> {
        rollableConditions.add(condition to Single(result))
        return this
    }

    public open fun condition(result: R, condition: RollCondition<T>): ConditionalRollableBuilder<T, R> {
        return condition(condition, result)
    }

    public open fun fallback(result: R): ConditionalRollableBuilder<T, R> {
        fallbackRollable = Single(result)
        return this
    }

    public open fun fallback(selector: ResultSelector<T, R>): ConditionalRollableBuilder<T, R> {
        fallbackRollable = SingleByFun(selector)
        return this
    }


    public open fun onConditionMatched(block: (T, ArgMap, RollCondition<T>) -> Unit): ConditionalRollableBuilder<T, R> {
        hooks.onConditionMatched(block)
        return this
    }

    public open fun onNoConditionMatched(block: (T, ArgMap) -> Unit): ConditionalRollableBuilder<T, R> {
        hooks.onNoConditionMatched(block)
        return this
    }

    init {
        construct {
            ConditionalRollableImpl<T, R>(
                conditions = rollableConditions,
                fallbackRollable = fallbackRollable,
                conditionalHooks = hooks.build()
            )
        }
    }
}

public data object conditionalRollable {
    public inline operator fun <T, R> invoke(block: ConditionalRollableBuilder<T, R>.() -> Unit): ConditionalRollable<T, R> {
        val builder = ConditionalRollableBuilder<T, R>()
        builder.apply(block)
        return builder.build()
    }

    public inline fun <T, R> simple(block: SimpleConditionalRollableBuilder<T, R>.() -> Unit): ConditionalRollable<T, R> {
        val builder = SimpleConditionalRollableBuilder<T, R>()
        builder.apply(block)
        val rollable = builder.build()
        return rollable
    }
}

public open class SimpleConditionalRollable<T, R>(
    private val condition: RollCondition<T>,
    private val conditionRollable: Rollable<T, R>,
    fallbackRollable: Rollable<T, R> = conditionRollable,
    hooks: ConditionalRollableHooks<T, R> = ConditionalRollableHooks.Default()
): ConditionalRollableImpl<T, R>(
    conditions = setOf(condition to conditionRollable),
    fallbackRollable = fallbackRollable,
    conditionalHooks = hooks
)

public open class SimpleConditionalRollableBuilder<T, R>: ConditionalRollableBuilder<T, R>() {
    public var theCondition: RollCondition<T>? = null
    public var theRollable: Rollable<T, R>? = null

    public open fun condition(condition: RollCondition<T>): SimpleConditionalRollableBuilder<T, R> {
        theCondition = condition
        return this
    }

    public open fun conditionRollable(result: Rollable<T, R>): SimpleConditionalRollableBuilder<T, R> {
        theRollable = result
        return this
    }

    public open fun conditionRollable(result: R): SimpleConditionalRollableBuilder<T, R> {
        return conditionRollable(Single(result))
    }

    public open fun conditionRollable(selector: ResultSelector<T, R>): SimpleConditionalRollableBuilder<T, R> {
        return conditionRollable(SingleByFun(selector))
    }

    public override fun condition(condition: RollCondition<T>, result: Rollable<T, R>): SimpleConditionalRollableBuilder<T, R> {
        condition(condition)
        conditionRollable(result)
        return this
    }

    public infix fun withRollable(result: Rollable<T, R>): SimpleConditionalRollableBuilder<T, R> {
        return conditionRollable(result)
    }

    public infix fun withRollable(result: R): SimpleConditionalRollableBuilder<T, R> {
        return conditionRollable(result)
    }

    init {
        construct {

            val firstRcPair = rollableConditions.firstOrNull()

            val conditionToUse = theCondition
                ?: firstRcPair?.first
                ?: error("Condition must be set for SimpleConditionalRollable")

            val resultToUse = theRollable
                ?: firstRcPair?.second
                ?: error("Result must be set for SimpleConditionalRollable")

            SimpleConditionalRollable<T, R>(
                condition = conditionToUse,
                conditionRollable = resultToUse,
                fallbackRollable = fallbackRollable,
                hooks = hooks.build()
            )
        }
    }
}

public inline fun <T, R> simpleConditionalRollable(block: SimpleConditionalRollableBuilder<T, R>.() -> Unit): ConditionalRollable<T, R> {
    val builder = SimpleConditionalRollableBuilder<T, R>()
    builder.apply(block)
    return builder.build()
}
