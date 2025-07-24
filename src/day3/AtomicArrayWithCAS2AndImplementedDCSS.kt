@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2AndImplementedDCSS.Status.*
import java.util.concurrent.atomic.*
import kotlin.math.PI

// This implementation never stores `null` values.
class AtomicArrayWithCAS2AndImplementedDCSS<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    private fun AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor.getExpected(index: Int): E {
        return (if (index == this.index1) this.expected1 else this.expected2) as E
    }

    private fun AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor.getUpdate(index: Int): E {
        return (if (index == this.index1) this.update1 else this.update2) as E
    }


    fun get(index: Int): E {
        // TODO: Copy the implementation from `AtomicArrayWithCAS2Simplified`
        val value = array[index]
        return when {
            value is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor -> {
                val expected = value.getExpected(index)
                when (value.status.get()) {
                    FAILED -> {
                        array.compareAndSet(index, value, expected)
                        return expected
                    }
                    SUCCESS -> {
                        val update = value.getUpdate(index)
                        array.compareAndSet(index, value, update)
                        return update
                    }
                    UNDECIDED -> {
                        return expected
                    }
                }
            }
            else -> value as E
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = if (index1 > index2) {
            CAS2Descriptor(
                index1 = index2, expected1 = expected2, update1 = update2,
                index2 = index1, expected2 = expected1, update2 = update1
            )
        } else {
            CAS2Descriptor(
                index1 = index1, expected1 = expected1, update1 = update1,
                index2 = index2, expected2 = expected2, update2 = update2
            )
        }
        descriptor.apply()
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int,
        val expected1: E,
        val update1: E,
        val index2: Int,
        val expected2: E,
        val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            val success = installDescriptor()
            applyLogically(success)
            applyPhysically()
        }

        private fun installCell(index: Int, expected: E): Boolean {
            while (true) {
                when (status.get()) {
                    SUCCESS -> return true
                    FAILED -> return false
                    UNDECIDED -> {}
                }

                val value = array[index]
                if (value == expected) {
                    if (dcss(index, expected, this, status, UNDECIDED)) {
                        return true
                    }
                    continue
                }

                if (value is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor) {
                    if (value === this) {
                        // someone helped us
                        return true
                    }

                    value.apply()
                    continue
                }

                return false
            }
        }

        private fun installDescriptor(): Boolean {
            // TODO: Install this descriptor to the cells,
            // TODO: returning `true` on success, and `false`
            // TODO: if one of the cells contained an unexpected value.
            if (!installCell(index1, expected1)) {
                return false
            }

            if (!installCell(index2, expected2)) {
                return false
            }
            return true
        }

        private fun applyLogically(success: Boolean) {
            // TODO: Apply this CAS2 operation logically
            // TODO: by updating the descriptor status.
            if (success) {
                status.compareAndSet(UNDECIDED, SUCCESS)
            } else {
                status.compareAndSet(UNDECIDED, FAILED)
            }
        }

        private fun applyPhysically() {
            // TODO: Apply this operation physically
            // TODO: by updating the cells to either
            // TODO: update values (on success)
            // TODO: or back to expected values (on failure).
            val status = status.get()
            if (status == SUCCESS) {
                array.compareAndSet(index1, this, update1)
                array.compareAndSet(index2, this, update2)
            } else {
                array.compareAndSet(index1, this, expected1)
                array.compareAndSet(index2, this, expected2)
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }

    // TODO: Please use this DCSS implementation to ensure that
    // TODO: the status is `UNDECIDED` when installing the descriptor.
    fun dcss(
        index: Int,
        expectedCellState: Any?,
        updateCellState: Any?,
        statusReference: AtomicReference<*>,
        expectedStatus: Any?
    ): Boolean =
        if (array[index] == expectedCellState && statusReference.get() == expectedStatus) {
            array[index] = updateCellState
            true
        } else {
            false
        }
}