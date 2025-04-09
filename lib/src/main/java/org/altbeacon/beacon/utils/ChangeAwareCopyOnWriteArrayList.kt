package org.altbeacon.beacon.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.ArrayList
import java.util.function.Predicate

class ChangeAwareCopyOnWriteArrayList<E>: ArrayList<E>() {
    var notifier: ChangeAwareCopyOnWriteArrayListNotifier? = null

    override fun add(element: E): Boolean {
        val result = super.add(element)
        notifier?.onChange()
        return result
    }

    override fun remove(element: E): Boolean {
        val result = super.remove(element)
        notifier?.onChange()
        return result
    }

    override fun clear() {
        super.clear()
        notifier?.onChange()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val result = super.addAll(elements)
        notifier?.onChange()
        return result
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val result = super.removeAll(elements)
        notifier?.onChange()
        return result
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun removeIf(filter: Predicate<in E>): Boolean {
        val result = super.removeIf(filter)
        notifier?.onChange()
        return result
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
        notifier?.onChange()
    }

    override fun set(index: Int, element: E): E {
        val result = super.set(index, element)
        notifier?.onChange()
        return result
    }
}

interface ChangeAwareCopyOnWriteArrayListNotifier {
    fun onChange()
}