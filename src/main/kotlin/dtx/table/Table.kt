package dtx.table

import dtx.core.Rollable


public interface Table<T, R>: Rollable<T, R> {


    public val tableEntries: Collection<Rollable<T, R>>


    public val ignoreModifier: Boolean


    public fun rollModifier(percentage: Double): Double {

        if (ignoreModifier) {
            return 1.0
        }

        return defaultRollModifier(percentage)
    }

    public companion object {

        public fun defaultRollModifier(percentage: Double = 0.0): Double {
            return 1.0 + (percentage / 100.0)
        }
    }
}
