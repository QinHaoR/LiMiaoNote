package com.limiao.notes.ui

import java.util.Calendar

/** 当前月 "YYYY-MM" */
internal fun currentYm(): String {
    val c = Calendar.getInstance()
    val m = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    return "${c.get(Calendar.YEAR)}-$m"
}

internal fun monthLabel(ym: String): String {
    val parts = ym.split("-")
    return if (parts.size == 2) "${parts[0]}年${parts[1].toInt()}月" else ym
}

internal fun shiftMonth(ym: String, delta: Int): String {
    val (y, m) = ym.split("-").map { it.toInt() }
    val c = Calendar.getInstance()
    c.set(y, m - 1 + delta, 1)
    val mm = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    return "${c.get(Calendar.YEAR)}-$mm"
}
