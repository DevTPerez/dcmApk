package com.example.dcmapk

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {

    private const val PREF_NAME = "Settings"
    private const val PREF_CLIENT_SELECTED = "clientSelected"
    private const val PREF_LOCATE_SELECTED = "locateSelected"
    private const val PREF_MODULE_SELECTED = "moduleSelected"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePreferences(context: Context, locateSelected: String, moduleSelected: String, clientSelected: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_LOCATE_SELECTED, locateSelected)
        editor.putString(PREF_MODULE_SELECTED, moduleSelected)
        editor.putString(PREF_CLIENT_SELECTED, clientSelected)
        editor.apply()
    }

    fun loadPreferences(context: Context): Triple<String?, String?, String?> {
        val sharedPreferences = getSharedPreferences(context)
        val locateSelected = sharedPreferences.getString(PREF_LOCATE_SELECTED, null)
        val moduleSelected = sharedPreferences.getString(PREF_MODULE_SELECTED, null)
        val clientSelected = sharedPreferences.getString(PREF_CLIENT_SELECTED, null)

        return Triple(locateSelected, moduleSelected, clientSelected)
    }

    fun getClientPreference(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_CLIENT_SELECTED, null)
    }
    fun getLocatePreference(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_LOCATE_SELECTED, null)
    }
    fun getModulePreference(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_MODULE_SELECTED, null)
    }

}
