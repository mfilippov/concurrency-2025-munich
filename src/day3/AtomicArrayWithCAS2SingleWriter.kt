@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2SingleWriter.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2SingleWriter<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        // TODO: the cell can store CAS2Descriptor
        val value = array[index]
        return when {
            value is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor -> when (value.status.get()) {
                SUCCESS -> (if (index == value.index1) value.update1 else value.update2) as E
                else -> (if (index == value.index1) value.expected1 else value.expected2) as E
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
        val index1: Int, val expected1: E, val update1: E,
        val index2: Int, val expected2: E, val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            // TODO: Install the descriptor, update the status, and update the cells;
            // TODO: create functions for each of these three phases.
            // TODO: In this task, only one thread can call cas2(..),
            // TODO: so cas2(..) calls cannot be executed concurrently.
            fun lockCell(index: Int, expected: E): Boolean {
                while (true) {
                    return when (array[index]) {
                        is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor -> false
                        expected if array.compareAndSet(index, expected, this) -> true
                        expected -> continue
                        else -> false
                    }
                }
            }

            if (!lockCell(index1, expected1)) {
                status.compareAndSet(UNDECIDED, FAILED)
                return
            }
            if (!lockCell(index2, expected2)) {
                array.compareAndSet(index1, this, expected1)
                status.compareAndSet(UNDECIDED, FAILED)
                return
            }

            status.set(SUCCESS)

            array.set(index1, update1)
            array.set(index2, update2)

            status.compareAndSet(UNDECIDED, SUCCESS)
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}