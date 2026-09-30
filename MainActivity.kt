package com.example.tiktokadskip

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(64, 96, 64, 64)
        }

        fun label(t: String) = TextView(this).apply {
            text = t
            textSize = 16f
            setPadding(0, 32, 0, 8)
        }

        fun addSwitch(label: String, key: String, default: Boolean) {
            layout.addView(Switch(this).apply {
                text = label
                textSize = 16f
                isChecked = prefs.getBoolean(key, default)
                setPadding(0, 24, 0, 24)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(key, checked).apply()
                }
            })
        }

        layout.addView(label("Bật \"TikTok Ad Skipper\" trong Cài đặt > Trợ năng, rồi chọn:"))
        addSwitch("Bỏ qua quảng cáo (Sponsored)", "skip_ads", true)
        addSwitch("Bỏ qua livestream", "skip_live", true)
        addSwitch("Bỏ qua quảng cáo game", "skip_game", true)

        layout.addView(label("Nhạc yêu thích (ngăn cách bằng dấu phẩy, gõ không dấu cũng được):"))
        layout.addView(EditText(this).apply {
            hint = "vd: em của ngày hôm qua, sơn tùng, lofi"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(prefs.getString("fav_music", ""))
            doAfterTextChanged { prefs.edit().putString("fav_music", it.toString()).apply() }
        })
        addSwitch("Rung khi gặp video có nhạc yêu thích", "music_notify", false)
        addSwitch("Chỉ xem nhạc yêu thích (tự vuốt qua video khác)", "music_only", false)

        layout.addView(Button(this).apply {
            text = "Mở cài đặt Trợ năng"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })
        layout.addView(Button(this).apply {
            text = "Không tối ưu pin cho app (giữ chạy nền)"
            setOnClickListener { requestIgnoreBatteryOptimization() }
        })

        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun requestIgnoreBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
        )
    }
}
