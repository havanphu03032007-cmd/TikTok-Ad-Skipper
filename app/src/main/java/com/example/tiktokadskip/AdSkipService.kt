package com.example.tiktokadskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.Normalizer

class AdSkipService : AccessibilityService() {

    companion object {
        private const val TAG = "AdSkip"

        // Bật true để xem trong Logcat các chữ ngắn / dòng nhạc trên màn hình TikTok,
        // dùng để tìm đúng nhãn nếu bản TikTok của bạn dùng chữ khác.
        private const val DEBUG = false

        // So khớp CHÍNH XÁC, không phân biệt hoa thường.
        private val AD_LABELS = setOf("sponsored", "được tài trợ", "quảng cáo", "ad")
        private val LIVE_LABELS = setOf("live", "trực tiếp")
        private val GAME_LABELS = setOf(
            "play now", "chơi ngay", "install now", "cài đặt ngay", "play game", "chơi game"
        )

        // Nhãn LIVE ở thanh menu trên cùng không được tính là livestream
        private const val TOP_BAR_FRACTION = 0.15f

        // Dòng tên nhạc nằm ở phần dưới màn hình, thường có các dấu hiệu sau
        // (đã bỏ dấu tiếng Việt, viết thường)
        private const val MUSIC_AREA_FRACTION = 0.55f
        private val MUSIC_MARKERS = listOf(
            "nhac nen", "am thanh goc", "original sound", "\u266A", "\u266B", " - "
        )

        private const val COOLDOWN_MS = 1500L
        private const val SCAN_INTERVAL_MS = 400L
        private const val MAX_DEPTH = 40

        // Chế độ "chỉ xem nhạc yêu thích"
        private const val DWELL_MS = 800L            // chờ video ổn định rồi mới quyết định
        private const val MAX_CONSECUTIVE_SKIPS = 25 // vuốt liên tiếp tối đa, sau đó nghỉ
        private const val PAUSE_MS = 60_000L
    }

    private var lastSwipeAt = 0L
    private var lastScanAt = 0L
    private var screenHeight = 0

    // Trạng thái theo dõi nhạc
    private var currentSignature = ""
    private var firstSeenAt = 0L
    private var lastMatchSignature = ""
    private var consecutiveSkips = 0
    private var pausedUntil = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() !in TIKTOK_PACKAGES) return

        val now = System.currentTimeMillis()
        if (now - lastSwipeAt < COOLDOWN_MS) return
        if (now - lastScanAt < SCAN_INTERVAL_MS) return
        lastScanAt = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() !in TIKTOK_PACKAGES) return

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val skipAds = prefs.getBoolean("skip_ads", true)
        val skipLive = prefs.getBoolean("skip_live", true)
        val skipGame = prefs.getBoolean("skip_game", true)
        val musicNotify = prefs.getBoolean("music_notify", false)
        val musicOnly = prefs.getBoolean("music_only", false)

        screenHeight = resources.displayMetrics.heightPixels

        if (skipAds || skipLive || skipGame) {
            val reason = findReason(root, 0, skipAds, skipLive, skipGame)
            if (reason != null) {
                Log.d(TAG, "Bỏ qua ($reason)")
                lastSwipeAt = now
                swipeToNextVideo()
                return
            }
        }

        if (musicNotify || musicOnly) handleMusic(root, prefs, now, musicNotify, musicOnly)
    }

    // ---------- Quảng cáo / livestream / game ----------

    private fun findReason(
        node: AccessibilityNodeInfo?, depth: Int,
        ads: Boolean, live: Boolean, game: Boolean
    ): String? {
        if (node == null || depth > MAX_DEPTH) return null

        val text = node.text?.toString()?.trim()?.lowercase()
        val desc = node.contentDescription?.toString()?.trim()?.lowercase()

        if (DEBUG) {
            if (!text.isNullOrEmpty() && text.length <= 20) Log.d(TAG, "text=$text")
            if (!desc.isNullOrEmpty() && desc.length <= 20) Log.d(TAG, "desc=$desc")
        }

        for (s in arrayOf(text, desc)) {
            if (s.isNullOrEmpty()) continue
            if (ads && s in AD_LABELS) return "quảng cáo"
            if (game && s in GAME_LABELS) return "quảng cáo game"
            if (live && s in LIVE_LABELS && !isInTopBar(node)) return "livestream"
        }

        for (i in 0 until node.childCount) {
            val r = findReason(node.getChild(i), depth + 1, ads, live, game)
            if (r != null) return r
        }
        return null
    }

    private fun isInTopBar(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.top < screenHeight * TOP_BAR_FRACTION
    }

    // ---------- Nhạc yêu thích ----------

    private fun handleMusic(
        root: AccessibilityNodeInfo, prefs: SharedPreferences,
        now: Long, notify: Boolean, onlyFavs: Boolean
    ) {
        val favs = (prefs.getString("fav_music", "") ?: "")
            .split(",", "\n")
            .map { norm(it.trim()) }
            .filter { it.isNotEmpty() }
        if (favs.isEmpty()) return

        val candidates = mutableListOf<String>()
        collectMusicCandidates(root, 0, candidates)
        // Không thấy dòng nhạc nào (đang tải, hoặc không phải màn hình video) -> không làm gì
        if (candidates.isEmpty()) return

        val signature = candidates.joinToString("|")
        if (signature != currentSignature) {
            currentSignature = signature
            firstSeenAt = now
        }

        val matched = favs.any { signature.contains(it) }

        if (matched) {
            consecutiveSkips = 0
            if (signature != lastMatchSignature) {
                lastMatchSignature = signature
                Log.d(TAG, "Gặp nhạc yêu thích: $signature")
                if (notify) vibrate()
            }
        } else if (onlyFavs && now >= pausedUntil && now - firstSeenAt >= DWELL_MS) {
            consecutiveSkips++
            if (consecutiveSkips >= MAX_CONSECUTIVE_SKIPS) {
                Log.d(TAG, "Vuốt quá $MAX_CONSECUTIVE_SKIPS video, nghỉ 1 phút")
                pausedUntil = now + PAUSE_MS
                consecutiveSkips = 0
            }
            lastSwipeAt = now
            swipeToNextVideo()
        }
    }

    private fun collectMusicCandidates(
        node: AccessibilityNodeInfo?, depth: Int, out: MutableList<String>
    ) {
        if (node == null || depth > MAX_DEPTH) return

        for (raw in arrayOf(node.text?.toString(), node.contentDescription?.toString())) {
            if (raw.isNullOrBlank() || raw.length !in 3..120) continue
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.top < screenHeight * MUSIC_AREA_FRACTION) continue
            val n = norm(raw)
            if (MUSIC_MARKERS.any { n.contains(it) }) {
                if (DEBUG) Log.d(TAG, "music=$n")
                out.add(n)
            }
        }

        for (i in 0 until node.childCount) {
            collectMusicCandidates(node.getChild(i), depth + 1, out)
        }
    }

    // Bỏ dấu tiếng Việt + viết thường để người dùng gõ không dấu vẫn khớp
    private fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= 26) {
            v?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v?.vibrate(80)
        }
    }

    // ---------- Vuốt ----------

    private fun swipeToNextVideo() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.75f
        val endY = metrics.heightPixels * 0.25f

        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 250)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service đã kết nối, đang theo dõi TikTok ở chế độ nền")
    }

    override fun onInterrupt() {}
}

// Global: com.zhiliaoapp.musically | Việt Nam/châu Á: com.ss.android.ugc.trill
val TIKTOK_PACKAGES = setOf(
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill"
)
