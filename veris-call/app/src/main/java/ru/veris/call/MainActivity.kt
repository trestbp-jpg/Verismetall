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
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("veris_profile", Context.MODE_PRIVATE) }
    private val dp get() = resources.displayMetrics.density
    private fun px(v:Int)= (v*dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prefs.contains("device_id")) prefs.edit().putString("device_id", UUID.randomUUID().toString()).apply()
        showMain()
        if (!complete()) Handler(Looper.getMainLooper()).postDelayed({ editProfile(true) }, 250)
    }

    private fun complete() = !prefs.getString("name","").isNullOrBlank() && !prefs.getString("phone","").isNullOrBlank()

    private fun showMain() {
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER_HORIZONTAL
            setPadding(px(24),px(34),px(24),px(24))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply {
            text="ВЕРИС"; textSize=36f; gravity=Gravity.CENTER
            setTypeface(Typeface.DEFAULT,Typeface.BOLD); setTextColor(Color.BLACK)
        }, LinearLayout.LayoutParams(-1,-2))
        root.addView(TextView(this).apply {
            text="Помощь спецтехнике"; textSize=16f; gravity=Gravity.CENTER; setTextColor(Color.DKGRAY)
        }, LinearLayout.LayoutParams(-1,-2).apply{topMargin=px(4)})
        val b=Button(this).apply {
            text="ВЫЗВАТЬ\nВЕРИС"; textSize=28f; gravity=Gravity.CENTER; isAllCaps=false
            setTextColor(Color.WHITE); setTypeface(Typeface.DEFAULT,Typeface.BOLD)
            background=GradientDrawable().apply{shape=GradientDrawable.OVAL; setColor(Color.rgb(196,0,0))}
            setOnClickListener { chooseLocation() }
        }
        root.addView(b, LinearLayout.LayoutParams(px(260),px(260)).apply{topMargin=px(44)})
        root.addView(TextView(this).apply {
            text="При поломке нажмите кнопку.\nКарточка и, с вашего разрешения, координаты будут отправлены в Верис."
            textSize=15f; gravity=Gravity.CENTER; setTextColor(Color.GRAY)
        }, LinearLayout.LayoutParams(-1,-2).apply{topMargin=px(26)})
        root.addView(Button(this).apply{
            text="Изменить карточку"; isAllCaps=false; setOnClickListener{editProfile(false)}
        }, LinearLayout.LayoutParams(-2,-2).apply{topMargin=px(18)})
        setContentView(root)
    }

    private fun editProfile(first:Boolean){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(px(18),0,px(18),0)}
        fun field(label:String,key:String)=EditText(this).apply{
            hint=label; setSingleLine(true); setText(prefs.getString(key,"")); box.addView(this,LinearLayout.LayoutParams(-1,-2))
        }
        val company=field("Организация (необязательно)","company")
        val name=field("Ваше имя *","name")
        val phone=field("Телефон *","phone")
        val equipment=field("Техника / модель","equipment")
        val number=field("Госномер / инв. №","machine_number")
        val d=AlertDialog.Builder(this)
            .setTitle("Карточка клиента")
            .setMessage("Данные хранятся на этом телефоне и передаются в Верис только при вызове.")
            .setView(box).setPositiveButton("Сохранить",null)
            .apply{if(!first)setNegativeButton("Отмена",null)}.create()
        d.setOnShowListener{
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                if(name.text.toString().trim().isEmpty()||phone.text.toString().trim().isEmpty()){
                    Toast.makeText(this,"Укажите имя и телефон",Toast.LENGTH_SHORT).show(); return@setOnClickListener
                }
                prefs.edit()
                    .putString("company",company.text.toString().trim())
                    .putString("name",name.text.toString().trim())
                    .putString("phone",phone.text.toString().trim())
                    .putString("equipment",equipment.text.toString().trim())
                    .putString("machine_number",number.text.toString().trim()).apply()
                d.dismiss()
            }
        }
        d.setCancelable(!first); d.show()
    }

    private fun chooseLocation(){
        if(!complete()){editProfile(true);return}
        AlertDialog.Builder(this).setTitle("Вызвать Верис")
            .setMessage("Передать местоположение техники?")
            .setPositiveButton("Да, передать"){_,_-> requestLocation()}
            .setNegativeButton("Нет"){_,_-> send(null)}
            .setNeutralButton("Отмена",null).show()
    }

    private fun requestLocation(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),41)
        } else locate()
    }

    override fun onRequestPermissionsResult(code:Int,p:Array<out String>,g:IntArray){
        super.onRequestPermissionsResult(code,p,g)
        if(code==41){
            if(g.any{it==PackageManager.PERMISSION_GRANTED}) locate()
            else AlertDialog.Builder(this).setTitle("Геолокация не разрешена")
                .setMessage("Отправить вызов без координат?")
                .setPositiveButton("Отправить"){_,_->send(null)}.setNegativeButton("Отмена",null).show()
        }
    }

    @Suppress("MissingPermission")
    private fun locate(){
        val lm=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider=when{
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)->LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)->LocationManager.NETWORK_PROVIDER
            else->null
        }
        if(provider==null){sendWithout("На телефоне выключено определение местоположения.");return}
        val wait=AlertDialog.Builder(this).setTitle("Определяем координаты")
            .setMessage("Подождите несколько секунд…").setCancelable(false).create(); wait.show()
        var done=false
        lateinit var listener:LocationListener
        listener=LocationListener{loc->
            if(!done){done=true; try{lm.removeUpdates(listener)}catch(_:Exception){}; wait.dismiss(); send(loc)}
        }
        lm.requestSingleUpdate(provider,listener,Looper.getMainLooper())
        Handler(Looper.getMainLooper()).postDelayed({
            if(!done){
                done=true; try{lm.removeUpdates(listener)}catch(_:Exception){}
                wait.dismiss()
                val last=try{lm.getLastKnownLocation(provider)}catch(_:Exception){null}
                if(last!=null) send(last) else sendWithout("Не удалось получить координаты.")
            }
        },9000)
    }

    private fun sendWithout(msg:String){
        AlertDialog.Builder(this).setTitle("Координаты недоступны").setMessage("$msg Отправить вызов без них?")
            .setPositiveButton("Отправить"){_,_->send(null)}.setNegativeButton("Отмена",null).show()
    }

    private fun send(loc:Location?){
        val wait=AlertDialog.Builder(this).setTitle("Отправляем вызов").setMessage("Связываемся с Верис…").setCancelable(false).create(); wait.show()
        Thread{
            val result=post(loc)
            runOnUiThread{
                wait.dismiss()
                if(result.first) AlertDialog.Builder(this).setTitle("Вызов отправлен")
                    .setMessage("Верис получил вашу заявку. Ожидайте звонка.").setPositiveButton("Хорошо",null).show()
                else AlertDialog.Builder(this).setTitle("Не удалось отправить")
                    .setMessage("Проверьте интернет и повторите попытку.\n\n${result.second}")
                    .setPositiveButton("Повторить"){_,_->send(loc)}.setNegativeButton("Закрыть",null).show()
            }
        }.start()
    }

    private fun post(loc:Location?):Pair<Boolean,String> = try{
        val j=JSONObject().apply{
            put("app_token",AppConfig.APP_TOKEN); put("device_id",prefs.getString("device_id",""))
            put("company",prefs.getString("company","")); put("name",prefs.getString("name",""))
            put("phone",prefs.getString("phone","")); put("equipment",prefs.getString("equipment",""))
            put("machine_number",prefs.getString("machine_number","")); put("location_shared",loc!=null)
            if(loc!=null){put("latitude",loc.latitude);put("longitude",loc.longitude);put("accuracy_m",loc.accuracy.toDouble())}
            put("client_time",SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(Date()))
        }
        val c=(URL(AppConfig.ENDPOINT).openConnection() as HttpURLConnection).apply{
            requestMethod="POST"; connectTimeout=10000; readTimeout=10000; doOutput=true
            setRequestProperty("Content-Type","application/json; charset=utf-8")
        }
        c.outputStream.use{it.write(j.toString().toByteArray(Charsets.UTF_8))}
        val code=c.responseCode
        val body=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()} ?: "HTTP $code"
        Pair(code in 200..299,body.take(250))
    }catch(e:Exception){Pair(false,e.message?:"Ошибка соединения")}
}
