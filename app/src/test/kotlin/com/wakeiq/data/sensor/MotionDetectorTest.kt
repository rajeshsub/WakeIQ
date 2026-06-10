package com.wakeiq.data.sensor

import com.wakeiq.domain.model.MotionSensitivity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MotionDetectorTest {

    @Test
    fun `LOW sensitivity has highest threshold`() {
        assert(MotionSensitivity.LOW.threshold > MotionSensitivity.MEDIUM.threshold)
    }

    @Test
    fun `MEDIUM sensitivity threshold between LOW and HIGH`() {
        assert(MotionSensitivity.MEDIUM.threshold > MotionSensitivity.HIGH.threshold)
        assert(MotionSensitivity.MEDIUM.threshold < MotionSensitivity.LOW.threshold)
    }

    @Test
    fun `HIGH sensitivity has lowest threshold`() {
        assertEquals(MotionSensitivity.HIGH, MotionSensitivity.entries.minByOrNull { it.threshold })
    }

    @Test
    fun `all sensitivities have positive thresholds`() {
        MotionSensitivity.entries.forEach { s ->
            assert(s.threshold > 0f) { "$s threshold must be positive" }
        }
    }
}
