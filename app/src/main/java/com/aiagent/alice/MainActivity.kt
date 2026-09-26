package com.aiagent.alice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiagent.alice.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private val messages = mutableListOf<ChatMessage>()
    private val httpClient = OkHttpClient()
    private var isListening = false

    // ВАЖНО: Вставь свой API ключ от Anthropic сюда
    private val API_KEY = "YOUR_ANTHROPIC_API_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTTS()
        setupSpeechRecognizer()
        setupClickListeners()
        loadNotes()

        // Приветствие
        addMessage("ИИ Агент", "Привет! Я твой личный ИИ ассистент. Могу отвечать на вопросы, создавать заметки и помогать звонить друзьям. Нажми на микрофон или напиши мне!")
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("ru", "RU")
                tts.setSpeechRate(0.95f)
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Распознавание речи недоступно", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.micBtn.setImageResource(R.drawable.ic_mic_active)
                binding.statusText.text = "Слушаю..."
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: return
                binding.inputField.setText(text)
                stopListening()
                sendMessage(text)
            }
            override fun onError(error: Int) {
                stopListening()
                binding.statusText.text = "Нажми для ввода"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupClickListeners() {
        binding.micBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (isListening) stopListening() else startListening()
        }

        binding.sendBtn.setOnClickListener {
            val text = binding.inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.inputField.setText("")
                sendMessage(text)
            }
        }

        binding.notesBtn.setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java))
        }

        binding.contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        binding.micBtn.setImageResource(R.drawable.ic_mic)
        binding.statusText.text = "Нажми для ввода"
    }

    private fun sendMessage(text: String) {
        addMessage("Вы", text)
        binding.inputField.setText("")

        // Проверяем команды
        when {
            text.contains("заметк", ignoreCase = true) && text.contains("запиши", ignoreCase = true) -> {
                val noteText = text.replace(Regex("запиши|заметку|заметка", RegexOption.IGNORE_CASE), "").trim()
                saveNote(noteText)
                val response = "Записал заметку: $noteText"
                addMessage("ИИ Агент", response)
                tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                return
            }
            text.contains("позвони", ignoreCase = true) -> {
                val contactName = text.replace(Regex("позвони|позвон"), "").trim()
                callContact(contactName)
                return
            }
        }

        // Отправляем в Claude API
        lifecycleScope.launch {
            callClaudeAPI(text)
        }
    }

    private fun callClaudeAPI(userText: String) {
        binding.typingIndicator.text = "ИИ Агент печатает..."

        val messagesArray = JSONArray()
        // Добавляем историю (последние 10 сообщений)
        val history = messages.takeLast(10).filter { it.sender != "Система" }
        for (msg in history) {
            val role = if (msg.sender == "Вы") "user" else "assistant"
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            })
        }

        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 1000)
            put("system", "Ты умный голосовой ИИ ассистент, похожий на Алису. Отвечай по-русски, кратко и дружелюбно. Можешь помогать с любыми вопросами.")
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", API_KEY)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.typingIndicator.text = ""
                    addMessage("ИИ Агент", "Нет соединения с сервером. Проверь интернет.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: ""
                runOnUiThread {
                    binding.typingIndicator.text = ""
                    try {
                        val json = JSONObject(responseText)
                        val reply = json.getJSONArray("content").getJSONObject(0).getString("text")
                        addMessage("ИИ Агент", reply)
                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                    } catch (e: Exception) {
                        addMessage("ИИ Агент", "Ошибка получения ответа.")
                    }
                }
            }
        })
    }

    private fun saveNote(text: String) {
        val prefs = getSharedPreferences("notes", MODE_PRIVATE)
        val existing = prefs.getString("all_notes", "") ?: ""
        val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(java.util.Date())
        val newNotes = "$existing\n[$timestamp] $text".trim()
        prefs.edit().putString("all_notes", newNotes).apply()
    }

    private fun loadNotes() {
        // Notes are loaded in NotesActivity
    }

    private fun callContact(name: String) {
        val prefs = getSharedPreferences("contacts", MODE_PRIVATE)
        val phone = prefs.getString(name.lowercase().trim(), null)
        if (phone != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")))
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 101)
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
            addMessage("ИИ Агент", "Звоню $name...")
        } else {
            addMessage("ИИ Агент", "Не нашёл контакт «$name». Добавь его в разделе Контакты.")
        }
    }

    private fun addMessage(sender: String, text: String) {
        messages.add(ChatMessage(sender, text))
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onDestroy() {
        tts.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
