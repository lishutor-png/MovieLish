package com.example.player

enum class AspectRatioMode(val label: String) {
    FIT("Muat Layar (Fit)"),
    FILL_CROP("Penuh Potong (Crop)"),
    RATIO_16_9("16:9 Cinema"),
    RATIO_4_3("4:3 Standar")
}

enum class GestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    ASPECT_RATIO,
    LOCK_INFO
}

data class PlayerGestureHudState(
    val gestureType: GestureType = GestureType.NONE,
    val valuePercent: Float = 0f, // 0f to 1f
    val seekTargetMs: Long = 0L,
    val seekDeltaMs: Long = 0L,
    val message: String = "",
    val isVisible: Boolean = false
)
