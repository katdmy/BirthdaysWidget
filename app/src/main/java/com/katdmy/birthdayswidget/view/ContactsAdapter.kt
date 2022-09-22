package com.katdmy.birthdayswidget.view

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.katdmy.birthdayswidget.data.Contact
import com.katdmy.birthdayswidget.data.ContactPhotos
import com.katdmy.birthdayswidget.EventsHelper
import com.katdmy.birthdayswidget.R
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ContactsAdapter(
    private val clickEvent: (Contact) -> Unit
): RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private val contacts: ArrayList<Contact> = ArrayList()

    class ContactsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val layoutItem: ViewGroup = itemView.findViewById(R.id.layout_item)
        val photoIV: ImageView = itemView.findViewById(R.id.iv_photo)
        val logoIV: ImageView = itemView.findViewById(R.id.iv_logo)
        val nameTV: TextView = itemView.findViewById(R.id.tv_name)
        val typeTV: TextView = itemView.findViewById(R.id.tv_type)
        val ageTV: TextView = itemView.findViewById(R.id.tv_age)
        val termTV: TextView? = itemView.findViewById(R.id.tv_term)
    }

    override fun getItemViewType(position: Int): Int {
        val contact = contacts[position]
        val today = LocalDate.now()
        return  if ((today.dayOfMonth == contact.nextDate.dayOfMonth) && (today.month == contact.nextDate.month)) {
            1
        } else {
            0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val layoutId = when (viewType) {
            1 -> R.layout.activity_list_item_today
            0 -> R.layout.activity_list_item
            else -> throw IllegalArgumentException("Wrong viewType in ContactsAdapter")
        }
        val itemView = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ContactsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val context = holder.itemView.context
        val contact = contacts[position]

        val today = LocalDate.now()
        val diffDays = ChronoUnit.DAYS.between(today, contact.nextDate).toInt()
        val term = when (diffDays) {
            0 -> context.getString(R.string.today)
            1 -> context.getString(R.string.tomorrow)
            else -> context.resources.getQuantityString(R.plurals.days, diffDays, diffDays)
        }

        holder.logoIV.setImageResource(when (contact.eventName) {
            context.getString(R.string.event_type_birthday) -> R.drawable.ic_cake
            else -> R.drawable.ic_event
        })
        holder.nameTV.text = contact.name
        holder.typeTV.text = contact.eventName
        holder.ageTV.text = contact.ages?.toString() ?: ""
        holder.termTV?.text = term

        val contactPhoto = EventsHelper.getContactPhotos(context, contact.lookupKey)
        when (contactPhoto) {
            is ContactPhotos.PhotoFileId -> {
                val photoStream =
                    EventsHelper.openDisplayPhoto(context, contactPhoto.fileId)
                val photoBitmap = BitmapFactory.decodeStream(photoStream)
                holder.photoIV.setImageBitmap(photoBitmap)
            }
            is ContactPhotos.PhotoThumbnail -> {
                val thumbnailBytes = contactPhoto.thumbnail
                val thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
                holder.photoIV.setImageBitmap(thumbnailBitmap)
            }
            is ContactPhotos.Empty -> {
                holder.photoIV.setImageResource(R.drawable.ic_person)
            }
        }

        holder.layoutItem.setOnClickListener { clickEvent(contact) }
    }

    override fun getItemCount(): Int = contacts.size

    fun setData(newContacts: ArrayList<Contact>) {
        contacts.clear()
        contacts.addAll(newContacts.sortedBy { it.nextDate })
        notifyDataSetChanged()
    }
}