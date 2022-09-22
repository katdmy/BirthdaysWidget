package com.katdmy.birthdayswidget

import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Photo
import androidx.core.database.getBlobOrNull
import androidx.core.database.getIntOrNull
import com.katdmy.birthdayswidget.data.Contact
import com.katdmy.birthdayswidget.data.ContactPhotos
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object EventsHelper {
    private val PROJECTION_LIST = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME,
        Event.DATA1,
        Event.DATA2,
        Event.DATA3
    )

    private val PROJECTION_DETAILS = arrayOf(
        ContactsContract.Contacts._ID,
        Photo.PHOTO_FILE_ID,
        Photo.PHOTO
    )

    private const val SELECTION_LIST: String =
        "${ContactsContract.Data.MIMETYPE} = '${Event.CONTENT_ITEM_TYPE}'"

    private const val SELECTION_DETAILS: String =
        "${ContactsContract.Data.LOOKUP_KEY} = ? AND "+
        "${ContactsContract.Data.MIMETYPE} = '${Photo.CONTENT_ITEM_TYPE}'"

    private lateinit var SELECTION_DETAILS_ARGS: Array<String>


    fun getContactList(context: Context): ArrayList<Contact> {
        val contactList: ArrayList<Contact> = ArrayList()

        val cr = context.contentResolver
        val cursor: Cursor? = cr.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_LIST,
            SELECTION_LIST,
            null,
            Event.DISPLAY_NAME + " ASC"
        )
        if (cursor != null) {
            try {
                val idIndex: Int = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val lookupKeyIndex: Int = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                val nameIndex: Int = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val startDateIndex: Int =
                    cursor.getColumnIndex(Event.DATA1)
                val typeIndex: Int =
                    cursor.getColumnIndex(Event.DATA2)
                val labelIndex: Int =
                    cursor.getColumnIndex(Event.DATA3)
                var id: Long
                var lookupKey: String
                var name: String
                var eventDate: LocalDate
                var type: Int?
                var nextDate: LocalDate
                while (cursor.moveToNext()) {
                    id = cursor.getLong(idIndex)

                    lookupKey = cursor.getString(lookupKeyIndex)

                    name = cursor.getString(nameIndex)

                    var ages: Int?

                    var date = cursor.getString(startDateIndex)
                    if (date.startsWith("--")) {
                        date = LocalDate.now().year.toString() + date.drop(1)
                        ages = null
                        eventDate = LocalDate.parse(date)
                        nextDate = if (eventDate.isBefore(LocalDate.now()))
                            eventDate.plusYears(1L)
                        else
                            eventDate
                    } else {
                        eventDate = LocalDate.parse(date)

                        val today = LocalDate.now()
                        ages =
                            if (eventDate.month == today.month && eventDate.dayOfMonth == today.dayOfMonth)
                                ChronoUnit.YEARS.between(eventDate, today).toInt()
                            else
                                (ChronoUnit.YEARS.between(eventDate, today) + 1).toInt()

                        nextDate = if (eventDate.isBefore(LocalDate.now()))
                            eventDate.plusYears(ages.toLong())
                        else
                            eventDate
                    }



                    type = cursor.getIntOrNull(typeIndex)

                    val eventName = if (type == 3) context.getString(R.string.event_type_birthday)
                        else cursor.getString(labelIndex) ?: "no label"

                    contactList.add(Contact(id, lookupKey, name, ages, eventName, nextDate))
                }
            } finally {
                cursor.close()
            }
        }
        return contactList
    }

    fun getContactPhotos(context: Context, lookupKey: String): ContactPhotos {
        var result : ContactPhotos = ContactPhotos.Empty

        SELECTION_DETAILS_ARGS = arrayOf(lookupKey)

        val cr = context.contentResolver
        val cursor: Cursor? = cr.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_DETAILS,
            SELECTION_DETAILS,
            SELECTION_DETAILS_ARGS,
            null
        )
        if (cursor != null) {
            try {
                val photoFileIdIndex: Int = cursor.getColumnIndex(Photo.PHOTO_FILE_ID)
                val photoIndex: Int = cursor.getColumnIndex(Photo.PHOTO)
                var photoFileId: Long
                var photo: ByteArray?
                while (cursor.moveToNext()) {
                    photoFileId = cursor.getLong(photoFileIdIndex)
                    photo = cursor.getBlobOrNull(photoIndex)

                    if (photoFileId > 0)
                        result = ContactPhotos.PhotoFileId(photoFileId)
                    else if (photo != null)
                        result = ContactPhotos.PhotoThumbnail(photo)
                }
            } finally {
                cursor.close()
            }
        }
        return result
    }

    fun getContactListSample(context: Context): ArrayList<Contact> {
        val contactList: ArrayList<Contact> = ArrayList()
        contactList.addAll(
            listOf(
                Contact(0, "lookupKey0", "Комаров Евгений", 24, "День рождения", LocalDate.of(2022,11,17)),
                Contact(1, "lookupKey1", "Лыткин Зиновий", 16, "День рождения", LocalDate.of(2023,2,11)),
                Contact(2, "lookupKey2", "Шубин Вальтер", 35, "Годовщина", LocalDate.of(2023,4,23)),
                Contact(3, "lookupKey3", "Прохоров Геннадий", 72, "День рождения", LocalDate.of(2022,9,4))
            )
        )
        return contactList
    }

    fun openDisplayPhoto(context: Context, photoFileId: Long): InputStream? {
        val displayPhotoUri: Uri = ContentUris.withAppendedId(ContactsContract.DisplayPhoto.CONTENT_URI, photoFileId)
        return try {
            val fd: AssetFileDescriptor? = context.contentResolver.openAssetFileDescriptor(displayPhotoUri, "r")
            fd?.createInputStream()
        } catch (e: IOException) {
            null
        }
    }
}