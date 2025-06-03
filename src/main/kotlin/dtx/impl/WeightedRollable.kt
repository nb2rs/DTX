package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollResult

public interface WeightedRollable<T, R>: Rollable<T, R> {

    public val weight: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return weight
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    private data object Empty: WeightedRollable<Any?, Any?> {

        override fun shouldRoll(target: Any?): Boolean {
            return false
        }

        override val weight: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()
    }

    public companion object {

        public fun <T, R> Empty(): WeightedRollable<T, R> {
            return Empty as WeightedRollable<T, R>
        }
    }
}

public data class WeightedRollableImpl<T, R>(
    override val weight: Double,
    override val rollable: Rollable<T, R>,
): WeightedRollable<T, R> {
    override fun shouldRoll(target: T): Boolean {
        return rollable.shouldRoll(target)
    }
}
