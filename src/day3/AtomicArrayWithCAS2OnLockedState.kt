package day3

import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2OnLockedState<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        // TODO: Cover the case when the cell state is LOCKED.
        while(true) {
            val value = array[index]
            if (value != LOCKED) {
                return value as E
            }
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        // TODO: Make me thread-safe by "locking" the cells
        // TODO: via atomically changing their states to LOCKED.
        if (index1 > index2) {
            return cas2(index2, expected2, update2, index1, expected1, update1)
        }

        fun withLock(index: Int, expected: E, update: E, block: () -> Boolean): Boolean {
            while(true) {
                val value = array[index]
                if (value == LOCKED) continue
                if (value != expected) return false

                if (array.compareAndSet(index, expected, LOCKED)) {
                    val success = block()
                    array.set(index, if (success) update else expected)
                    return success
                }
            }
        }
        return withLock(index1, expected1, update1) {
            withLock(index2, expected2, update2, {true})
        }
    }
}

// TODO: Store me in `a` to indicate that the reference is "locked".
// TODO: Other operations should wait in an active loop until the
// TODO: value changes.
private val LOCKED = "Locked"