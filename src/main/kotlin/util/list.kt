package util

/**
 * Checks if a list is sorted according to a selector function.
 *
 * This function iterates through the list and checks if each element is greater than or equal to
 * the previous element, according to the values returned by the selector function.
 *
 * @param T The type of elements in the list.
 * @param R The type of the comparable values returned by the selector function.
 * @param bySelector A function that returns a comparable value for each element in the list.
 * @return True if the list is sorted in ascending order according to the selector function, false otherwise.
 */
internal fun <T, R: Comparable<R>> List<T>.isSortedBy(bySelector: (T) -> R): Boolean {

    for (i in 1 until size) {

        val prev = bySelector(get(i - 1))
        val cur = bySelector(get(i))

        if (prev > cur) {
            return false
        }
    }

    return true
}
