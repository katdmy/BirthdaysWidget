package com.katdmy.birthdayswidget.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.katdmy.birthdayswidget.BuildConfig
import com.katdmy.birthdayswidget.EventsHelper
import com.katdmy.birthdayswidget.R
import com.katdmy.birthdayswidget.data.Contact
import com.katdmy.birthdayswidget.widget.WIDGET_DETAILS_ACTION
import com.katdmy.birthdayswidget.widget.EXTRA_CONTACT_ID
import com.katdmy.birthdayswidget.widget.EXTRA_LOOKUP_KEY

class MainActivity : Activity() {

    private lateinit var listRV: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var topAppBar: MaterialToolbar

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 10001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkPermissions()) {
            // App does not have permissions, ask for permissions.
            requestPermissions()
        } else {
            initViews()
            startWork()
        }

        if (intent.action == WIDGET_DETAILS_ACTION) {
            val contactId: Long = intent.getLongExtra(EXTRA_CONTACT_ID, 0)
            val lookupKey: String = intent.getStringExtra(EXTRA_LOOKUP_KEY) ?: ""
            val lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
            val uri = Uri.withAppendedPath(lookupUri, contactId.toString())
            val contactIntent = Intent(Intent.ACTION_VIEW, uri)
            contactIntent.data = uri
            startActivity(contactIntent)
            finish()
        }
    }

    override fun onResume() {
        startWork()
        super.onResume()
    }

    private fun initViews() {
        listRV = findViewById(R.id.rv_list)
        adapter = ContactsAdapter { contact: Contact ->
            val lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contact.lookupKey)
            val uri = Uri.withAppendedPath(lookupUri, contact.id.toString())
            val contactIntent = Intent(Intent.ACTION_VIEW, uri)
            contactIntent.data = uri
            startActivity(contactIntent)
        }
        listRV.adapter = adapter
        listRV.layoutManager = LinearLayoutManager(this)

        topAppBar = findViewById(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_settings -> {
                    val settingsIntent = Intent(this, SettingsActivity::class.java)
                    startActivity(settingsIntent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkPermissions(): Boolean {

        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.READ_CONTACTS
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Snackbar.make(
                findViewById(R.id.frame_content),
                getString(R.string.permission_rationale),
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(getString(R.string.permission_action_provide)) { // Request permission
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.READ_CONTACTS),
                        REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initViews()
                    startWork()
                } else {
                    // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.
                    Snackbar.make(
                        findViewById(R.id.frame_content),
                        getString(R.string.permission_notification),
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(getString(R.string.permission_action_provide)) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    private fun startWork() {
        adapter.setData(EventsHelper.getContactList(this))
    }
}