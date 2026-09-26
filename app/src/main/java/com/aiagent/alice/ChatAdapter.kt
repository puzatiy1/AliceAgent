package com.aiagent.alice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_USER = 0
        const val VIEW_AI = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].sender == "Вы") VIEW_USER else VIEW_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_ai, parent, false)
            AIViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is AIViewHolder -> holder.bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.msgText)
        private val time: TextView = view.findViewById(R.id.msgTime)
        fun bind(msg: ChatMessage) {
            text.text = msg.text
            time.text = msg.timestamp
        }
    }

    class AIViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.msgText)
        private val time: TextView = view.findViewById(R.id.msgTime)
        fun bind(msg: ChatMessage) {
            text.text = msg.text
            time.text = msg.timestamp
        }
    }
}
