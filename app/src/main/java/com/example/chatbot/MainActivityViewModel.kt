package com.example.chatbot

import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {

    private val list = ArrayList<Message>()

    fun getList(): ArrayList<Message> = list
}