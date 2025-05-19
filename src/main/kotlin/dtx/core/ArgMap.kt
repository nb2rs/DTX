package dtx.core

/**
 * Represents a key for an [ArgMap] argument with a default value.
 *
 * @param T The type of the value associated with this key.
 *
 * @property str The string identifier for this key.
 *
 * @property default The default value to use if no value is provided for this key.
 */
public data class ArgKey<T>(val str: String, val default: T)

/**
 * Represents a key-value pair for an argument.
 *
 * @param T The type of the value.
 *
 * @property key The key for this argument.
 *
 * @property value The value for this argument.
 */
public data class ArgPair<T>(val key: ArgKey<T>, val value: T)

/**
 * Creates an ArgPair from an ArgKey and a value.
 *
 * @param other The value to associate with this key.
 *
 * @return [ArgPair]
 */
public infix fun <T> ArgKey<T>.with(other: T) = ArgPair(this, other)

/**
 * An extremely basic map-like utility class for optional argument-passing functionality.
 *
 * [ArgMap] provides a type-safe way to pass arguments to functions and methods.
 * Each argument is identified by an ArgKey, which includes a default value that is returned if the key is not
 *  present in the map.
 */
public class ArgMap private constructor(private val locked: Boolean = false) {

    /**
     * Creates a new ArgMap with the specified arguments and optional base map.
     *
     * @param args The initial arguments to include in the map.
     *
     * @param otherMap Another ArgMap whose entries will be included in this map.
     */
    public constructor(
        vararg args: ArgPair<*>,
        otherMap: ArgMap = ArgMap.Empty
    ): this(locked = false) {

        args.forEach { argPair ->
            setWildcard(argPair)
        }

        otherMap.map.forEach { pair ->
            setWildcard(pair.key, pair.value)
        }
    }

    /**
     * The internal map storing the arguments.
     */
    private val map = mutableMapOf<ArgKey<*>, Any?>()

    /**
     * Sets the value for the specified key.
     *
     * @param key The key for the argument.
     *
     * @param value The value to associate with the key.
     *
     * @throws IllegalStateException if the map is locked.
     */
    public operator fun <T> set(key: ArgKey<T>, value: T) {

        if (locked) {
            throw IllegalStateException("ArgMap is locked")
        }

        map[key] = value
    }

    /**
     * Sets the value in a technically type-unsafe for the specified key using a wildcard ArgPair.
     *
     * @param argPair The key-value pair to add to the map.
     */
    private fun setWildcard(argPair: ArgPair<*>) {
        map[argPair.key] = argPair.value
    }

    /**
     * Sets the value for the specified key.
     *
     * @param key The key for the argument.
     *
     * @param value The value to associate with the key.
     */
    private fun setWildcard(key: ArgKey<*>, value: Any?) {
        map[key] = value
    }

    /**
     * Gets the value for the specified key, or the key's default value if not present.
     *
     * @param key The key for the argument.
     * @return [T]
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(key: ArgKey<T>): T {
        return (map[key] ?: key.default) as T
    }

    /**
     * Companion object containing utility values.
     */
    public companion object {
        /**
         * An empty, locked ArgMap that cannot be modified.
         */
        public val Empty: ArgMap = ArgMap(locked = true)
    }
}
