package com.example.data

import com.squareup.moshi.JsonClass

enum class TrackType {
    VIDEO, AUDIO, TEXT, STICKER, EFFECT
}

@JsonClass(generateAdapter = true)
data class KeyframeData(
    val timeOffsetMs: Long,
    val positionX: Float? = null,
    val positionY: Float? = null,
    val scale: Float? = null,
    val opacity: Float? = null,
    val rotation: Float? = null,
    val volume: Float? = null
)

@JsonClass(generateAdapter = true)
data class CurvePoint(
    val x: Float, // 0.0 to 1.0 (time ratio)
    val y: Float  // 0.1 to 10.0 (speed factor)
)

@JsonClass(generateAdapter = true)
data class TimelineItem(
    val id: String,
    val type: TrackType,
    val title: String,
    val duration: Long, // Duration of active segment in timeline (ms)
    val startInTimeline: Long, // Start position in main playhead timeline (ms)
    val sourceStartOffset: Long = 0L, // Crop/trim start offset (ms)
    val sourceDuration: Long, // Original source media duration (ms)
    val speed: Float = 1.0f, // Uniform speed (e.g. 1.0x, 2.0x, 0.5x)
    val speedCurve: List<CurvePoint>? = null, // Custom Bezier curves for speed ramping
    val volume: Float = 1.0f,
    val filePathOrAssetId: String, // "stock_cyberpunk", "stock_beats_1", "file://...", etc.
    val positionX: Float = 0.0f, // X shift on preview canvas (-200 to +200)
    val positionY: Float = 0.0f, // Y shift on preview canvas (-200 to +200)
    val scale: Float = 1.0f, // Zoom level (0.1 to 5.0)
    val rotation: Float = 0.0f, // Rotation in degrees (0 to 360)
    val opacity: Float = 1.0f, // Opacity (0.0 to 1.0)
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val keyframes: List<KeyframeData> = emptyList(),
    val filterType: String? = null, // "Vintage", "Cyberpunk", "Film", "B&W", "Lomo", etc.
    // Video Adjustments
    val brightness: Float = 0.0f,  // -1.0 to 1.0
    val contrast: Float = 0.0f,    // -1.0 to 1.0
    val saturation: Float = 0.0f,  // -1.0 to 1.0
    val warmth: Float = 0.0f,      // -1.0 to 1.0
    val sharpen: Float = 0.0f,     // 0.0 to 1.0
    val vignette: Float = 0.0f,    // 0.0 to 1.0
    val fade: Float = 0.0f,        // 0.0 to 1.0
    val effectType: String? = null, // "Glitch", "Blur", "Shutter", "Flash", "RGB Split"
    val stickerType: String? = null, // "sticker_heart", "sticker_arrow", "sticker_sparkle", etc.
    val transitionType: String? = null, // "Mix", "Fade", "Wipe", "Pull In", "Zoom"
    val transitionDuration: Long = 500L,
    val textCustomFont: String? = "Default",
    val textCustomColor: Int = 0xFFFFFFFF.toInt(),
    val textCustomBgColor: Int = 0x00000000.toInt(),
    val textAnimationType: String? = "None", // "Typewriter", "Fade", "Pop", "Slide"
    val ttsVoiceId: String? = null,
    val isFrozen: Boolean = false,
    val isChromaKeyEnabled: Boolean = false,
    val chromaKeyColor: Int = 0xFF00FF00.toInt() // Default Green
)
