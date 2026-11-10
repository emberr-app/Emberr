package com.emberr.presentation.people

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsPickerSessionContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.people.ContactDetailToImport
import com.emberr.domain.people.ContactToImport
import com.emberr.domain.people.contactDateToBirthdayText
import com.emberr.domain.util.system.showNativeToast
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.people_could_not_read_contact
import emberr.shared.generated.resources.people_no_contacts_app_found
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

private const val ANDROID_17 = 37
private const val CUSTOM_TYPE = 0

private val phoneLabels = mapOf(Phone.TYPE_HOME to "Home", Phone.TYPE_WORK to "Work", Phone.TYPE_MAIN to "Main")
private val emailLabels = mapOf(Email.TYPE_HOME to "Personal", Email.TYPE_WORK to "Work", Email.TYPE_MOBILE to "Mobile")
private val addressLabels = mapOf(StructuredPostal.TYPE_HOME to "Home", StructuredPostal.TYPE_WORK to "Work")
private val eventLabels = mapOf(Event.TYPE_ANNIVERSARY to "Anniversary", Event.TYPE_OTHER to "Date")

@Composable
actual fun rememberPhoneContactPicker(onContactsPicked: (List<ContactToImport>) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnContactsPicked by rememberUpdatedState(onContactsPicked)
    var isNewPickerOpen by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pickedUri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || pickedUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val contacts = withContext(Dispatchers.IO) {
                try {
                    if (isNewPickerOpen) {
                        readContactsFromPickerSession(context, pickedUri)
                    } else {
                        listOfNotNull(readNameAndPhotoOfContact(context, pickedUri))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            if (contacts.isNullOrEmpty()) showNativeToast(getString(Res.string.people_could_not_read_contact)) else latestOnContactsPicked(contacts)
        }
    }
    return {
        isNewPickerOpen = Build.VERSION.SDK_INT >= ANDROID_17 && tryLaunching { launcher.launch(newContactPickerIntent()) }
        if (!isNewPickerOpen && !tryLaunching { launcher.launch(Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI)) }) {
            scope.launch { showNativeToast(getString(Res.string.people_no_contacts_app_found)) }
        }
    }
}

private fun tryLaunching(launch: () -> Unit): Boolean =
    try {
        launch()
        true
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        false
    }

@RequiresApi(ANDROID_17)
private fun newContactPickerIntent(): Intent {
    val requestedFields = arrayListOf(
        StructuredName.CONTENT_ITEM_TYPE,
        Phone.CONTENT_ITEM_TYPE,
        Email.CONTENT_ITEM_TYPE,
        StructuredPostal.CONTENT_ITEM_TYPE,
        Event.CONTENT_ITEM_TYPE,
        Organization.CONTENT_ITEM_TYPE,
        Website.CONTENT_ITEM_TYPE,
        Nickname.CONTENT_ITEM_TYPE,
        Photo.CONTENT_ITEM_TYPE
    )
    return Intent(ContactsPickerSessionContract.ACTION_PICK_CONTACTS).apply {
        putExtra(Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER, true)
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        putExtra(ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS, false)
        putStringArrayListExtra(ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS, requestedFields)
    }
}

private class PickedContact(val name: String) {
    val details = mutableListOf<ContactDetailToImport>()
    var thumbnailBytes: ByteArray? = null

    fun add(kind: PersonDetailKind, label: String?, value: String?) {
        if (!value.isNullOrBlank()) details += ContactDetailToImport(kind = kind, label = label, value = value)
    }
}

