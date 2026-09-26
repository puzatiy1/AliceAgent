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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
    private val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
    private val MODEL = "llama3-70b-8192"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTTS()
        setupSpeechRecognizer()
        setupClickListeners()

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
                stopListening()
                sendMessage(text)
            }
            override fun onError(error: Int) { stopListening() }
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

        when {
            text.contains("запиши", ignoreCase = true) -> {
                val noteText = text.replace(Regex("запиши|заметку|заметка", RegexOption.IGNORE_CASE), "").trim()
                saveNote(noteText)
                val response = "Записал: $noteText"
                addMessage("ИИ Агент", response)
                tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                return
            }
            text.contains("позвони", ignoreCase = true) -> {
                val name = text.replace(Regex("позвони|позвон"), "").trim()
                callContact(name)
                return
            }
        }

        lifecycleScope.launch { callGroqAPI(text) }
    }

    private suspend fun callGroqAPI(userText: String) {
        withContext(Dispatchers.Main) { binding.typingIndicator.text = "ИИ Агент печатает..." }

        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", """Ты личный ИИ ассистент по имени Макс. Ты умный, дружелюбный и знаешь всё.
Ты умеешь:
- Отвечать на любые вопросы: наука, история, медицина, право, кулинария, спорт, технологии, культура
- Помогать с математикой и расчётами
- Переводить тексты на любой язык
- Писать тексты, письма, посты, стихи, сценарии
- Давать советы по здоровью, питанию, тренировкам (с оговоркой что ты не врач)
- Помогать с работой: резюме, бизнес-планы, презентации
- Объяснять сложные вещи простыми словами
- Рассказывать новости и факты из своих знаний
- Помогать планировать путешествия, маршруты
- Развлекать: загадки, шутки, истории, игры в слова
- Создавать заметки (скажи "запиши...")
- Звонить контактам (скажи "позвони [имя]")

Правила:
- Всегда отвечай на том языке на котором спрашивают
- Если спрашивают по-русски — отвечай по-русски
- Будь краток когда вопрос простой, развёрнут когда нужно объяснение
- Никогда не говори что не знаешь — давай лучший возможный ответ
- Ты говоришь вслух, поэтому избегай списков с символами — говори естественно
- Обращайся к пользователю на "ты"
- Имя пользователя: Алекс""")
        })

        val history = messages.takeLast(10)
        for (msg in history) {
            val role = if (msg.sender == "Вы") "user" else "assistant"
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            })
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1000)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(GROQ_URL)
            .addHeader("Authorization", "Bearer $GROQ_API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.typingIndicator.text = ""
                    addMessage("ИИ Агент", "Нет соединения. Проверь интернет.")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: ""
                runOnUiThread {
                    binding.typingIndicator.text = ""
                    try {
                        val json = JSONObject(responseText)
                        val reply = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        addMessage("ИИ Агент", reply)
                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                    } catch (e: Exception) {
                        addMessage("ИИ Агент", "Ошибка: $responseText")
                    }
                }
            }
        })
    }

    private fun saveNote(text: String) {
        val prefs = getSharedPreferences("notes", MODE_PRIVATE)
        val existing = prefs.getString("all_notes", "") ?: ""
        val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(java.util.Date())
        prefs.edit().putString("all_notes", "$existing\n[$timestamp] $text".trim()).apply()
    }

    private fun callContact(name: String) {
        val prefs = getSharedPreferences("contacts", MODE_PRIVATE)
        val phone = prefs.getString(name.lowercase().trim(), null)
        if (phone != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
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
