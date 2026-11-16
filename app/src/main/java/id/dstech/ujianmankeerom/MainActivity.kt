package id.dstech.ujianmankeerom
// Layar ujian dan kunci layar

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.view.WindowManager
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val HOME_URL = BuildConfig.HOME_URL
    private val PASSWORD = BuildConfig.PASSWORD

    private lateinit var web: WebView
    private lateinit var navBar: LinearLayout
    private lateinit var examScreen: LinearLayout
    private val navButtons = mutableListOf<Button>()
    private var inExam = false
    private var exiting = false
    private lateinit var loginView: View
    private lateinit var navDivider: View
    private lateinit var keyboardBlock: View

    // Setiap detik saat ujian: jika keyboard bukan Simple Keyboard, layar ujian ditutup
    private val keyboardWatch = object : Runnable {
        override fun run() {
            if (inExam) {
                keyboardBlock.visibility = if (isSimpleKeyboardActive()) View.GONE else View.VISIBLE
            }
            window.decorView.postDelayed(this, 1000)
        }
    }

    // Halaman jaringan lemah, warnanya mengikuti tema HP
    private fun weakNetworkHtml(): String {
        val night = isNightMode()
        val bg = if (night) "#000000" else "#ffffff"
        val fg = if (night) "#E5E7EB" else "#374151"
        val sub = if (night) "#9CA3AF" else "#6B7280"
        return """
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;font-family:sans-serif;background:$bg;color:$fg;text-align:center">
          <div style="font-size:48px">&#128246;</div>
          <div style="font-size:18px;font-weight:600;margin-top:12px">Jaringan lemah</div>
          <div style="font-size:14px;color:$sub;margin-top:6px">Menghubungkan kembali...</div>
        </body></html>
        """.trimIndent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                // Jaringan putus: tampilkan info jaringan lemah lalu coba lagi otomatis
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true && view != null) {
                        view.loadDataWithBaseURL(null, weakNetworkHtml(), "text/html", "utf-8", null)
                        view.postDelayed({ view.loadUrl(HOME_URL) }, 3000)
                    }
                }
            }
            setBackgroundColor(Color.WHITE)
        }

        navBar = buildNavbar()
        navDivider = View(this)

        examScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(navBar)
            addView(navDivider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
            addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        loginView = buildLoginScreen()
        keyboardBlock = buildKeyboardBlock()
        applyThemeColors()

        val full = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        setContentView(FrameLayout(this).apply {
            addView(loginView, full)
            addView(examScreen, full)
            addView(keyboardBlock, full)
        })
        window.decorView.postDelayed(keyboardWatch, 1000)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (inExam && web.canGoBack()) {
                    web.goBack()
                } else {
                    exitWithPassword()
                }
            }
        })

    }

    // ---------- Halaman login ----------

    private fun buildLoginScreen(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackgroundColor(c("#F3F4F6", "#000000"))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        }

        val heading = TextView(this).apply {
            text = BuildConfig.LOGIN_TITLE
            setTextColor(c("#111827", "#F9FAFB"))
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val sub = TextView(this).apply {
            text = "Masukkan password untuk memulai ujian"
            setTextColor(c("#6B7280", "#9CA3AF"))
            textSize = 14f
            gravity = Gravity.CENTER
        }

        val input = EditText(this).apply {
            hint = "Password"
            setTextColor(c("#111111", "#F9FAFB"))
            setHintTextColor(c("#6B7280", "#9CA3AF"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(c("#FFFFFF", "#1F2937"))
                setStroke(dp(1), c("#9CA3AF", "#6B7280"))
            }
        }

        val error = TextView(this).apply {
            text = "Password salah"
            setTextColor(Color.parseColor("#DC2626"))
            textSize = 13f
            gravity = Gravity.CENTER
            visibility = View.INVISIBLE
        }

        // Muncul hanya jika keyboard belum Simple Keyboard: ganti keyboard, pengaturan, dan link unduh
        val keyboardHelp = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        // Ganti keyboard dan ubah keyboard bersebelahan
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(outlineButton("Ganti keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(0, 0, dp(4), 0)
        })
        row.addView(outlineButton("Ubah keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(4), 0, 0, 0)
        })
        keyboardHelp.addView(row, fullWidth(0))
        val notYet = TextView(this).apply {
            text = "Belum punya Simple Keyboard?"
            setTextColor(c("#6B7280", "#9CA3AF"))
            textSize = 13f
            gravity = Gravity.CENTER
        }
        keyboardHelp.addView(notYet, fullWidth(12))
        keyboardHelp.addView(outlineButton("Unduh Simple Keyboard (Play Store)") {
            openLink("https://play.google.com/store/apps/details?id=rkr.simplekeyboard.inputmethod")
        }, fullWidth(8))
        keyboardHelp.addView(outlineButton("Unduh Simple Keyboard (F-Droid)") {
            openLink("https://f-droid.org/packages/rkr.simplekeyboard.inputmethod/")
        }, fullWidth(8))

        val loginButton = Button(this).apply {
            text = "Masuk"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(c("#0D9488", "#2563EB"))
            }
            setOnClickListener {
                // Hanya boleh masuk jika keyboard yang aktif adalah Simple Keyboard
                if (!isSimpleKeyboardActive()) {
                    error.text = "Keyboard yang dipakai bukan Simple Keyboard. Pilih Simple Keyboard untuk masuk."
                    error.visibility = View.VISIBLE
                    keyboardHelp.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                keyboardHelp.visibility = View.GONE
                if (input.text.toString() == PASSWORD) {
                    onLoginSuccess()
                } else {
                    error.text = "Password salah"
                    error.visibility = View.VISIBLE
                    input.text.clear()
                }
            }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(c("#FFFFFF", "#111827"))
                setStroke(dp(1), c("#D1D5DB", "#374151"))
            }
            elevation = dp(8).toFloat()
        }

        card.addView(logo, LinearLayout.LayoutParams(dp(96), dp(96)))
        card.addView(heading, fullWidth(8))
        card.addView(sub, fullWidth(6))
        card.addView(input, fullWidth(32))
        card.addView(error, fullWidth(8))
        card.addView(loginButton, fullWidth(12))
        card.addView(keyboardHelp, fullWidth(10))

        col.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        return col
    }

    // Tombol bergaya garis tepi, dipakai di bagian bantuan keyboard
    private fun outlineButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(c("#0D9488", "#2563EB"))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(c("#FFFFFF", "#111827"))
                setStroke(dp(1), c("#0D9488", "#2563EB"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun openLink(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    // Cek keyboard yang sedang dipakai: harus rkr.simplekeyboard.inputmethod
    private fun isSimpleKeyboardActive(): Boolean {
        val current = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false
        return current.startsWith("rkr.simplekeyboard.inputmethod/")
    }

    private fun fullWidth(topDp: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(topDp) }
    }

    // ---------- Pin layar dan pembukaan ujian ----------

    private fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(this)

    private fun onLoginSuccess() {
        if (!canDrawOverlay()) {
            Toast.makeText(
                this,
                "Aktifkan izin Tampil di atas aplikasi lain untuk Ujian MAN, lalu coba lagi",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
            return
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(
                this,
                "Aktifkan izin Jangan Ganggu untuk Ujian MAN, lalu coba lagi",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        try {
            startLockTask()
        } catch (e: Exception) {
            finish()
            return
        }
        checkPinThenOpen(0)
    }

    private fun checkPinThenOpen(attempt: Int) {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (isPinned()) {
            openExam()
        } else if (attempt < 30) {
            // Dialog pin masih terbuka, cek lagi tiap detik
            window.decorView.postDelayed({ checkPinThenOpen(attempt + 1) }, 1000)
        } else {
            // Pin ditolak atau dibatalkan, aplikasi tidak boleh masuk
            Toast.makeText(this, "Pin harus diizinkan untuk masuk", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun openExam() {
        inExam = true
        loginView.visibility = View.GONE
        examScreen.visibility = View.VISIBLE
        keyboardBlock.visibility = if (isSimpleKeyboardActive()) View.GONE else View.VISIBLE
        web.loadUrl(HOME_URL)
    }

    // Layar penghalang saat keyboard bukan Simple Keyboard di tengah ujian
    private fun buildKeyboardBlock(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackgroundColor(c("#F3F4F6", "#000000"))
            isClickable = true
            visibility = View.GONE
        }

        val title = TextView(this).apply {
            text = "Keyboard tidak sesuai"
            setTextColor(c("#111827", "#F9FAFB"))
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val info = TextView(this).apply {
            text = "Ujian dijeda. Pakai keyboard Simple Keyboard untuk melanjutkan."
            setTextColor(c("#6B7280", "#9CA3AF"))
            textSize = 15f
            gravity = Gravity.CENTER
        }

        val pick = Button(this).apply {
            text = "Ganti keyboard"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(c("#0D9488", "#2563EB"))
            }
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }

        col.addView(title, fullWidth(0))
        col.addView(info, fullWidth(12))
        col.addView(pick, fullWidth(24))
        return col
    }

    private fun isNightMode(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    // Navbar mengikuti tema HP: terang -> putih, gelap -> hitam pekat
    private fun applyThemeColors() {
        if (isNightMode()) {
            setNavColor(Color.BLACK, Color.WHITE, Color.parseColor("#374151"), Color.parseColor("#1F2937"))
        } else {
            setNavColor(Color.WHITE, Color.parseColor("#111111"), Color.parseColor("#9CA3AF"), Color.parseColor("#E5E7EB"))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeColors()
    }

    private fun exitWithPassword() {
        askPassword("Masukkan password untuk keluar") {
            try { stopLockTask() } catch (e: Exception) {}
            alarmOnceThenFinish()
        }
    }

    // ---------- Navbar ----------

    private var alarmPlayer: MediaPlayer? = null
    private var alarmPrevFilter: Int? = null
    private var blockView: View? = null
    private val NOTIF_ID = 1001
    private val CHANNEL_ID = "ujian_keluar"

    // Keluar lewat password: alarm berbunyi 1 kali penuh, lalu aplikasi ditutup
    private fun alarmOnceThenFinish() {
        if (!inExam) {
            finish()
            return
        }
        if (exiting) return
        exiting = true
        stopAlarm()
        startAlarm(loop = false) { finish() }
    }

    // Keluar paksa (Home atau Recent): layar penutup, notifikasi, dan alarm berulang tanpa henti
    private fun alarmForever() {
        if (!inExam) return
        showBlockOverlay()
        showExamNotification()
        if (alarmPlayer == null) startAlarm(loop = true, onDone = null)
    }

    // Layar penutup setelah keluar paksa: menutupi aplikasi, status bar, notifikasi, dan Control Center tetap bisa dipakai
    private fun showBlockOverlay() {
        if (blockView != null || !canDrawOverlay()) return
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackgroundColor(c("#F3F4F6", "#000000"))
            isClickable = true
        }
        col.addView(TextView(this).apply {
            text = "Ujian dihentikan"
            setTextColor(c("#111827", "#F9FAFB"))
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, fullWidth(0))
        col.addView(TextView(this).apply {
            text = "Ketuk notifikasi ${BuildConfig.APP_NAME} untuk kembali ke ujian."
            setTextColor(c("#6B7280", "#9CA3AF"))
            textSize = 15f
            gravity = Gravity.CENTER
        }, fullWidth(12))
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(col, lp)
            blockView = col
        } catch (e: Exception) {}
    }

    private fun hideBlockOverlay() {
        val v = blockView ?: return
        blockView = null
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v) } catch (e: Exception) {}
    }

    // Notifikasi yang tidak bisa digeser hilang: ketuk untuk membuka kembali aplikasi ujian
    private fun showExamNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, BuildConfig.APP_NAME, NotificationManager.IMPORTANCE_HIGH)
        )
        val open = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("${BuildConfig.APP_NAME} dihentikan")
            .setContentText("Ketuk untuk kembali ke ujian")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun hideExamNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
    }

    private fun stopAlarm() {
        try { alarmPlayer?.stop() } catch (e: Exception) {}
        try { alarmPlayer?.release() } catch (e: Exception) {}
        alarmPlayer = null
        restoreDnd()
    }

    private fun restoreDnd() {
        val filter = alarmPrevFilter ?: return
        alarmPrevFilter = null
        try {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).setInterruptionFilter(filter)
        } catch (e: Exception) {}
    }

    private fun startAlarm(loop: Boolean, onDone: (() -> Unit)?) {
        try {
            // Volume alarm dinaikkan ke maksimum
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

            // Matikan Jangan Ganggu sementara (jika izin sudah diberikan)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted && alarmPrevFilter == null) {
                alarmPrevFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }

            // Suara alarm dari file res/raw/alarm.* (bisa diganti sendiri)
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(this, Uri.parse("android.resource://$packageName/${R.raw.alarm}"))
            player.isLooping = loop
            player.setVolume(1f, 1f)
            player.prepare()
            alarmPlayer = player
            if (!loop) {
                player.setOnCompletionListener {
                    stopAlarm()
                    onDone?.invoke()
                }
            }
            player.start()

            // Volume dikembalikan ke maksimum selama alarm masih berbunyi
            val enforcer = object : Runnable {
                override fun run() {
                    if (alarmPlayer == null) return
                    if (am.getStreamVolume(AudioManager.STREAM_ALARM) < maxVol) {
                        am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                    }
                    window.decorView.postDelayed(this, 500)
                }
            }
            window.decorView.post(enforcer)
        } catch (e: Exception) {
            // Gagal memutar alarm: tetap lanjut
            stopAlarm()
            onDone?.invoke()
        }
    }

    private fun isPinned(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    // Tombol Home atau Recent ditekan saat tidak terpin: layar penutup dan alarm
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isPinned()) alarmForever()
    }

    // Aplikasi dibuka lagi: layar penutup dan notifikasi dihilangkan, alarm tetap berbunyi sampai keluar dengan password
    override fun onResume() {
        super.onResume()
        hideBlockOverlay()
        hideExamNotification()
        if (inExam && !isPinned() && alarmPlayer == null) alarmForever()
    }

    override fun onDestroy() {
        hideBlockOverlay()
        hideExamNotification()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // Pilih warna terang atau gelap sesuai tema HP
    private fun c(light: String, dark: String): Int = Color.parseColor(if (isNightMode()) dark else light)

    private fun buildNavbar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        bar.addView(navButton("◀") { if (web.canGoBack()) web.goBack() }, navLp())
        bar.addView(navButton("▶") { if (web.canGoForward()) web.goForward() }, navLp())
        bar.addView(navButton("⟳") { web.reload() }, navLp())
        bar.addView(navButton("⌂") { web.loadUrl(HOME_URL) }, navLp())
        bar.addView(navButton("✕") { exitWithPassword() }, navLp())

        return bar
    }

    private fun navLp(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            setMargins(dp(3), 0, dp(3), 0)
        }
    }

    private fun navButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 18f
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener { onClick() }
            navButtons.add(this)
        }
    }

    private fun setNavColor(bg: Int, fg: Int, line: Int, btn: Int) {
        navBar.setBackgroundColor(bg)
        navDivider.setBackgroundColor(line)
        navButtons.forEach { b ->
            b.setTextColor(fg)
            b.background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(btn)
            }
        }
    }

    // ---------- Dialog password (untuk keluar) ----------

    private fun askPassword(message: String, onOk: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(BuildConfig.APP_NAME)
            .setMessage(message)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.toString() == PASSWORD) {
                    dialog.dismiss()
                    onOk()
                } else {
                    Toast.makeText(this, "Password salah", Toast.LENGTH_SHORT).show()
                    input.text.clear()
                }
            }
        }
        dialog.show()
    }
}

