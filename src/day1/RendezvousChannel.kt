package day1

import java.util.concurrent.atomic.*
import kotlin.coroutines.*

/**
 * Rendezvous channel is the key synchronization primitive behind coroutines,
 * it is also known as a synchronous queue.
 *
 * At its core, a rendezvous channel is a blocking bounded queue of zero capacity.
 * It supports `send(e)` and `receive()` requests:
 * send(e) checks whether the queue contains any receivers and either removes the first one
 * (i.e. performs a “rendezvous” with it) or adds itself to the queue as a waiting sender and suspends.
 * The `receive()` operation works symmetrically.
 *
 * This implementation never stores `null`-s for simplicity.
 *
 * (You may also follow `SequentialChannelInt` in tests for the sequential semantics).
 */
class RendezvousChannel<E : Any> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null, null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    suspend fun send(element: E) {
        while (true) {
            // TODO: feel free to change this code if needed.
            // Is this queue empty or contain other senders?
            val curHead = head.get()
            val curTail = tail.get()
            if (isEmptyOrContainsSenders()) {
                val success = suspendCoroutine<Boolean> { continuation ->
                    val node = Node(element, continuation as Continuation<Any?>)
                    if (!tryAddNode(curTail, node)) {
                        // Fail and retry.
                        continuation.resume(false)
                    }
                }
                // Finish on success and retry on failure.
                if (success) return
            } else {
                // The queue contains receivers, try to extract the first one.
                val firstReceiver = tryExtractNode(curHead) ?: continue
                firstReceiver.continuation!!.resume(element)
                return
            }
        }
    }

    suspend fun receive(): E {
        while (true) {
            // TODO: feel free to change this code if needed.
            // Is this queue empty or contain other receivers?
            val curHead = head.get()
            val curTail = tail.get()
            if (isEmptyOrContainsReceivers()) {
                val element = suspendCoroutine<E?> { continuation ->
                    val node = Node(RECEIVER, continuation as Continuation<Any?>)
                    if (!tryAddNode(curTail, node)) {
                        // Fail and retry.
                        continuation.resume(null)
                    }
                }
                // Should we retry?
                if (element == null) continue
                // Return the element
                return element
            } else {
                // The queue contains senders, try to extract the first one.
                val firstSender = tryExtractNode(curHead) ?: continue
                firstSender.continuation!!.resume(true)
                return firstSender.element as E
            }
        }
    }

    private fun isEmptyOrContainsReceivers(): Boolean {
        // TODO(" Implement me!")
        // For receivers, Node.element === RECEIVER
        val next = head.get().next.get()
        return next == null || next.element === RECEIVER
    }

    private fun isEmptyOrContainsSenders(): Boolean {
        //TODO(" Implement me!")
        // For senders, Node.element !== RECEIVER
        return head.get().next.get()?.element !== RECEIVER
    }

    private fun tryAddNode(curTail: Node, newNode: Node): Boolean {
        // TODO("Implement me!")
        // TODO: Return `false` if the attempt has failed.
        val next = curTail.next.get()
        if (next != null) {
            tail.compareAndSet(curTail, next)
            return false
        }
        val curHead = head.get()
        if ((curTail != curHead) && (newNode.element === RECEIVER) != (curTail.element === RECEIVER)) {
            return false
        }
        if (!curTail.next.compareAndSet(null, newNode)) {
            return false
        }
        tail.compareAndSet(curTail, newNode)
        return true
    }

    private fun tryExtractNode(curHead: Node): Node? {
        // TODO("Implement me!")
        // TODO: Return a node with the extracted continuation & element.
        val next = curHead.next.get() ?: return null
        if (head.compareAndSet(curHead, next)) {
            val result = Node(next.element, next.continuation)
            next.element = null
            next.continuation = null
            return result
        }
        return null
    }

    class Node(
        // Sending element in case of suspended `send()` or
        // RECEIVER in case of suspended `receive()`.
        var element: Any?,
        // Suspended `send` of `receive` request.
        var continuation: Continuation<Any?>?
    ) {
        val next = AtomicReference<Node?>(null)
    }
}

private val RECEIVER = "Receiver"