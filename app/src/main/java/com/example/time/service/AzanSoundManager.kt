package com.example.time.service

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

object AzanSoundManager {
    private var soundUriForFajr: Uri =
        "android.resource://com.example.time/raw/alarm".toUri()

    private var soundUriForAll: Uri =
        "android.resource://com.example.time/raw/abdelbasset".toUri()

    fun updateSoundUriForFajr(context: Context, uri: Uri) {
        soundUriForFajr = uri
        saveSoundUriToPreferences(context, "sound_uri_fajr", uri.toString())
    }

    fun updateSoundUriForAll(context: Context, uri: Uri) {
        soundUriForAll = uri
        saveSoundUriToPreferences(context, "sound_uri_all", uri.toString())
    }

    fun getSoundUriForFajr(context: Context): Uri {
        val saved = getSoundUriFromPreferences(context, "sound_uri_fajr")
        return saved ?: soundUriForFajr
    }

    fun getSoundUriForAll(context: Context): Uri {
        val saved = getSoundUriFromPreferences(context, "sound_uri_all")
        return saved ?: soundUriForAll
    }

    @SuppressLint("UseKtx")
    private fun saveSoundUriToPreferences(context: Context, key: String, uri: String) {
        val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, uri).apply()
    }

    private fun getSoundUriFromPreferences(context: Context, key: String): Uri? {
        val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString(key, null)
        return uriStr?.toUri()
    }
}