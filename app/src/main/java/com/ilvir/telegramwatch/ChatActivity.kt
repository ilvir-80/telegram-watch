package com.ilvir.telegramwatch

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi

class ChatActivity : AppCompatActivity(), TdlibClient.MessagesListener {

    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_CHAT_TITLE = "chat_title"
    }

    private var chatId: Long = 0
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent.getLongExtra(EXTRA_CHAT_ID, 0)
        title = intent.getStringExtra(EXTRA_CHAT_TITLE)

        adapter = MessageAdapter { message -> message.isOutgoing }

        val list: RecyclerView = findViewById(R.id.message_list)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        list.layoutManager = layoutManager
        list.adapter = adapter

        val input: EditText = findViewById(R.id.input_message)
        val sendBtn: ImageButton = findViewById(R.id.btn_send)

        sendBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                TdlibClient.sendText(chatId, text)
                input.text.clear()
            }
        }

        TdlibClient.loadHistory(chatId, 0) { messages ->
            adapter.submit(messages.reversed())
            list.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun onStart() {
        super.onStart()
        TdlibClient.addMessagesListener(this)
        TdlibClient.openChat(chatId)
    }

    override fun onStop() {
        super.onStop()
        TdlibClient.removeMessagesListener(this)
        TdlibClient.closeChat(chatId)
    }

    override fun onNewMessage(chatId: Long, message: TdApi.Message) {
        if (chatId == this.chatId) {
            adapter.append(message)
        }
    }
}
