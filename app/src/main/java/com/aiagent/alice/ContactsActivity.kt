package com.aiagent.alice

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ContactsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        val contactsList = findViewById<TextView>(R.id.contactsList)
        val nameInput = findViewById<EditText>(R.id.contactNameInput)
        val phoneInput = findViewById<EditText>(R.id.contactPhoneInput)
        val saveBtn = findViewById<Button>(R.id.saveContactBtn)

        loadContacts(contactsList)

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim().lowercase()
            val phone = phoneInput.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val prefs = getSharedPreferences("contacts", MODE_PRIVATE)
                prefs.edit().putString(name, phone).apply()
                nameInput.setText("")
                phoneInput.setText("")
                loadContacts(contactsList)
            }
        }
    }

    private fun loadContacts(view: TextView) {
        val prefs = getSharedPreferences("contacts", MODE_PRIVATE)
        val all = prefs.all
        if (all.isEmpty()) {
            view.text = "Контактов пока нет.\nДобавь имя и номер телефона."
        } else {
            view.text = all.entries.joinToString("\n") { "👤 ${it.key}: ${it.value}" }
        }
    }
}
