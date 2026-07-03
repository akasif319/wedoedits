package com.example.data

data class StockVideo(
    val id: String,
    val title: String,
    val durationMs: Long,
    val colorPrimary: Long, // Hex Long for styling
    val colorSecondary: Long,
    val description: String,
    val theme: String // "Cyberpunk", "Vintage", "Nature", "Futuristic", "Minimalist"
)

data class StockAudio(
    val id: String,
    val title: String,
    val category: String, // "Music" or "SFX"
    val durationMs: Long,
    val bpm: Int,
    val waveformPoints: List<Float> // Waveform points for rendering (0f to 1f)
)

data class StickerAsset(
    val id: String,
    val name: String,
    val iconEmoji: String, // High quality emoji fallback or icon representation
    val animType: String // "Spin", "Pulse", "Bounce", "Shake"
)

object TemplateAssets {
    val stockVideos = listOf(
        StockVideo("stock_tokyo", "Neon Tokyo Nights", 15000L, 0xFFFF0055, 0xFF00FFFF, "Cyberpunk cityscape with rain and flashing neon signs", "Cyberpunk"),
        StockVideo("stock_forest", "Golden Hour Forest", 12000L, 0xFFFF9900, 0xFF336600, "Sunlight filtering through redwood tree canopies", "Nature"),
        StockVideo("stock_ocean", "Retro Sunset Beach", 18000L, 0xFFFF5500, 0xFF550088, "Synthwave style ocean waves under large pixelated sun", "Vintage"),
        StockVideo("stock_matrix", "Digital Matrix Rain", 10000L, 0xFF00FF00, 0xFF001100, "Falling streams of binary code in high tech dark room", "Futuristic"),
        StockVideo("stock_nebula", "Cosmic Stardust", 20000L, 0xFF8800FF, 0xFF000033, "Colorful rotating galactic nebulae and twinkling stars", "Futuristic")
    )

    val stockAudios = listOf(
        // Music
        StockAudio("audio_synthwave", "Retro Grid Runner", "Music", 30000L, 120, listOf(0.3f, 0.5f, 0.8f, 0.4f, 0.9f, 0.7f, 0.6f, 0.8f, 0.5f, 0.3f, 0.6f, 0.8f, 0.9f, 0.5f, 0.7f, 0.4f, 0.6f, 0.8f, 0.5f, 0.3f)),
        StockAudio("audio_lofi", "Cozy Coffee Shop", "Music", 45000L, 80, listOf(0.4f, 0.4f, 0.5f, 0.3f, 0.6f, 0.5f, 0.4f, 0.5f, 0.4f, 0.3f, 0.5f, 0.4f, 0.6f, 0.5f, 0.4f, 0.3f, 0.4f, 0.5f, 0.4f, 0.4f)),
        StockAudio("audio_epic", "Unstoppable Cinematic", "Music", 25000L, 140, listOf(0.2f, 0.5f, 0.9f, 0.8f, 0.9f, 0.9f, 0.6f, 0.8f, 0.7f, 0.5f, 0.8f, 0.9f, 0.9f, 0.8f, 0.7f, 0.6f, 0.8f, 0.9f, 0.5f, 0.2f)),
        // Sound Effects (SFX)
        StockAudio("sfx_swoosh", "Cyber Glitch Whoosh", "SFX", 1200L, 0, listOf(0.1f, 0.2f, 0.4f, 0.8f, 0.9f, 0.5f, 0.2f, 0.1f)),
        StockAudio("sfx_laser", "8-Bit Laser Blast", "SFX", 800L, 0, listOf(0.9f, 0.8f, 0.6f, 0.4f, 0.2f, 0.1f)),
        StockAudio("sfx_camera", "Vintage Camera Shutter", "SFX", 500L, 0, listOf(0.8f, 0.9f, 0.2f, 0.0f)),
        StockAudio("sfx_scratch", "DJ Record Scratch", "SFX", 1500L, 0, listOf(0.5f, 0.8f, 0.9f, 0.8f, 0.5f, 0.8f, 0.9f, 0.4f))
    )

    val stickers = listOf(
        StickerAsset("sticker_sparkle", "Sparkles", "✨", "Pulse"),
        StickerAsset("sticker_heart", "Pulse Heart", "❤️", "Pulse"),
        StickerAsset("sticker_fire", "Lit Fire", "🔥", "Bounce"),
        StickerAsset("sticker_cool", "Sunglasses Face", "😎", "Spin"),
        StickerAsset("sticker_crown", "Royal Crown", "👑", "Pulse"),
        StickerAsset("sticker_arrow", "Pointer Arrow", "🎯", "Bounce"),
        StickerAsset("sticker_glitch", "Cyber Glitch", "⚡", "Shake"),
        StickerAsset("sticker_vibe", "Vibe Badge", "🎵", "Spin")
    )
}
