package com.example.time

internal fun sinDeg(x: Number) = StrictMath.sin(StrictMath.toRadians(x.toDouble()))
internal fun cosDeg(x: Number) = StrictMath.cos(StrictMath.toRadians(x.toDouble()))
internal fun tanDeg(x: Number) = StrictMath.tan(StrictMath.toRadians(x.toDouble()))

internal fun asinDeg(sin: Number) = StrictMath.toDegrees(StrictMath.asin(sin.toDouble()))
internal fun acosDeg(cos: Number) = StrictMath.toDegrees(StrictMath.acos(cos.toDouble()))
internal fun acotDeg(cot: Number) = StrictMath.toDegrees(StrictMath.atan(1 / cot.toDouble()))

internal fun atan2Deg(y: Number, x: Number) = StrictMath.toDegrees(StrictMath.atan2(y.toDouble(), x.toDouble()))

// TODO: research whether to allow 360 degrees
internal fun normalizeAngle(angleInDegrees: Double) = angleInDegrees % 360 + if (angleInDegrees < 0) 360 else 0