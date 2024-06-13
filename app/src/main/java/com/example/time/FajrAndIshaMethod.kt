package com.example.time
sealed interface FajrOffset
sealed interface IshaOffset
interface FajrAndIshaMethod {

    val fajrOffset: FajrOffset
    val ishaOffset: IshaOffset
}