package com.ilvir.telegramwatch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.drinkless.tdlib.TdApi

class LoginActivity : AppCompatActivity(), TdlibClient.AuthListener {

    private lateinit var statusText: TextView
    private lateinit var inputField: EditText
    private lateinit var btnNext: Button

    private enum class Step { PHONE, CODE, PASSWORD, DONE }
    private var step = Step.PHONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        statusText = findViewById(R.id.status_text)
        inputField = findViewById(R.id.input_field)
        btnNext = findViewById(R.id.btn_next)

        btnNext.setOnClickListener { onNextClicked() }
    }

    override fun onStart() {
        super.onStart()
        TdlibClient.addAuthListener(this)
    }

    override fun onStop() {
        super.onStop()
        TdlibClient.removeAuthListener(this)
    }

    private fun onNextClicked() {
        val value = inputField.text.toString().trim()
        if (value.isEmpty()) return

        when (step) {
            Step.PHONE -> TdlibClient.submitPhone(value) { showError(it) }
            Step.CODE -> TdlibClient.submitCode(value) { showError(it) }
            Step.PASSWORD -> TdlibClient.submitPassword(value) { showError(it) }
            Step.DONE -> {}
        }
        inputField.text.clear()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                step = Step.PHONE
                statusText.text = getString(R.string.hint_phone)
                inputField.hint = getString(R.string.hint_phone)
                inputField.inputType = android.text.InputType.TYPE_CLASS_PHONE
            }

            is TdApi.AuthorizationStateWaitCode -> {
                step = Step.CODE
                statusText.text = getString(R.string.hint_code)
                inputField.hint = getString(R.string.hint_code)
                inputField.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                step = Step.PASSWORD
                statusText.text = getString(R.string.hint_password)
                inputField.hint = getString(R.string.hint_password)
                inputField.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            is TdApi.AuthorizationStateReady -> {
                step = Step.DONE
                startActivity(Intent(this, ChatListActivity::class.java))
                finish()
            }

            else -> { /* промежуточные состояния (WaitTdlibParameters и т.п.) */ }
        }
    }
}
