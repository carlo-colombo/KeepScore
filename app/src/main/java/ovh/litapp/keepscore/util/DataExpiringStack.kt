package ovh.litapp.keepscore.util

import java.util.LinkedList

/**
 * Stack that pushes items off the end (i.e. the bottom of the stack) if there are more items than the specified
 * max capacity.
 *
 * @author nolan
 */
class DataExpiringStack<E>(private val capacity: Int) {

    private val list: LinkedList<E> = LinkedList()

    init {
        if (capacity <= 0) {
            throw IllegalArgumentException("capacity must be greater than 0: $capacity")
        }
    }

    fun pop(obj: E) {
        if (list.size == capacity) {
            // expire from the beginning of the list
            list.removeFirst()
        }
        list.addLast(obj)
    }

    fun poll(): E? {
        if (isEmpty()) {
            return null
        }
        return list.removeLast()
    }

    fun size(): Int = list.size

    fun peek(): E? {
        if (isEmpty()) {
            return null
        }
        return list.last
    }

    fun isEmpty(): Boolean = list.isEmpty()

    fun clear() {
        list.clear()
    }

    override fun toString(): String = "DataExpiringStack [capacity=$capacity, list=$list]"
}