private fun readContactsFromPickerSession(context: Context, sessionUri: Uri): List<ContactToImport> {
    val contactsByKey = linkedMapOf<String, PickedContact>()
    val projection = arrayOf(
        Contacts.LOOKUP_KEY,
        Contacts.DISPLAY_NAME_PRIMARY,
        Data.MIMETYPE,
        Data.DATA1,
        Data.DATA2,
        Data.DATA3,
        Data.DATA4,
        Data.DATA15
    )
    context.contentResolver.query(sessionUri, projection, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.text(Contacts.DISPLAY_NAME_PRIMARY).orEmpty()
            val contactKey = cursor.text(Contacts.LOOKUP_KEY) ?: name
            val contact = contactsByKey.getOrPut(contactKey) { PickedContact(name = name) }
            addPickedRow(contact, cursor)
        }
    }
    return contactsByKey.values.map { contact ->
        ContactToImport(
            name = contact.name,
            details = contact.details,
            note = "",
            photoBytes = contact.thumbnailBytes
        )
    }
}

private fun addPickedRow(contact: PickedContact, cursor: Cursor) {
    val value = cursor.text(Data.DATA1)
    when (cursor.text(Data.MIMETYPE)) {
        Phone.CONTENT_ITEM_TYPE -> contact.add(PersonDetailKind.PHONE, cursor.typeLabel(phoneLabels), value)
        Email.CONTENT_ITEM_TYPE -> contact.add(PersonDetailKind.EMAIL, cursor.typeLabel(emailLabels), value)
        StructuredPostal.CONTENT_ITEM_TYPE -> contact.add(PersonDetailKind.ADDRESS, cursor.typeLabel(addressLabels), value)
        Event.CONTENT_ITEM_TYPE -> addPickedDate(contact, cursor, value)
        Organization.CONTENT_ITEM_TYPE -> {
            contact.add(PersonDetailKind.COMPANY, null, value)
            contact.add(PersonDetailKind.ROLE, null, cursor.text(Organization.TITLE))
        }
        Website.CONTENT_ITEM_TYPE -> contact.add(PersonDetailKind.WEBSITE, null, value)
        Nickname.CONTENT_ITEM_TYPE -> contact.add(PersonDetailKind.CUSTOM, "Nickname", value)
        Photo.CONTENT_ITEM_TYPE -> contact.thumbnailBytes = contact.thumbnailBytes ?: cursor.bytes(Photo.PHOTO)
    }
}

private fun addPickedDate(contact: PickedContact, cursor: Cursor, value: String?) {
    if (value.isNullOrBlank()) return
    val birthdayText = contactDateToBirthdayText(value)
    when {
        cursor.int(Data.DATA2) == Event.TYPE_BIRTHDAY && birthdayText != null -> contact.add(PersonDetailKind.BIRTHDAY, null, birthdayText)
        cursor.int(Data.DATA2) == Event.TYPE_BIRTHDAY -> contact.add(PersonDetailKind.CUSTOM, "Birthday", value)
        else -> contact.add(PersonDetailKind.CUSTOM, cursor.typeLabel(eventLabels) ?: "Date", value)
    }
}

private fun readNameAndPhotoOfContact(context: Context, contactUri: Uri): ContactToImport? {
    val projection = arrayOf(Contacts.DISPLAY_NAME_PRIMARY, Contacts.PHOTO_URI)
    return context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        ContactToImport(
            name = cursor.text(Contacts.DISPLAY_NAME_PRIMARY).orEmpty(),
            details = emptyList(),
            note = "",
            photoBytes = readPhotoOrNull(context, cursor.text(Contacts.PHOTO_URI))
        )
    }
}

private fun readPhotoOrNull(context: Context, photoUri: String?): ByteArray? {
    if (photoUri == null) return null
    return try {
        context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream -> stream.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Cursor.typeLabel(knownLabels: Map<Int, String>): String? {
    val type = int(Data.DATA2) ?: return null
    return if (type == CUSTOM_TYPE) text(Data.DATA3) else knownLabels[type]
}

private fun Cursor.text(column: String): String? {
    val index = getColumnIndex(column)
    return if (index < 0 || isNull(index)) null else getString(index)
}

private fun Cursor.int(column: String): Int? {
    val index = getColumnIndex(column)
    return if (index < 0 || isNull(index)) null else getInt(index)
}

private fun Cursor.bytes(column: String): ByteArray? {
    val index = getColumnIndex(column)
    return if (index < 0 || isNull(index)) null else getBlob(index)
}
