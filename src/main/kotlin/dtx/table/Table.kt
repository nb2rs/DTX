package dtx.table

import dtx.core.Rollable

/**
 * Represents a table that can be rolled on to produce [TableResult].
 * A Table implements [Rollable] and adds functionality for modifying the roll based on a percentage.
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public interface Table<T, R>: Rollable<T, R> {

    /**
     * The collection of entries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     */
    public val tableEntries: Collection<Rollable<T, R>>

    /**
     * Indicates whether roll modifiers should be ignored for this table.
     *
     * If true, the rollModifier method will always return 1.0 regardless of the
     * percentage provided.
     */
    public val ignoreModifier: Boolean

    /**
     * Calculates a roll modifier based on a percentage.
     *
     * If ignoreModifier is true, this method always returns 1.0.
     * Otherwise, it uses the defaultRollModifier function to calculate the modifier.
     *
     * @param percentage The percentage to use for the modifier calculation.
     * @return [Double]
     */
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
