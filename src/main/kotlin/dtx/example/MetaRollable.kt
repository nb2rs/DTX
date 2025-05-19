package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.table.Table


/**
 * Interface for filtering and modifying entries in a MetaWeightedTable.
 *
 * A MetaEntryFilter determines which entries should be modified when a specific
 * entry is selected, and how those entries should be modified.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public interface MetaEntryFilter<T, R> {
    /**
     * Determines whether an entry with the given identifier should be modified.
     *
     * @param modifier The identifier of the entry to check.
     * @return [Boolean]
     */
    public fun filterEntry(modifier: String): Boolean = false

    /**
     * Modifies the given entry.
     *
     * @param entry The entry to modify.
     */
    public fun modifyEntry(entry: MetaRollable<T, R>)
}

/**
 * Implementation of MetaEntryFilter that uses functions for filtering and modification.
 *
 * @property filterFunc The function to use for filtering entries.
 * @property modifyFunc The function to use for modifying entries.
 */
internal class MetaEntryFilterImpl<T, R>(
    val filterFunc: (String) -> Boolean,
    val modifyFunc: (MetaRollable<T, R>) -> Unit
): MetaEntryFilter<T, R> {
    override fun filterEntry(modifier: String): Boolean = filterFunc(modifier)
    override fun modifyEntry(entry: MetaRollable<T, R>) = modifyFunc(entry)
}


/**
 * Builder class for creating MetaEntryFilter instances.
 *
 * This builder provides a fluent API for configuring and creating MetaEntryFilter instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class MetaEntryFilterBuilder<T, R> {

    /**
     * The function to use for filtering entries.
     */
    public var filterFunc: (String) -> Boolean = { false }

    /**
     * The function to use for modifying entries.
     */
    public var modifyFunc: (MetaRollable<T, R>) -> Unit = { }

    /**
     * Sets the function to use for filtering entries.
     *
     * @param func The function to use.
     * @return This builder instance for method chaining.
     */
    public fun filter(func: (String) -> Boolean): MetaEntryFilterBuilder<T, R> = apply {
        filterFunc = func
    }

    /**
     * Sets the function to use for modifying entries.
     *
     * @param func The function to use.
     * @return This builder instance for method chaining.
     */
    public fun modify(func: (MetaRollable<T, R>) -> Unit): MetaEntryFilterBuilder<T, R> = apply {
        modifyFunc = func
    }

    /**
     * Builds and returns a [MetaEntryFilter] based on inputs
     *
     * @return [MetaEntryFilter]
     */
    public fun build(): MetaEntryFilter<T, R> {
        return MetaEntryFilterImpl(filterFunc) { modifyFunc(it) }
    }
}

/**
 * A [Rollable] implementation that gives it some sort of meta-knowledge that it and others can access
 */
public interface MetaRollable<T, R>: Rollable<T, R> {

    /**
     * The [Rollable] that is being made meta
     */
    public val rollable: Rollable<T, R>

    /**
     * The identifier of this [MetaRollable] in the parent [MetaTable]
     */
    public val identifier: String

    /**
     * The Filters that decide which other meta filters to modify.
     */
    public val metaEntryFilters: MutableSet<MetaEntryFilter<T, R>>

    /**
     * The [MetaTable] that this [MetaRollable] will be drawing from nad modifying the entries of
     */
    public val parentTable: MetaTable<T, R>

    /**
     * Rolls on [Rollable] and then uses [metaEntryFilters] to filter from [parentTable] and modify them
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val result = rollable.roll(target, otherArgs)

        parentTable.tableEntries.forEach { otherEntry ->
            metaEntryFilters.forEach { metaFilter ->
                if (metaFilter.filterEntry(otherEntry.identifier)) {
                    metaFilter.modifyEntry(otherEntry)
                }
            }
        }

        onSelect(target, result)

        return result
    }
}

public interface MetaTable<T, R>: Table<T, R> {
    override val tableEntries: Collection<MetaRollable<T, R>>
}
