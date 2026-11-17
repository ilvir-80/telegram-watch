package com.ilvir.telegramwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi

class MessageAdapter(
    private val isOutgoing: (TdApi.Message) -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TdApi.Message>()

    companion object {
        private const val TYPE_IN = 0
        private const val TYPE_OUT = 1
    }

    fun submit(newItems: List<TdApi.Message>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun append(message: TdApi.Message) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.message_text)
    }

    override fun getItemViewType(position: Int) =
        if (isOutgoing(items[position])) TYPE_OUT else TYPE_IN

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_OUT) R.layout.item_message_out else R.layout.item_message_in
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        (holder as VH).text.text = renderContent(message.content)
    }

    override fun getItemCount() = items.size

    private fun renderContent(content: TdApi.MessageContent): String = when (content) {
        is TdApi.MessageText -> content.text.text
        is TdApi.MessagePhoto -> "📷 Фото" + (content.caption?.text?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
        is TdApi.MessageVoiceNote -> "🎤 Голосовое сообщение (${content.voiceNote.duration} сек)"
        is TdApi.MessageSticker -> "🙂 ${content.sticker.emoji}"
        is TdApi.MessageVideo -> "🎬 Видео"
        is TdApi.MessageDocument -> "📎 Файл: ${content.document.fileName}"
        else -> "[неподдерживаемый тип сообщения]"
    }
}
