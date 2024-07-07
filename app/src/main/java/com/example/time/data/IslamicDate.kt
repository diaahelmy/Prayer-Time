package com.example.time.data

data class IslamicDate(val year: Int, val month: Int, val dayOfMonth: Int) {
    val monthName: String
        get() {
            return when (month) {
                1 -> "Muharram"
                2 -> "Safar"
                3 -> "Rabi' al-Awwal"
                4 -> "Rabi' al-Thani"
                5 -> "Jumada al-Awwal"
                6 -> "Jumada al-Thani"
                7 -> "Rajab"
                8 -> "Sha'ban"
                9 -> "Ramadan"
                10 -> "Shawwal"
                11 -> "Dhu al-Qi'dah"
                12 -> "Dhu al-Hijjah"
                else -> "Unknown"
            }
        }
}