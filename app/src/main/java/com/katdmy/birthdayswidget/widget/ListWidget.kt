package com.katdmy.birthdayswidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.katdmy.birthdayswidget.R
import com.katdmy.birthdayswidget.view.MainActivity
import com.katdmy.birthdayswidget.view.SettingsActivity
import java.time.LocalDateTime

const val WIDGET_DETAILS_ACTION = "com.katdmy.birthdayswidget.WIDGET_DETAILS_ACTION"
const val EXTRA_CONTACT_ID = "com.katdmy.birthdayswidget.EXTRA_CONTACT_ID"
const val EXTRA_LOOKUP_KEY = "com.katdmy.birthdayswidget.EXTRA_LOOKUP_KEY"
const val WIDGET_ID = "com.katdmy.birthdayswidget.WIDGET_ID"

/**
 * Implementation of App Widget functionality.
 */
class ListWidget : AppWidgetProvider() {

    private var spCommonEditor: SharedPreferences.Editor? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (spCommonEditor == null)
            spCommonEditor = context.getSharedPreferences(
                "${context.applicationContext.packageName}.widgetPreferences",
                Context.MODE_PRIVATE
            ).edit()

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            spCommonEditor!!.putInt(WIDGET_ID, appWidgetId).apply()
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray) {
        spCommonEditor?.clear()?.apply()
        super.onDeleted(context, appWidgetIds)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val updateIntent = Intent(context, ListWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra("uniqueData", System.currentTimeMillis())
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }

    val views = RemoteViews(context.packageName, R.layout.widget_list).apply {
        setRemoteAdapter(R.id.lv_list, updateIntent)
        setEmptyView(R.id.lv_list, R.id.tv_empty)

        val contactDetailsPendingIntent: PendingIntent = Intent(context, MainActivity::class.java).run {
            action = WIDGET_DETAILS_ACTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

            PendingIntent.getActivity(context, 0, this, PendingIntent.FLAG_MUTABLE)
        }

        setPendingIntentTemplate(R.id.lv_list, contactDetailsPendingIntent)

        setInt(R.id.iv_settings, "setVisibility", View.GONE)
    }
    appWidgetManager.updateAppWidget(appWidgetId, views)
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_list)
}