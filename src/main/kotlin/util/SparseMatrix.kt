package util

import kotlin.collections.iterator


/**
 * A CSR sparse matrix of elements of type [T].
 *
 * @param T The type of elements in the matrix. Must be a non-nullable type.
 * @property rows The number of rows in the matrix.
 * @property columns The number of columns in the matrix.
 * @property defaultValue The default value for elements in the matrix.
 * @property returnDefaultOnOob Whether to return the default value for out-of-bounds accesses (true) or throw an error (false).
 */
public class SparseMatrix<T : Any>(
    public val rows: Int,
    public val columns: Int,
    public val defaultValue: T,
    public val returnDefaultOnOob: Boolean = false,
) {
    /**
     * The list of non-default values in the matrix.
     */
    private val nonDefaultValues: MutableList<T> = mutableListOf()

    /**
     * The column indices of the non-default values.
     */
    private val columnIndices: MutableList<Int> = mutableListOf()

    /**
     * The row pointers that indicate where each row starts in the columnIndices and nonDefaultValues lists.
     */
    private val rowPointers: MutableList<Int> = MutableList(rows + 1) { 0 }

    /**
     * The number of non-default values in the matrix.
     */
    private var nonDefaultCount = 0

    /**
     * Initializes the matrix and validates the dimensions.
     *
     * @throws IllegalArgumentException if the matrix dimensions are less than 1x1.
     */
    init {
        require(rows > 0 && columns > 0) {
            "Matrix dimensions must be at least 1x1 (passed ${rows}x$columns)."
        }
    }

    /**
     * Checks if the given row and column indices are out of bounds.
     *
     * @param row The row index to check.
     * @param col The column index to check.
     * @return True if the indices are out of bounds, false otherwise.
     */
    private fun boundCheck(row: Int, col: Int) = row < 0 || row >= rows || col < 0 || col >= columns

    /**
     * Finds the index of a column in a specific row.
     *
     * @param row The row to search in.
     * @param col The column to find.
     * @return The index of the column in the columnIndices list, or a negative value if not found.
     */
    private fun colIndexOf(row: Int, col: Int): Int {

        val rowStart = rowPointers[row]
        val rowEnd = rowPointers[row + 1]

        return columnIndices.binarySearch(col, fromIndex = rowStart, toIndex = rowEnd)
    }

    /**
     * Gets the value at the specified row and column.
     *
     * @param row The row index.
     * @param col The column index.
     * @return The value at the specified position, or the default value if the position contains the default value.
     * @throws Error if the indices are out of bounds and returnDefaultOnOob is false.
     */
    public operator fun get(row: Int, col: Int): T {

        val isOob = boundCheck(row, col)

        if (isOob) {

            if (returnDefaultOnOob) {
                return defaultValue
            }

            throw error("Out of Bounds for [$row:$col] (matrix size [$rows:$columns]")
        }

        val index = colIndexOf(row, col)

        return if (index >= 0) {
            nonDefaultValues[index]
        } else {
            defaultValue
        }
    }

    /**
     * Sets the value at the specified row and column.
     *
     * If the new value is the default value, the entry is removed from the sparse representation.
     * If the new value is not the default value, the entry is added to the sparse representation.
     *
     * @param row The row index.
     * @param col The column index.
     * @param value The value to set.
     * @throws Error if the indices are out of bounds and returnDefaultOnOob is false.
     */
    public operator fun set(row: Int, col: Int, value: T) {

        val isOob = boundCheck(row, col)

        if (isOob) {

            if (returnDefaultOnOob) {
                return
            }

            throw error("Out of Bounds for [$row:$col] (matrix size [$rows:$columns]")
        }

        val index = colIndexOf(row, col)
        val isDefaultValue = (value == defaultValue)

        if (index >= 0) {

            if (isDefaultValue) {
                removeElementAt(index, row)
            } else {
                nonDefaultValues[index] = value
            }

        } else {

            if (!isDefaultValue) {

                val insertionPoint = -(index + 1)
                insertAt(insertionPoint, row, col, value)
            }
        }
    }

    /**
     * Inserts a non-default value at the specified position in the sparse representation.
     *
     * @param index The index at which to insert the value.
     * @param row The row index of the value.
     * @param col The column index of the value.
     * @param value The value to insert.
     */
    private fun insertAt(index: Int, row: Int, col: Int, value: T) {

        if (value == defaultValue) {
            return
        }

        nonDefaultValues.add(index, value)
        columnIndices.add(index, col)
        nonDefaultCount++

        for (r in (row + 1)..rows) {
            rowPointers[r]++
        }
    }

    /**
     * Removes a non-default value at the specified position from the sparse representation.
     *
     * @param index The index of the value to remove.
     * @param row The row index of the value.
     */
    private fun removeElementAt(index: Int, row: Int) {

        nonDefaultValues.removeAt(index)
        columnIndices.removeAt(index)
        nonDefaultCount--

        for (r in (row + 1)..rows) {
            rowPointers[r]--
        }
    }

    public fun nonDefaults(): Iterator<Triple<Int, Int, T>> {

        val elements = mutableListOf<Triple<Int, Int, T>>()
        var currentRow = 0

        for (i in 0 until nonDefaultCount) {

            while (rowPointers[currentRow + 1] <= i) {
                currentRow++
            }

            elements.add(Triple(currentRow, columnIndices[i], nonDefaultValues[i]))
        }

        return elements.iterator()
    }


    public fun nonDefaultString(): String = buildString {
        append("nondefaults (row, col, value):\n")
        for(triple in nonDefaults()) {
            append("  (${triple.first}, ${triple.second}) -> ${triple.third}\n")
        }
    }


    override fun toString(): String = buildString {

        append("Matrix: \n")
        for (r in 0 until rows) {

            append("[ ")
            for (c in 0 until columns) {

                append(get(r, c).toString().padEnd(5))
                if (c < columns - 1) {
                    append(", ")
                }
            }

            append(" ]\n")
        }

        return toString()
    }

    public fun copy(defaultValue: T = this.defaultValue): SparseMatrix<T> {
        val matrix = SparseMatrix<T>(rows, columns, defaultValue)
        nonDefaults().forEach { (row, column, nonDefaultValue) ->
            matrix[row, column] = nonDefaultValue
        }
        return matrix
    }
}
