package com.example.time.Manager

import android.app.AlertDialog
import android.content.Context

class SoundSelectionHandler(private val context: Context) {

    fun showSoundSelectionDialog(title: String, choices: Array<String>, onSelect: (Int) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(choices) { dialog, which ->
                onSelect(which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}