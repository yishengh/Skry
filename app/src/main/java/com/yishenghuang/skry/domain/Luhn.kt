package com.yishenghuang.skry.domain

object Luhn {
    fun isValid(cardNumber: String): Boolean {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length !in 13..19) return false
        var sum = 0
        var isSecond = false
        for (i in digits.length - 1 downTo 0) {
            var d = digits[i] - '0'
            if (isSecond) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            isSecond = !isSecond
        }
        return sum % 10 == 0
    }
}
