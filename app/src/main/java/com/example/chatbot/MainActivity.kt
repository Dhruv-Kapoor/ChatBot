package com.example.chatbot

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.android.synthetic.main.activity_chat.*

class MainActivity : AppCompatActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }
    private val list by lazy {
        viewModel.getList()
    }
    private val adapter by lazy {
        ChatAdapter(supportFragmentManager, list)
    }
    private val classifier by lazy {
        Classifier(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmojiManager.install(GoogleEmojiProvider())
        setContentView(R.layout.activity_chat)

        rvMessages.adapter = adapter

        btnSend.setOnClickListener {
            val userInput = etMsg.text.toString()
            etMsg.setText("")
            addUserMessage(userInput)
            processUserInput(userInput)
        }
        btnEmoji.setOnClickListener {
            val inputMethodManager= getSystemService(InputMethodManager::class.java)
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

    }

    private fun processUserInput(userInput: String) {
        val response = classifier.chatBotResponse(userInput)
        if (response == null) {
            Toast.makeText(this, "Still loading data", Toast.LENGTH_SHORT).show()
            return
        }
        if (response == "report") {
            addReportMessage()
        } else {
            addBotMessage(response)
        }

    }

    private fun addReportMessage() {
        list.add(ReportMessage())
        adapter.notifyItemInserted(list.size - 1)
        rvMessages.scrollToPosition(list.size - 1)
    }

    private fun addBotMessage(message: String) {
        list.add(NormalMessage(message, false, System.currentTimeMillis()))
        adapter.notifyItemInserted(list.size - 1)
        rvMessages.scrollToPosition(list.size - 1)
    }

    private fun addUserMessage(message: String) {
        list.add(NormalMessage(message, true, System.currentTimeMillis()))
        adapter.notifyItemInserted(list.size - 1)
        rvMessages.scrollToPosition(list.size - 1)
    }

    override fun onStart() {
        super.onStart()
        classifier
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.destroy()
    }


}
