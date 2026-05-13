package com.example.supriyadigitalproducerfinal

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────────────────────────
// DATA STORE — local SharedPreferences (used only for migration)
// BUG-3 FIX: clearLegacyData() uses a single chained edit block —
//            no nested apply() calls.
// ─────────────────────────────────────────────────────────────────

class DataStore(context: Context) {
    private val prefs = context.getSharedPreferences("ProducerAppData", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEvents(events: List<Event>) {
        prefs.edit { putString("events_data", gson.toJson(events)) }
    }

    fun loadEvents(): MutableList<Event> {
        val json = prefs.getString("events_data", null) ?: return mutableListOf()
        return gson.fromJson(json, object : TypeToken<MutableList<Event>>() {}.type)
    }

    fun saveItemList(items: List<MasterItem>) {
        prefs.edit { putString("item_list_data", gson.toJson(items)) }
    }

    fun loadItemList(): MutableList<MasterItem> {
        val json = prefs.getString("item_list_data", null) ?: return mutableListOf()
        return gson.fromJson(json, object : TypeToken<MutableList<MasterItem>>() {}.type)
    }

    fun saveMasterTakers(takers: List<MasterTaker>) {
        prefs.edit { putString("master_takers_list", gson.toJson(takers)) }
    }

    fun loadMasterTakers(): MutableList<MasterTaker> {
        val json = prefs.getString("master_takers_list", null) ?: return mutableListOf()
        return gson.fromJson(json, object : TypeToken<MutableList<MasterTaker>>() {}.type)
    }

    fun saveAlbumSettings(s: AlbumSettings) {
        prefs.edit { putString("album_settings", gson.toJson(s)) }
    }

    fun loadAlbumSettings(): AlbumSettings {
        val json = prefs.getString("album_settings", null) ?: return AlbumSettings()
        return gson.fromJson(json, AlbumSettings::class.java)
    }

    // BUG-3 FIX: single prefs.edit() chain — one apply() at end
    fun clearLegacyData() {
        prefs.edit()
            .remove("events_data")
            .remove("item_list_data")
            .remove("master_takers_list")
            .apply()
    }
}
