package com.example.chatbot

abstract class Message

data class NormalMessage (
    val message: String,
    val sent: Boolean,
    val time: Long
): Message() {
    constructor(): this("",true, System.currentTimeMillis())
}

data class ReportMessage(
    var hospitalId: String,
    var opdId: String,
    var phoneNo: String,
    var isFetched: Boolean,
    var isPositive: Boolean,
    var pdfLink: String,
    val time: Long
):Message(){
    constructor(): this("","","",false, false,"", System.currentTimeMillis())
}