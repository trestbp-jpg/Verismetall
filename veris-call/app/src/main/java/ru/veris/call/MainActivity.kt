package ru.veris.call

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("veris_profile", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var emergencyButton: TextView? = null
    private var emergencyGlow: View? = null
    private var callInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (!prefs.contains("device_id")) prefs.edit().putString("device_id", UUID.randomUUID().toString()).apply()
        buildScreen()
        if (!profileComplete()) handler.postDelayed({ editProfile(true) }, 350)
    }

    private fun profileComplete() =
        !prefs.getString("name", "").isNullOrBlank() && !prefs.getString("phone", "").isNullOrBlank()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildScreen() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(18,24,29)) }

        val bg = ImageView(this).apply {
            setImageResource(R.drawable.screen_bg)
            scaleType = ImageView.ScaleType.FIT_XY
        }
        root.addView(bg, FrameLayout.LayoutParams(-1, -1))

        val profileHit = View(this).apply {
            contentDescription = "Профиль"
            isClickable = true
            setOnClickListener { showProfile() }
        }
        root.addView(profileHit, FrameLayout.LayoutParams(dp(120), dp(100), Gravity.TOP or Gravity.START))

        val infoHit = View(this).apply {
            contentDescription = "О сервисе"
            isClickable = true
            setOnClickListener { showServiceInfo() }
        }
        root.addView(infoHit, FrameLayout.LayoutParams(dp(120), dp(100), Gravity.TOP or Gravity.END))

        val emergency = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        root.addView(emergency, FrameLayout.LayoutParams(dp(280), dp(280)))

        val mask = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(30,31,32))
                setStroke(dp(4), Color.rgb(90,93,94))
            }
        }
        emergency.addView(mask, FrameLayout.LayoutParams(dp(238), dp(238), Gravity.CENTER))

        val glow = View(this).apply {
            alpha = 0f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = dp(140).toFloat()
                colors = intArrayOf(
                    Color.argb(235,255,30,15),
                    Color.argb(130,255,35,15),
                    Color.argb(0,255,0,0)
                )
            }
        }
        emergency.addView(glow, FrameLayout.LayoutParams(dp(280), dp(280), Gravity.CENTER))

        val ring = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(185,188,188), Color.rgb(75,78,80), Color.rgb(30,31,32))).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(3), Color.rgb(215,215,210))
            }
        }
        emergency.addView(ring, FrameLayout.LayoutParams(dp(226), dp(226), Gravity.CENTER))

        val stem = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(150,0,0), Color.rgb(85,0,0))).apply {
                shape = GradientDrawable.OVAL
            }
        }
        emergency.addView(stem, FrameLayout.LayoutParams(dp(180), dp(180), Gravity.CENTER))

        val button = TextView(this).apply {
            text = "ВЫЗВАТЬ\nВЕРИС"
            gravity = Gravity.CENTER
            textSize = 25f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            elevation = dp(16).toFloat()
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(242,58,48), Color.rgb(202,12,8), Color.rgb(120,0,0))).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(3), Color.rgb(100,0,0))
            }
            setOnClickListener { pressEmergency() }
        }
        emergency.addView(button, FrameLayout.LayoutParams(dp(205), dp(180), Gravity.CENTER))
        emergencyButton = button
        emergencyGlow = glow

        setContentView(root)

        root.post {
            val size = (root.width * 0.62f).toInt().coerceAtMost(dp(330))
            val lp = emergency.layoutParams as FrameLayout.LayoutParams
            lp.width = size
            lp.height = size
            lp.leftMargin = (root.width - size) / 2
            lp.topMargin = (root.height * 0.39f).toInt()
            emergency.layoutParams = lp

            val childScale = size / dp(280).toFloat()
            for (i in 0 until emergency.childCount) {
                emergency.getChildAt(i).scaleX = childScale
                emergency.getChildAt(i).scaleY = childScale
            }
        }
    }

    private fun pressEmergency() {
        val button = emergencyButton ?: return
        val glow = emergencyGlow ?: return
        if (callInProgress || !button.isEnabled) return
        if (!profileComplete()) { editProfile(true); return }

        callInProgress = true
        button.isEnabled = false
        button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        button.animate().cancel()
        glow.animate().cancel()
        glow.alpha = 0f
        glow.scaleX = .78f
        glow.scaleY = .78f

        button.animate()
            .translationY(dp(15).toFloat())
            .scaleX(.96f).scaleY(.96f)
            .setDuration(150).start()

        glow.animate()
            .alpha(1f)
            .scaleX(1.08f).scaleY(1.08f)
            .setDuration(220).start()

        handler.postDelayed({ if (callInProgress) showLocationQuestion() }, 2000)
    }

    private fun showLocationQuestion() {
        val d = AlertDialog.Builder(this)
            .setTitle("Вызвать Верис")
            .setMessage("Передать данные геолокации (вашего местоположения)?")
            .setPositiveButton("Да", null)
            .setNegativeButton("Нет", null)
            .setNeutralButton("Отменить вызов", null)
            .setCancelable(false)
            .create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                d.dismiss()
                keepLitForFiveSeconds()
                requestLocation()
            }
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                d.dismiss()
                keepLitForFiveSeconds()
                send(null)
            }
            d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                d.dismiss()
                cancelCall()
            }
        }
        d.show()
    }

    private fun keepLitForFiveSeconds() {
        handler.postDelayed({ resetEmergencyVisual() }, 5000)
    }

    private fun cancelCall() {
        resetEmergencyVisual()
        Toast.makeText(this, "Вызов отменён", Toast.LENGTH_SHORT).show()
    }

    private fun resetEmergencyVisual() {
        val button = emergencyButton ?: return
        val glow = emergencyGlow
        glow?.animate()?.cancel()
        button.animate().cancel()

        glow?.animate()?.alpha(0f)?.scaleX(.85f)?.scaleY(.85f)?.setDuration(160)?.start()
        button.animate()
            .translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(180)
            .withEndAction {
                button.isEnabled = true
                callInProgress = false
            }.start()
    }

    private fun showProfile() {
        val company = prefs.getString("company", "").orEmpty()
        val name = prefs.getString("name", "").orEmpty()
        val phone = prefs.getString("phone", "").orEmpty()
        val equipment = prefs.getString("equipment", "").orEmpty()
        val number = prefs.getString("machine_number", "").orEmpty()

        val msg = buildString {
            append("Имя: ").append(if (name.isBlank()) "—" else name).append("\n")
            append("Телефон: ").append(if (phone.isBlank()) "—" else phone).append("\n")
            append("Организация: ").append(if (company.isBlank()) "—" else company).append("\n")
            append("Техника: ").append(if (equipment.isBlank()) "—" else equipment).append("\n")
            append("Госномер / инв. №: ").append(if (number.isBlank()) "—" else number)
        }

        AlertDialog.Builder(this)
            .setTitle("Мой профиль")
            .setMessage(msg)
            .setPositiveButton("Изменить") { _, _ -> editProfile(false) }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showServiceInfo() {
        val info = "ГК «Верис»\n\n" +
            "Ремонт и обслуживание грузоподъёмных кранов и спецтехники. " +
            "Гидравлика, электрооборудование, приборы безопасности, металлоконструкции и металлообработка.\n\n" +
            "Зона работы: Калининградская область.\n\n" +
            "Тел.: +7 (4012) 507-937\n" +
            "Тел.: +7 963 350-79-37\n" +
            "E-mail: info@veris39.ru\n" +
            "Сайт: veris39.ru"

        AlertDialog.Builder(this)
            .setTitle("О сервисе")
            .setMessage(info)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun editProfile(first: Boolean) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
        }
        fun field(label: String, key: String) = EditText(this).apply {
            hint = label
            setSingleLine(true)
            setText(prefs.getString(key, ""))
            box.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        val company = field("Организация (необязательно)", "company")
        val name = field("Ваше имя *", "name")
        val phone = field("Телефон *", "phone")
        val equipment = field("Техника / модель", "equipment")
        val number = field("Госномер / инв. №", "machine_number")

        val d = AlertDialog.Builder(this)
            .setTitle("Карточка клиента")
            .setMessage("Данные хранятся на этом телефоне и передаются в Верис только при вызове.")
            .setView(box)
            .setPositiveButton("Сохранить", null)
            .apply { if (!first) setNegativeButton("Отмена", null) }
            .create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (name.text.toString().trim().isEmpty() || phone.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Укажите имя и телефон", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                prefs.edit()
                    .putString("company", company.text.toString().trim())
                    .putString("name", name.text.toString().trim())
                    .putString("phone", phone.text.toString().trim())
                    .putString("equipment", equipment.text.toString().trim())
                    .putString("machine_number", number.text.toString().trim())
                    .apply()
                d.dismiss()
            }
        }
        d.setCancelable(!first)
        d.show()
    }

    private fun requestLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 41)
        } else locate()
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(code, p, g)
        if (code == 41) {
            if (g.any { it == PackageManager.PERMISSION_GRANTED }) locate()
            else AlertDialog.Builder(this)
                .setTitle("Геолокация не разрешена")
                .setMessage("Отправить вызов без координат?")
                .setPositiveButton("Да") { _, _ -> send(null) }
                .setNegativeButton("Отменить вызов") { _, _ -> cancelCall() }
                .show()
        }
    }

    @Suppress("MissingPermission")
    private fun locate() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) { locationUnavailable("На телефоне выключено определение местоположения."); return }

        var done = false
        lateinit var listener: LocationListener
        listener = LocationListener { loc ->
            if (!done) {
                done = true
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
                send(loc)
            }
        }
        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())

        handler.postDelayed({
            if (!done) {
                done = true
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
                val last = try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null }
                if (last != null) send(last) else locationUnavailable("Не удалось получить координаты.")
            }
        }, 9000)
    }

    private fun locationUnavailable(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Координаты недоступны")
            .setMessage("$msg Отправить вызов без них?")
            .setPositiveButton("Да") { _, _ -> send(null) }
            .setNegativeButton("Отменить вызов") { _, _ -> cancelCall() }
            .show()
    }

    private fun send(loc: Location?) {
        Thread {
            val result = post(loc)
            runOnUiThread {
                if (!result.first) {
                    resetEmergencyVisual()
                    AlertDialog.Builder(this)
                        .setTitle("Не удалось отправить вызов")
                        .setMessage("Проверьте интернет и попробуйте ещё раз.")
                        .setPositiveButton("Повторить") { _, _ -> pressEmergency() }
                        .setNegativeButton("Закрыть", null)
                        .show()
                }
            }
        }.start()
    }

    private fun post(loc: Location?): Pair<Boolean, String> = try {
        val j = JSONObject().apply {
            put("app_token", AppConfig.APP_TOKEN)
            put("device_id", prefs.getString("device_id", ""))
            put("company", prefs.getString("company", ""))
            put("name", prefs.getString("name", ""))
            put("phone", prefs.getString("phone", ""))
            put("equipment", prefs.getString("equipment", ""))
            put("machine_number", prefs.getString("machine_number", ""))
            put("location_shared", loc != null)
            if (loc != null) {
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
                put("accuracy_m", loc.accuracy.toDouble())
            }
            put("client_time", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()))
        }
        val c = (URL(AppConfig.ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        c.outputStream.use { it.write(j.toString().toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
        Pair(code in 200..299, body.take(250))
    } catch (e: Exception) {
        Pair(false, e.message ?: "Ошибка соединения")
    }
}
