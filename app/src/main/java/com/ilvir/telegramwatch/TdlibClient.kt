package com.ilvir.telegramwatch

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/**
 * Единая точка входа в TDLib на всё приложение. Держит один Client,
 * раздаёт обновления подписчикам (экранам) и прячет низкоуровневые детали
 * авторизации.
 *
 * ВАЖНО: пакет org.drinkless.tdlib.* генерируется самой сборкой TDLib
 * (см. .github/workflows/build.yml) и физически появляется в проекте
 * только во время CI-сборки — локально IDE будет подсвечивать эти
 * импорты как отсутствующие, это нормально.
 */
object TdlibClient {

    interface AuthListener {
        fun onAuthState(state: TdApi.AuthorizationState)
    }

    interface ChatsListener {
        fun onChatsChanged()
    }

    interface MessagesListener {
        fun onNewMessage(chatId: Long, message: TdApi.Message)
    }

    private lateinit var appContext: Context
    private var client: Client? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val authListeners = mutableListOf<AuthListener>()
    private val chatsListeners = mutableListOf<ChatsListener>()
    private val messagesListeners = mutableListOf<MessagesListener>()

    // chatId -> Chat, простейший локальный кэш списка чатов
    val chats = LinkedHashMap<Long, TdApi.Chat>()

    var lastAuthState: TdApi.AuthorizationState? = null
        private set

    fun init(context: Context) {
        if (client != null) return
        appContext = context.applicationContext

        client = Client.create(
            { update -> handleUpdate(update) },
            null,
            null
        )
    }

    fun addAuthListener(l: AuthListener) {
        authListeners.add(l)
        lastAuthState?.let { state -> mainHandler.post { l.onAuthState(state) } }
    }

    fun removeAuthListener(l: AuthListener) = authListeners.remove(l)

    fun addChatsListener(l: ChatsListener) = chatsListeners.add(l)
    fun removeChatsListener(l: ChatsListener) = chatsListeners.remove(l)

    fun addMessagesListener(l: MessagesListener) = messagesListeners.add(l)
    fun removeMessagesListener(l: MessagesListener) = messagesListeners.remove(l)

    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                lastAuthState = update.authorizationState
                onAuthorizationStateUpdated(update.authorizationState)
                mainHandler.post {
                    authListeners.forEach { it.onAuthState(update.authorizationState) }
                }
            }

            is TdApi.UpdateNewChat -> {
                chats[update.chat.id] = update.chat
                notifyChatsChanged()
            }

            is TdApi.UpdateChatLastMessage -> {
                chats[update.chatId]?.let { chat ->
                    chat.lastMessage = update.lastMessage
                }
                notifyChatsChanged()
            }

            is TdApi.UpdateChatPosition -> {
                // Порядок чатов мог измениться — просто просим экран перерисоваться,
                // сортировку делает ChatListActivity по chat.positions.
                notifyChatsChanged()
            }

            is TdApi.UpdateNewMessage -> {
                mainHandler.post {
                    messagesListeners.forEach { it.onNewMessage(update.message.chatId, update.message) }
                }
            }

            else -> { /* остальные апдейты пока игнорируем */ }
        }
    }

    private fun notifyChatsChanged() {
        mainHandler.post { chatsListeners.forEach { it.onChatsChanged() } }
    }

    private fun onAuthorizationStateUpdated(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val dbDir = appContext.filesDir.absolutePath + "/tdlib"
                val filesDir = appContext.filesDir.absolutePath + "/tdlib-files"
                send(
                    TdApi.SetTdlibParameters(
                        false, // useTestDc
                        dbDir,
                        filesDir,
                        null, // databaseEncryptionKey
                        true, // useFileDatabase
                        true, // useChatInfoDatabase
                        true, // useMessageDatabase
                        false, // useSecretChats
                        Config.API_ID,
                        Config.API_HASH,
                        "ru",
                        "Android Watch",
                        "HarmonyOS/Android",
                        "0.1"
                    )
                ) { }
            }

            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                // Ждём, пока LoginActivity вызовет submitPhone()
            }

            is TdApi.AuthorizationStateWaitCode -> {
                // Ждём submitCode()
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                // Ждём submitPassword()
            }

            is TdApi.AuthorizationStateReady -> {
                send(TdApi.LoadChats(TdApi.ChatListMain(), 30)) { }
            }

            else -> { /* Closing / Closed / LoggingOut — ничего не делаем */ }
        }
    }

    fun submitPhone(phone: String, onError: (String) -> Unit) {
        send(TdApi.SetAuthenticationPhoneNumber(phone, null)) { result ->
            if (result is TdApi.Error) mainHandler.post { onError(result.message) }
        }
    }

    fun submitCode(code: String, onError: (String) -> Unit) {
        send(TdApi.CheckAuthenticationCode(code)) { result ->
            if (result is TdApi.Error) mainHandler.post { onError(result.message) }
        }
    }

    fun submitPassword(password: String, onError: (String) -> Unit) {
        send(TdApi.CheckAuthenticationPassword(password)) { result ->
            if (result is TdApi.Error) mainHandler.post { onError(result.message) }
        }
    }

    fun loadHistory(chatId: Long, fromMessageId: Long, onResult: (List<TdApi.Message>) -> Unit) {
        send(TdApi.GetChatHistory(chatId, fromMessageId, 0, 30, false)) { result ->
            val list = if (result is TdApi.Messages) result.messages?.filterNotNull() ?: emptyList() else emptyList()
            mainHandler.post { onResult(list) }
        }
    }

    fun sendText(chatId: Long, text: String) {
        val content = TdApi.InputMessageText(TdApi.FormattedText(text, arrayOf()), null, true)
        send(TdApi.SendMessage(chatId, null, null, null, null, content)) { }
    }

    fun openChat(chatId: Long) {
        send(TdApi.OpenChat(chatId)) { }
    }

    fun closeChat(chatId: Long) {
        send(TdApi.CloseChat(chatId)) { }
    }

    private fun send(function: TdApi.Function<*>, handler: (TdApi.Object) -> Unit) {
        client?.send(function) { result -> handler(result) }
    }
}
