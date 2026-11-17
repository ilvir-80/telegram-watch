package com.ilvir.telegramwatch

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi

class ChatListActivity : AppCompatActivity(), TdlibClient.ChatsListener {

    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        adapter = ChatListAdapter { chat ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(ChatActivity.EXTRA_CHAT_ID, chat.id)
            intent.putExtra(ChatActivity.EXTRA_CHAT_TITLE, chat.title)
            startActivity(intent)
        }

        val list: RecyclerView = findViewById(R.id.chat_list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        refresh()

        // Запускаем фоновый сервис, чтобы сообщения приходили, даже когда
        // экран списка чатов не открыт.
        TdlibBackgroundService.start(this)
    }

    override fun onStart() {
        super.onStart()
        TdlibClient.addChatsListener(this)
        refresh()
    }

    override fun onStop() {
        super.onStop()
        TdlibClient.removeChatsListener(this)
    }

    override fun onChatsChanged() = refresh()

    private fun refresh() {
        val sorted: List<TdApi.Chat> = TdlibClient.chats.values.sortedByDescending { chat ->
            chat.positions?.firstOrNull { it.list is TdApi.ChatListMain }?.order ?: 0L
        }
        adapter.submit(sorted)
    }
}
