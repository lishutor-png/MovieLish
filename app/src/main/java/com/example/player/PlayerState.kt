package com.example.player

enum class AspectRatioMode {
    FIT,
    FILL_CROP,
    RATIO_16_9,
    RATIO_4_3
}

enum class GestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK
}

data class PlayerGestureHudState(
    val gestureType: GestureType = GestureType.NONE,
    val valuePercent: Float = 0f, // 0f to 1f
    val seekTargetMs: Long = 0L,
    val seekDeltaMs: Long = 0L,
    val isVisible: Boolean = false
)
