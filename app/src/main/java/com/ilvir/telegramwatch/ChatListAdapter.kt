package com.ilvir.telegramwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi

class ChatListAdapter(
    private val onClick: (TdApi.Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.VH>() {

    private val items = mutableListOf<TdApi.Chat>()

    fun submit(newItems: List<TdApi.Chat>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.chat_title)
        val lastMessage: TextView = view.findViewById(R.id.chat_last_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chat = items[position]
        holder.title.text = chat.title
        holder.lastMessage.text = extractPreview(chat.lastMessage)
        holder.itemView.setOnClickListener { onClick(chat) }
    }

    override fun getItemCount() = items.size

    private fun extractPreview(message: TdApi.Message?): String {
        val content = message?.content ?: return ""
        return when (content) {
            is TdApi.MessageText -> content.text.text
            is TdApi.MessagePhoto -> "📷 Фото"
            is TdApi.MessageVoiceNote -> "🎤 Голосовое"
            is TdApi.MessageSticker -> "Стикер"
            else -> ""
        }
    }
}
