package com.aiagent.alice

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
)
