package com.wakeiq.domain.model

enum class MotionSensitivity(val threshold: Float) {
    LOW(0.8f),
    MEDIUM(0.4f),
    HIGH(0.2f),
}
