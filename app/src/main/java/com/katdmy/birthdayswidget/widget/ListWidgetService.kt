package com.katdmy.birthdayswidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.katdmy.birthdayswidget.EventsHelper
import com.katdmy.birthdayswidget.R
import com.katdmy.birthdayswidget.data.Contact
import com.katdmy.birthdayswidget.data.ContactPhotos
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ListWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ListRemoteViewsFactory(this.applicationContext, intent)
    }
}

class ListRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private lateinit var arrayData: ArrayList<Contact>
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var eventsCount: Int = 4
    private var textSize: Float = 14F
    private var background: Int = 2
    private var usePhoto: Boolean = false

    override fun onCreate() {
        val tempData = EventsHelper.getContactList(context)
        val count = if (tempData.size > eventsCount) eventsCount else tempData.size
        arrayData = ArrayList(tempData.sortedBy { it.nextDate }.subList(0, count))
    }

    override fun onDataSetChanged() {
        val sp = context.getSharedPreferences(
            "${context.applicationContext.packageName}.widgetPreferences$appWidgetId",
            Context.MODE_PRIVATE
        )
        textSize = sp.getFloat("text_size", 14F)
        background = sp.getInt("background", 2)
        eventsCount = sp.getInt("events_count", 4)
        usePhoto = sp.getBoolean("use_photo", false)

        val tempData = EventsHelper.getContactList(context)
        val count = if (tempData.size > eventsCount) eventsCount else tempData.size
        arrayData = ArrayList(tempData.sortedBy { it.nextDate }.subList(0, count))
    }

    override fun onDestroy() {
        arrayData.clear()
    }

    override fun getCount(): Int {
        return if (arrayData.size > eventsCount)
            eventsCount
        else
            arrayData.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val layoutRes = if (usePhoto)
            R.layout.widget_list_item_photo
        else
            R.layout.widget_list_item

        return RemoteViews(context.packageName, layoutRes).apply {
            val currentEvent = arrayData[position]

            val name = if (currentEvent.eventName == context.getString(R.string.event_type_birthday))
                currentEvent.name
            else
                "${currentEvent.name}  [${currentEvent.eventName}]"

            setInt(R.id.widget_item, "setBackgroundResource",
                when (background) {
                    1 -> R.drawable.app_widget_background_1
                    3 -> R.drawable.app_widget_background_3
                    else -> R.drawable.app_widget_background_2
                } )

            val today = LocalDate.now()
            if ((today.dayOfMonth == currentEvent.nextDate.dayOfMonth) && (today.month == currentEvent.nextDate.month))
                setInt(R.id.widget_item, "setBackgroundColor", Color.GREEN)

            val diffDays = ChronoUnit.DAYS.between(today, currentEvent.nextDate).toInt()
            val term = when (diffDays) {
                0 -> context.getString(R.string.today)
                1 -> context.getString(R.string.tomorrow)
                else -> context.resources.getQuantityString(R.plurals.days, diffDays, diffDays)
            }

            if (usePhoto) {
                val contactPhoto =
                    EventsHelper.getContactPhotos(context, currentEvent.lookupKey)
                when (contactPhoto) {
                    is ContactPhotos.PhotoFileId -> {
                        val photoStream =
                            EventsHelper.openDisplayPhoto(
                                context,
                                contactPhoto.fileId
                            )
                        val photoBitmap = BitmapFactory.decodeStream(photoStream)
                        setImageViewBitmap(R.id.iv_photo, photoBitmap)
                    }
                    is ContactPhotos.PhotoThumbnail -> {
                        val thumbnailBytes = contactPhoto.thumbnail
                        val thumbnailBitmap =
                            BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
                        setImageViewBitmap(R.id.iv_photo, thumbnailBitmap)
                    }
                    is ContactPhotos.Empty -> {
                        setImageViewResource(R.id.iv_photo, R.drawable.ic_person)
                    }
                }
            } else
                setImageViewResource(R.id.iv_logo, when (currentEvent.eventName) {
                    context.getString(R.string.event_type_birthday) -> R.drawable.ic_cake
                    else -> R.drawable.ic_event
                })

            setTextViewText(R.id.tv_name, name)
            setTextViewText(R.id.tv_age, currentEvent.ages?.toString() ?: "")
            setTextViewText(R.id.tv_term, term)

            setFloat(R.id.tv_name, "setTextSize", textSize)
            setFloat(R.id.tv_age, "setTextSize", textSize)
            setFloat(R.id.tv_term, "setTextSize", textSize)

            val fillInIntent = Intent().apply {
                Bundle().also { extras ->
                    extras.putLong(EXTRA_CONTACT_ID, currentEvent.id)
                    extras.putString(EXTRA_LOOKUP_KEY, currentEvent.lookupKey)
                    putExtras(extras)
                }
            }

            setOnClickFillInIntent(R.id.widget_item, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

}