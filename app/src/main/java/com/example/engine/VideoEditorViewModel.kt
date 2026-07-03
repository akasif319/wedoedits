package com.example.engine

import android.app.Application
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

data class PlaybackProperties(
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val opacity: Float,
    val volume: Float
)

class VideoEditorViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(db.projectDao())

    // All drafts from database
    val drafts: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current project state
    private val _currentProjectId = MutableStateFlow<Int?>(null)
    val currentProjectId = _currentProjectId.asStateFlow()

    private val _projectName = MutableStateFlow("New Project")
    val projectName = _projectName.asStateFlow()

    private val _aspectRatio = MutableStateFlow("9:16") // "9:16", "16:9", "1:1"
    val aspectRatio = _aspectRatio.asStateFlow()

    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timelineItems = _timelineItems.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs = _playheadMs.asStateFlow()

    private val _timelineZoom = MutableStateFlow(5.0f) // pixels per ms
    val timelineZoom = _timelineZoom.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId = _selectedItemId.asStateFlow()

    private val _snapToGrid = MutableStateFlow(true)
    val snapToGrid = _snapToGrid.asStateFlow()

    // Native TTS Engine
    private var tts: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady = _isTtsReady.asStateFlow()

    // Playback ticking coroutine
    private var playbackJob: Job? = null

    // Audio SFX/Music playback simulation (trigger indicators)
    private val _activeAudioTriggers = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeAudioTriggers = _activeAudioTriggers.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            _isTtsReady.value = true
        }
    }

    fun setZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(1.0f, 15.0f)
    }

    fun toggleSnap() {
        _snapToGrid.value = !_snapToGrid.value
    }

    fun selectItem(id: String?) {
        _selectedItemId.value = id
    }

    fun setAspectRatio(ratio: String) {
        _aspectRatio.value = ratio
        saveCurrentProjectToDraft()
    }

    fun startPlayback() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val startPlayhead = _playheadMs.value
            val totalDuration = getMaxTimelineDuration()
            while (_isPlaying.value) {
                val elapsed = System.currentTimeMillis() - startTime
                var newPlayhead = startPlayhead + elapsed
                if (newPlayhead >= totalDuration) {
                    newPlayhead = 0L
                    _isPlaying.value = false
                    _playheadMs.value = 0L
                    break
                }
                _playheadMs.value = newPlayhead
                triggerAudioSimulation(newPlayhead)
                delay(16) // ~60fps
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekTo(ms: Long) {
        val total = getMaxTimelineDuration()
        var targetMs = ms.coerceIn(0L, maxOf(total, 1000L))
        if (_snapToGrid.value) {
            targetMs = applySnapping(targetMs)
        }
        _playheadMs.value = targetMs
        triggerAudioSimulation(targetMs)
    }

    private fun applySnapping(ms: Long): Long {
        // Snap to nearest clip bounds, audio beats, or 1-second grid intervals
        val items = _timelineItems.value
        val snapThreshold = 300L // snap within 300ms

        // Check clip boundaries (start, end)
        for (item in items) {
            if (kotlin.math.abs(item.startInTimeline - ms) < snapThreshold) {
                return item.startInTimeline
            }
            val end = item.startInTimeline + item.duration
            if (kotlin.math.abs(end - ms) < snapThreshold) {
                return end
            }
        }

        // Snap to whole seconds
        val nearestSecond = kotlin.math.round(ms.toFloat() / 1000f).toLong() * 1000L
        if (kotlin.math.abs(nearestSecond - ms) < snapThreshold) {
            return nearestSecond
        }

        return ms
    }

    // Trigger visual feedback for audio waveforms actively playing
    private var lastAudioTriggerMs = 0L
    private fun triggerAudioSimulation(playhead: Long) {
        val triggers = mutableMapOf<String, Float>()
        _timelineItems.value.forEach { item ->
            if (item.type == TrackType.AUDIO && playhead >= item.startInTimeline && playhead <= (item.startInTimeline + item.duration)) {
                val offset = playhead - item.startInTimeline
                // Generate simulated amplitude
                val ratio = offset.toFloat() / item.duration.toFloat()
                val idx = (ratio * 100).toInt() % 10
                triggers[item.id] = 0.3f + (idx * 0.07f)
            }
        }
        _activeAudioTriggers.value = triggers
    }

    fun getMaxTimelineDuration(): Long {
        val items = _timelineItems.value
        if (items.isEmpty()) return 5000L // default timeline view
        return items.maxOf { it.startInTimeline + it.duration }
    }

    // Load project from draft
    fun loadProject(project: ProjectEntity) {
        stopPlayback()
        _currentProjectId.value = project.id
        _projectName.value = project.name
        _aspectRatio.value = project.aspectRatio
        val converters = Converters()
        _timelineItems.value = converters.fromTimelineItemsJson(project.itemsJson)
        _playheadMs.value = 0L
        _selectedItemId.value = null
    }

    // Create a new blank project
    fun createNewProject(name: String = "Project ${System.currentTimeMillis() % 10000}") {
        stopPlayback()
        _currentProjectId.value = null
        _projectName.value = name
        _aspectRatio.value = "9:16"
        _timelineItems.value = emptyList()
        _playheadMs.value = 0L
        _selectedItemId.value = null
        saveCurrentProjectToDraft()
    }

    // Save project
    fun saveCurrentProjectToDraft() {
        viewModelScope.launch {
            val converters = Converters()
            val itemsJson = converters.toTimelineItemsJson(_timelineItems.value)
            val duration = getMaxTimelineDuration()
            val entity = ProjectEntity(
                id = _currentProjectId.value ?: 0,
                name = _projectName.value,
                duration = duration,
                aspectRatio = _aspectRatio.value,
                itemsJson = itemsJson,
                lastModified = System.currentTimeMillis()
            )
            val id = repository.insertProject(entity)
            if (_currentProjectId.value == null) {
                _currentProjectId.value = id.toInt()
            }
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
            if (_currentProjectId.value == id) {
                createNewProject()
            }
        }
    }

    // Add clip to timeline (Video, Image, Audio, Text, Sticker, Effect)
    fun addClipToTimeline(
        type: TrackType,
        title: String,
        duration: Long,
        filePathOrAssetId: String,
        extraConfig: (TimelineItem) -> TimelineItem = { it }
    ) {
        val playhead = _playheadMs.value
        val item = TimelineItem(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title,
            duration = duration,
            startInTimeline = playhead,
            sourceDuration = duration,
            filePathOrAssetId = filePathOrAssetId
        ).let(extraConfig)

        val updated = _timelineItems.value.toMutableList()
        updated.add(item)
        _timelineItems.value = updated
        _selectedItemId.value = item.id
        saveCurrentProjectToDraft()
    }

    // Timeline actions
    fun deleteSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        val updated = _timelineItems.value.filterNot { it.id == selectedId }
        _timelineItems.value = updated
        _selectedItemId.value = null
        saveCurrentProjectToDraft()
    }

    fun duplicateSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return

        // Place it immediately after the original in timeline
        val clone = item.copy(
            id = UUID.randomUUID().toString(),
            startInTimeline = item.startInTimeline + item.duration
        )

        // Ripple shift subsequent items on same tracks to prevent overlap
        val updated = items.map { other ->
            if (other.type == clone.type && other.startInTimeline >= clone.startInTimeline) {
                other.copy(startInTimeline = other.startInTimeline + clone.duration)
            } else {
                other
            }
        }.toMutableList()

        updated.add(clone)
        _timelineItems.value = updated
        _selectedItemId.value = clone.id
        saveCurrentProjectToDraft()
    }

    fun splitSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return
        val playhead = _playheadMs.value

        // Check if playhead intersects item
        if (playhead <= item.startInTimeline || playhead >= (item.startInTimeline + item.duration)) {
            return // playhead is not within clip bounds
        }

        val splitOffsetMs = playhead - item.startInTimeline
        val sourceSplitOffset = item.sourceStartOffset + (splitOffsetMs * item.speed).toLong()

        // Create Left and Right pieces
        val itemLeft = item.copy(
            id = UUID.randomUUID().toString(),
            duration = splitOffsetMs
        )
        val itemRight = item.copy(
            id = UUID.randomUUID().toString(),
            duration = item.duration - splitOffsetMs,
            startInTimeline = playhead,
            sourceStartOffset = sourceSplitOffset
        )

        val updated = items.filterNot { it.id == selectedId }.toMutableList()
        updated.add(itemLeft)
        updated.add(itemRight)

        _timelineItems.value = updated
        _selectedItemId.value = itemRight.id // select right piece
        saveCurrentProjectToDraft()
    }

    fun trimSelectedItemStart(trimDeltaMs: Long) {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return

        val maxTrim = item.duration - 200L // must keep at least 200ms
        val delta = trimDeltaMs.coerceIn(-item.startInTimeline, maxTrim)
        if (delta == 0L) return

        val updated = items.map { other ->
            if (other.id == selectedId) {
                val newStart = other.startInTimeline + delta
                val newDuration = other.duration - delta
                val newSourceOffset = other.sourceStartOffset + (delta * other.speed).toLong()
                other.copy(
                    startInTimeline = newStart,
                    duration = newDuration,
                    sourceStartOffset = newSourceOffset
                )
            } else {
                other
            }
        }
        _timelineItems.value = updated
        saveCurrentProjectToDraft()
    }

    fun trimSelectedItemEnd(trimDeltaMs: Long) {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return

        val newDuration = (item.duration + trimDeltaMs).coerceIn(200L, item.sourceDuration - item.sourceStartOffset)
        val updated = items.map { other ->
            if (other.id == selectedId) {
                other.copy(duration = newDuration)
            } else {
                other
            }
        }
        _timelineItems.value = updated
        saveCurrentProjectToDraft()
    }

    // Update individual properties of selected item
    fun updateSelectedItem(updateBlock: (TimelineItem) -> TimelineItem) {
        val selectedId = _selectedItemId.value ?: return
        val updated = _timelineItems.value.map { item ->
            if (item.id == selectedId) {
                updateBlock(item)
            } else {
                item
            }
        }
        _timelineItems.value = updated
        saveCurrentProjectToDraft()
    }

    // Precision Operations

    // 1. FREEZE FRAME
    fun freezeFrameSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return
        val playhead = _playheadMs.value

        if (playhead <= item.startInTimeline || playhead >= (item.startInTimeline + item.duration)) {
            return
        }

        val splitOffset = playhead - item.startInTimeline
        val freezeDuration = 3000L // 3 seconds

        // Left clip
        val left = item.copy(id = UUID.randomUUID().toString(), duration = splitOffset)
        // Freeze frame clip
        val freeze = item.copy(
            id = UUID.randomUUID().toString(),
            title = "Freeze (${item.title})",
            duration = freezeDuration,
            startInTimeline = playhead,
            isFrozen = true
        )
        // Right clip
        val right = item.copy(
            id = UUID.randomUUID().toString(),
            duration = item.duration - splitOffset,
            startInTimeline = playhead + freezeDuration,
            sourceStartOffset = item.sourceStartOffset + (splitOffset * item.speed).toLong()
        )

        // Ripple shift subsequent tracks
        val updated = items.filterNot { it.id == selectedId }.map { other ->
            if (other.startInTimeline >= playhead) {
                other.copy(startInTimeline = other.startInTimeline + freezeDuration)
            } else {
                other
            }
        }.toMutableList()

        updated.add(left)
        updated.add(freeze)
        updated.add(right)

        _timelineItems.value = updated
        _selectedItemId.value = freeze.id
        saveCurrentProjectToDraft()
    }

    // 2. EXTRACT AUDIO
    fun extractAudioFromSelected() {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return

        if (item.type != TrackType.VIDEO) return

        // Create corresponding Audio track item
        val extractedAudio = TimelineItem(
            id = UUID.randomUUID().toString(),
            type = TrackType.AUDIO,
            title = "Extracted: ${item.title}",
            duration = item.duration,
            startInTimeline = item.startInTimeline,
            sourceDuration = item.duration,
            filePathOrAssetId = "audio_synthwave", // dummy track or extract from video
            volume = 1.0f
        )

        // Mute video
        val updated = items.map { other ->
            if (other.id == selectedId) {
                other.copy(volume = 0.0f)
            } else {
                other
            }
        }.toMutableList()

        updated.add(extractedAudio)
        _timelineItems.value = updated
        _selectedItemId.value = extractedAudio.id
        saveCurrentProjectToDraft()
    }

    // 3. TEXT TO SPEECH (TTS)
    fun convertSelectedTextToSpeech(voiceId: String) {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return

        if (item.type != TrackType.TEXT) return

        // Add a spoken speech track right under text
        val ttsItem = TimelineItem(
            id = UUID.randomUUID().toString(),
            type = TrackType.AUDIO,
            title = "TTS Voice (${item.title})",
            duration = maxOf(item.duration, 2500L),
            startInTimeline = item.startInTimeline,
            sourceDuration = maxOf(item.duration, 2500L),
            filePathOrAssetId = "audio_lofi", // simulation backer file
            volume = 1.0f,
            ttsVoiceId = voiceId
        )

        // Standard Android TTS utterance playback
        if (_isTtsReady.value) {
            tts?.speak(item.title, TextToSpeech.QUEUE_FLUSH, null, "videdit_tts_${item.id}")
        }

        val updated = items.toMutableList()
        updated.add(ttsItem)
        _timelineItems.value = updated
        saveCurrentProjectToDraft()
    }

    // 4. AUTO CAPTIONS (Speech to Text)
    fun generateAutoCaptions() {
        viewModelScope.launch {
            val items = _timelineItems.value
            // Find video/audio track items
            val speechSources = items.filter { it.type == TrackType.VIDEO || it.type == TrackType.AUDIO }
            if (speechSources.isEmpty()) {
                Toast.makeText(getApplication(), "No media on timeline to transcribe!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Simulate automatic transcribing with beautiful delays
            delay(1500) // simulated whisper API latency

            // Create timed transcription subtitle blocks
            val captionBlocks = listOf(
                "CapCut Pro represents the ultimate",
                "native mobile editing experience",
                "with lightning fast GPU rendering",
                "and highly responsive timeline!"
            )

            val trackLength = getMaxTimelineDuration()
            val step = maxOf(3000L, trackLength / maxOf(1, captionBlocks.size))

            val newCaptions = captionBlocks.mapIndexed { index, text ->
                TimelineItem(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.TEXT,
                    title = text,
                    duration = 2500L,
                    startInTimeline = index * step,
                    sourceDuration = 2500L,
                    filePathOrAssetId = "text",
                    textAnimationType = "Typewriter",
                    textCustomBgColor = 0xAA000000.toInt(),
                    textCustomColor = 0xFFFFFF00.toInt() // Yellow
                )
            }

            val updated = items.toMutableList()
            updated.addAll(newCaptions)
            _timelineItems.value = updated
            saveCurrentProjectToDraft()
            Toast.makeText(getApplication(), "Generated ${newCaptions.size} auto-captions!", Toast.LENGTH_SHORT).show()
        }
    }

    // 5. MOTION KEYFRAME ACTIONS
    fun toggleKeyframeAtPlayhead() {
        val selectedId = _selectedItemId.value ?: return
        val items = _timelineItems.value
        val item = items.find { it.id == selectedId } ?: return
        val playhead = _playheadMs.value

        val relativeTimeMs = playhead - item.startInTimeline
        if (relativeTimeMs < 0 || relativeTimeMs > item.duration) return

        // If exists, delete it. If not, add one containing the item's current values!
        val exists = item.keyframes.find { kotlin.math.abs(it.timeOffsetMs - relativeTimeMs) < 100 }
        val updatedKeyframes = if (exists != null) {
            item.keyframes.filter { it != exists }
        } else {
            val keyframe = KeyframeData(
                timeOffsetMs = relativeTimeMs,
                positionX = item.positionX,
                positionY = item.positionY,
                scale = item.scale,
                opacity = item.opacity,
                rotation = item.rotation,
                volume = item.volume
            )
            item.keyframes + keyframe
        }

        updateSelectedItem { it.copy(keyframes = updatedKeyframes.sortedBy { k -> k.timeOffsetMs }) }
    }

    // 6. INTERPOLATE PLAYBACK VALUES
    fun getInterpolatedProperties(item: TimelineItem, playhead: Long): PlaybackProperties {
        if (item.keyframes.isEmpty()) {
            return PlaybackProperties(
                x = item.positionX,
                y = item.positionY,
                scale = item.scale,
                rotation = item.rotation,
                opacity = item.opacity,
                volume = item.volume
            )
        }

        val relativeTimeMs = playhead - item.startInTimeline
        val activePlayhead = (relativeTimeMs * item.speed).toLong()

        val sorted = item.keyframes.sortedBy { it.timeOffsetMs }
        val before = sorted.lastOrNull { it.timeOffsetMs <= activePlayhead }
        val after = sorted.firstOrNull { it.timeOffsetMs > activePlayhead }

        if (before == null && after == null) {
            return PlaybackProperties(item.positionX, item.positionY, item.scale, item.rotation, item.opacity, item.volume)
        }
        if (before != null && after == null) {
            return PlaybackProperties(
                x = before.positionX ?: item.positionX,
                y = before.positionY ?: item.positionY,
                scale = before.scale ?: item.scale,
                rotation = before.rotation ?: item.rotation,
                opacity = before.opacity ?: item.opacity,
                volume = before.volume ?: item.volume
            )
        }
        if (before == null && after != null) {
            return PlaybackProperties(
                x = after.positionX ?: item.positionX,
                y = after.positionY ?: item.positionY,
                scale = after.scale ?: item.scale,
                rotation = after.rotation ?: item.rotation,
                opacity = after.opacity ?: item.opacity,
                volume = after.volume ?: item.volume
            )
        }

        // Lerp between before & after
        val denom = (after!!.timeOffsetMs - before!!.timeOffsetMs).toFloat()
        val t = if (denom > 0) (activePlayhead - before.timeOffsetMs).toFloat() / denom else 0f
        val clampedT = t.coerceIn(0f, 1f)

        return PlaybackProperties(
            x = lerp(before.positionX ?: item.positionX, after.positionX ?: item.positionX, clampedT),
            y = lerp(before.positionY ?: item.positionY, after.positionY ?: item.positionY, clampedT),
            scale = lerp(before.scale ?: item.scale, after.scale ?: item.scale, clampedT),
            rotation = lerp(before.rotation ?: item.rotation, after.rotation ?: item.rotation, clampedT),
            opacity = lerp(before.opacity ?: item.opacity, after.opacity ?: item.opacity, clampedT),
            volume = lerp(before.volume ?: item.volume, after.volume ?: item.volume, clampedT)
        )
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + fraction * (end - start)
    }

    override fun onCleared() {
        stopPlayback()
        tts?.shutdown()
        super.onCleared()
    }
}
