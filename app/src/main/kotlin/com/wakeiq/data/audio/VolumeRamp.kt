package com.wakeiq.data.audio

/**
 * Pure volume-ramp maths for the alarm escalation, isolated from ExoPlayer so it can be unit-tested.
 *
 * The ramp runs in two phases that always sum to the configured duration: a gentle whisper phase
 * from [START_VOLUME] to [WHISPER_VOLUME], then an escalation phase up to the target volume. Keeping
 * the split here lets the audio ramp stay in step with the brightness ramp at any duration.
 */
object VolumeRamp {
    const val START_VOLUME = 0.05f
    const val WHISPER_VOLUME = 0.15f
    const val STEPS = 100

    private const val WHISPER_PHASE_FRACTION = 0.40
    private const val WHISPER_FLOOR_MS = 20_000L
    private const val MIN_ESCALATION_MS = 10_000L

    /**
     * Split the total ramp into a whisper phase and an escalation phase. The whisper phase is 40
     * percent of the total but never shorter than [WHISPER_FLOOR_MS], so even very short ramps open
     * gently, and never so long that the escalation drops below [MIN_ESCALATION_MS]. The two phases
     * always sum to the total.
     */
    fun computeRampPhases(totalMs: Long): Pair<Long, Long> {
        val raw = (totalMs * WHISPER_PHASE_FRACTION).toLong()
        val whisper = raw.coerceAtLeast(WHISPER_FLOOR_MS)
            .coerceAtMost((totalMs - MIN_ESCALATION_MS).coerceAtLeast(0L))
        val escalation = totalMs - whisper
        return whisper to escalation
    }

    /**
     * The [STEPS] interim volume levels the player steps through ramping from [from] to [to], each
     * clamped to the valid 0..1 range. The caller sets the exact [to] value after the last step.
     */
    fun volumeSteps(from: Float, to: Float): List<Float> {
        val volStep = (to - from) / STEPS
        return (0 until STEPS).map { (from + volStep * it).coerceIn(0f, 1f) }
    }
}
