package com.katdmy.birthdayswidget.view

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.widget.doOnTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.katdmy.birthdayswidget.R
import com.katdmy.birthdayswidget.widget.ListWidgetService
import com.katdmy.birthdayswidget.widget.WIDGET_DETAILS_ACTION
import com.katdmy.birthdayswidget.widget.WIDGET_ID

class SettingsActivity : Activity() {

    private lateinit var btnMinus: Button
    private lateinit var btnPlus: Button
    private lateinit var tvFontSize: TextView
    private lateinit var rgBackground: RadioGroup
    private lateinit var rb1: RadioButton
    private lateinit var rb2: RadioButton
    private lateinit var rb3: RadioButton
    private lateinit var swPhoto: SwitchMaterial
    private lateinit var textFieldEventsCount: TextInputLayout
    private lateinit var closeBtn: MaterialButton
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var sp: SharedPreferences
    private lateinit var spEditor: SharedPreferences.Editor
    private lateinit var spCommon: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        spCommon = getSharedPreferences("${applicationContext.packageName}.widgetPreferences", Context.MODE_PRIVATE)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetId = spCommon.getInt(WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        sp = getSharedPreferences("${applicationContext.packageName}.widgetPreferences$appWidgetId", Context.MODE_PRIVATE)
        spEditor = sp.edit()

        initViews()

        setResult(RESULT_CANCELED)
    }

    private fun initViews() {
        tvFontSize = findViewById(R.id.tv_font_size)
        var textSize = sp.getFloat("text_size", 14F)
        tvFontSize.textSize = textSize

        btnMinus = findViewById(R.id.btn_minus)
        btnMinus.setOnClickListener {
            textSize = sp.getFloat("text_size", 14F)
            textSize--
            spEditor.putFloat("text_size", textSize).apply()
            tvFontSize.textSize = textSize
        }

        btnPlus = findViewById(R.id.btn_plus)
        btnPlus.setOnClickListener {
            textSize = sp.getFloat("text_size", 14F)
            textSize++
            spEditor.putFloat("text_size", textSize).apply()
            tvFontSize.textSize = textSize
        }

        rb1 = findViewById(R.id.rb1)
        rb2 = findViewById(R.id.rb2)
        rb3 = findViewById(R.id.rb3)
        rgBackground = findViewById(R.id.rg_background)
        when (sp.getInt("background", 2)) {
            1 -> rb1.isChecked = true
            2 -> rb2.isChecked = true
            3 -> rb3.isChecked = true
        }
        rgBackground.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb1 -> spEditor.putInt("background", 1).apply()
                R.id.rb2 -> spEditor.putInt("background", 2).apply()
                R.id.rb3 -> spEditor.putInt("background", 3).apply()
            }
        }

        swPhoto = findViewById(R.id.sw_use_photo)
        swPhoto.isChecked = sp.getBoolean("use_photo", false)
        swPhoto.setOnCheckedChangeListener { _, isChecked ->
            spEditor.putBoolean("use_photo", isChecked).apply()
        }

        textFieldEventsCount = findViewById(R.id.tf_events_count)
        textFieldEventsCount.editText?.setText(sp.getInt("events_count", 4).toString())
        textFieldEventsCount.editText?.doOnTextChanged { inputText, _, _, _ ->
            val inputInt = inputText.toString().toIntOrNull()
            if (inputInt == null || inputInt <= 0) {
                textFieldEventsCount.error = getString(R.string.events_count_error)
            } else {
                textFieldEventsCount.error = ""
                spEditor.putInt("events_count", inputInt).apply()
            }
        }

        closeBtn = findViewById(R.id.btn_close)
        closeBtn.setOnClickListener {
            val appWidgetManager = AppWidgetManager.getInstance(this)

            val updateIntent = Intent(this@SettingsActivity, ListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            val views = RemoteViews(this.packageName, R.layout.widget_list).apply {
                setRemoteAdapter(R.id.lv_list, updateIntent)
                setEmptyView(R.id.lv_list, R.id.tv_empty)
                val contactDetailsPendingIntent: PendingIntent = Intent(this@SettingsActivity, MainActivity::class.java).run {
                    action = WIDGET_DETAILS_ACTION
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                    PendingIntent.getActivity(this@SettingsActivity, 0, this, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                setPendingIntentTemplate(R.id.lv_list, contactDetailsPendingIntent)

                val settingsPendingIntent: PendingIntent = Intent(this@SettingsActivity, SettingsActivity::class.java).run {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

                    PendingIntent.getActivity(this@SettingsActivity, 0, this, PendingIntent.FLAG_IMMUTABLE)
                }
                setOnClickPendingIntent(R.id.iv_settings, settingsPendingIntent)
                setInt(R.id.iv_settings, "setVisibility", View.VISIBLE)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_list)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

}