package dtx.core

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents a result that can hold either no data, a single result, or a list of results.
 *
 * @param R The type of the result(s) contained within the instance.
 */
public sealed interface RollResult<R> {

    public data object Nothing: RollResult<Nothing>

    public data class Single<R>(val result: R): RollResult<R>

    public data class ListOf<R>(val results: List<R>): RollResult<R>

    public companion object {

        public fun <R> Nothing(): RollResult<R> = Nothing as RollResult<R>
    }
}

/**
 * Flattens a list of RollResult instances into a single RollResult.ListOf
 *
 * when input:
 * - Empty list = Nothing
 * - Single item list = that item
 * - List with only Single items = ListOf with all results
 * - List with only ListOf items = ListOf with all results flattened
 * - Mixed list = Combined result of all items
 *
 * @return [RollResult]
 */
public fun <R> List<RollResult<R>>.flattenToList(): RollResult.ListOf<R> {

    if (isEmpty()) {
        return RollResult.ListOf<R>(emptyList())
    }

    if (size == 1) {
        val first = first().flatten()
        return when (first) {
            is RollResult.Nothing -> RollResult.ListOf<R>(emptyList())
            is RollResult.Single<R> -> RollResult.ListOf<R>(listOf(first.result))
            is RollResult.ListOf<R> -> RollResult.ListOf<R>(first.results)
        }
    }

    val filtered = filterNot { it is RollResult.Nothing }
    val singles = filtered.filterIsInstance<RollResult.Single<R>>().map { it.result }
    val singlesResult = RollResult.ListOf(singles)

    if (singles.size == filtered.size) {
        return singlesResult
    }

    val lists = (filtered.filterIsInstance<RollResult.ListOf<R>>()).map { it.results }
    val listsResult = RollResult.ListOf(lists.flatten())

    if (lists.size == filtered.size) {
        return listsResult
    }

    return RollResult.ListOf(listsResult.results + singles)

}

/**
 * Simplifies the traversal of all possibly-contained [RollResult].
 *
 * This function simplifies the structure of a RollResult:
 * - Nothing remains as Nothing
 * - Single remains as Single
 * - ListOf with no items becomes Nothing
 * - ListOf with one item becomes Single with that item
 * - ListOf with multiple items remains as ListOf
 *
 * @return [RollResult]
 */
public fun <R> RollResult<R>.flatten(): RollResult<R> = when (this) {

    is RollResult.Nothing -> this
    is RollResult.Single<R> -> this

    is RollResult.ListOf<R> -> when (results.size) {
        0 -> RollResult.Nothing()
        1 -> RollResult.Single(results.first())
        else -> RollResult.ListOf(results)
    }
}
