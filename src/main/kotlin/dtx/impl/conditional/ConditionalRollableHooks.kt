package dtx.impl.conditional

import dtx.core.AbstractRollableHooksBuilder
import dtx.core.ArgMap
import dtx.core.RollableHooks

public interface ConditionalRollableHooks<T, R>: RollableHooks<T, R> {

    public fun onConditionMatched(target: T, otherArgs: ArgMap, condition: RollCondition<T>): Unit

    public fun onNoConditionMatched(target: T, otherArgs: ArgMap): Unit

    public companion object {
        public fun <T, R> Default(): ConditionalRollableHooks<T, R> = object: ConditionalRollableHooks<T, R>, RollableHooks<T, R> by RollableHooks.Default() {
            override fun onConditionMatched(target: T, otherArgs: ArgMap, condition: RollCondition<T>): Unit { }
            override fun onNoConditionMatched(target: T, otherArgs: ArgMap): Unit { }
        }
    }
}

public class ConditionalRollableHooksImpl<T, R>(
    public val baseHooks: RollableHooks<T, R> = RollableHooks.Default(),
    public val onConditionMatchedFunc: (target: T, otherArgs: ArgMap, condition: RollCondition<T>) -> Unit =
        ConditionalRollableHooks.Default<T, R>()::onConditionMatched,
    public val onNoConditionMatchedFunc: (target: T, otherArgs: ArgMap) -> Unit =
        ConditionalRollableHooks.Default<T, R>()::onNoConditionMatched
): ConditionalRollableHooks<T, R>, RollableHooks<T, R> by baseHooks {

    override fun onConditionMatched(target: T, otherArgs: ArgMap, condition: RollCondition<T>) {
        return onConditionMatchedFunc(target, otherArgs, condition)
    }

    override fun onNoConditionMatched(target: T, otherArgs: ArgMap) {
        return onNoConditionMatchedFunc(target, otherArgs)
    }
}

public open class ConditionalRollableHooksBuilder<T, R>: AbstractRollableHooksBuilder<
        T,
        R,
        ConditionalRollableHooks<T, R>,
        ConditionalRollableHooksBuilder<T, R>
>() {

    public var onConditionMatchedFunc: (T, ArgMap, RollCondition<T>) -> Unit = { _, _, _ -> }

    public var onNoConditionMatchedFunc: (T, ArgMap) -> Unit = { _, _ -> }

    public fun onConditionMatched(block: (T, ArgMap, RollCondition<T>) -> Unit): ConditionalRollableHooksBuilder<T, R> {
        onConditionMatchedFunc = block
        return this
    }

    public fun onNoConditionMatched(block: (T, ArgMap) -> Unit): ConditionalRollableHooksBuilder<T, R> {
        onNoConditionMatchedFunc = block
        return this
    }

    init {
        construct {
            ConditionalRollableHooksImpl<T, R>(
                baseHooks = buildBaseRollableHooks(),
                onConditionMatchedFunc = onConditionMatchedFunc,
                onNoConditionMatchedFunc = onNoConditionMatchedFunc
            )
        }
    }
}
