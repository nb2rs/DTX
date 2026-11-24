package dtx.impl.conditional

import dtx.core.ArgMap

public fun interface RollCondition<T>: (T, ArgMap) -> Boolean {
    public val rollConditionId: String get() = "UnnamedCondition[$this]"
    public fun evaluate(target: T, otherArgs: ArgMap): Boolean = invoke(target, otherArgs)

    public companion object {
        public inline operator fun <T> invoke(id: String, crossinline condition: (T, ArgMap) -> Boolean): RollCondition<T> =
            object: RollCondition<T> {
                override val rollConditionId: String = id
                override fun invoke(target: T, otherArgs: ArgMap): Boolean = condition(target, otherArgs)
            }
    }
}
