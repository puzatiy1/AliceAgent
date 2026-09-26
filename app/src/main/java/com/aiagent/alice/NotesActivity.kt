package com.aiagent.alice

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        val notesView = findViewById<TextView>(R.id.notesText)
        val inputField = findViewById<EditText>(R.id.noteInput)
        val saveBtn = findViewById<Button>(R.id.saveNoteBtn)
        val clearBtn = findViewById<Button>(R.id.clearNotesBtn)

        loadNotes(notesView)

        saveBtn.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                saveNote(text)
                inputField.setText("")
                loadNotes(notesView)
            }
        }

        clearBtn.setOnClickListener {
            getSharedPreferences("notes", MODE_PRIVATE).edit().clear().apply()
            notesView.text = "Заметок пока нет"
        }
    }

    private fun saveNote(text: String) {
        val prefs = getSharedPreferences("notes", MODE_PRIVATE)
        val existing = prefs.getString("all_notes", "") ?: ""
        val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val newNotes = "$existing\n[$timestamp] $text".trim()
        prefs.edit().putString("all_notes", newNotes).apply()
    }

    private fun loadNotes(view: TextView) {
        val prefs = getSharedPreferences("notes", MODE_PRIVATE)
        val notes = prefs.getString("all_notes", "")
        view.text = if (notes.isNullOrEmpty()) "Заметок пока нет" else notes
    }
}
