package com.example.player

enum class AspectRatioMode(val label: String) {
    FILL_CROP("Penuh Layar (Rasio Asli)"),
    FIT("Muat Layar (Fit)"),
    RATIO_16_9("16:9 Cinema"),
    RATIO_4_3("4:3 Standar")
}

enum class GestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    ASPECT_RATIO,
    LOCK_INFO,
    SLEEP_TIMER,
    ORIENTATION
}

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("Nonaktif", 0),
    MIN_15("15 Menit", 15),
    MIN_30("30 Menit", 30),
    MIN_45("45 Menit", 45),
    MIN_60("60 Menit", 60),
    CUSTOM("Kustom", -2),
    END_OF_VIDEO("Akhir Video Ini", -1)
}

data class SleepTimerState(
    val option: SleepTimerOption = SleepTimerOption.OFF,
    val targetTimestampMs: Long = 0L,
    val customMinutes: Int = 15,
    val isCustom: Boolean = false,
    val fadeOutAudio: Boolean = true
)

data class PlayerGestureHudState(
    val gestureType: GestureType = GestureType.NONE,
    val valuePercent: Float = 0f, // 0f to 1f
    val seekTargetMs: Long = 0L,
    val seekDeltaMs: Long = 0L,
    val message: String = "",
    val isVisible: Boolean = false
)
