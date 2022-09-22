package com.katdmy.birthdayswidget.data

sealed class ContactPhotos {
    class PhotoFileId(val fileId: Long) : ContactPhotos()
    class PhotoThumbnail(val thumbnail: ByteArray) : ContactPhotos()
    object Empty: ContactPhotos()
}
