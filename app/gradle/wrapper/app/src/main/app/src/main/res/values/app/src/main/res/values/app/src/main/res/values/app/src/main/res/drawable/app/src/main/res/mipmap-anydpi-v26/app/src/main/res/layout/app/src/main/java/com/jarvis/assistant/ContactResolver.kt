package com.jarvis.assistant

import android.content.Context
import android.provider.ContactsContract

object ContactResolver {

    fun findPhoneNumber(context: Context, name: String): String? {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (cursor.moveToFirst() && numberIndex >= 0) {
                return cursor.getString(numberIndex)?.replace(" ", "")
            }
        }
        return null
    }
}
