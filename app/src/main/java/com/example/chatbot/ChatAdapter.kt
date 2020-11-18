package com.example.chatbot

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.list_item_chat_recv_message.view.*
import kotlinx.android.synthetic.main.list_item_chat_recv_message.view.tvTime
import kotlinx.android.synthetic.main.report_message.view.*
import java.util.*

class ChatAdapter(val supportFragmentManager: FragmentManager, val list: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = { layout: Int ->
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
        }
        return when (viewType) {
            TEXT_MESSAGE_RECEIVED -> NormalMessageViewHolder(inflate(R.layout.list_item_chat_recv_message))
            TEXT_MESSAGE_SENT -> NormalMessageViewHolder(inflate(R.layout.list_item_chat_sent_message))
            REPORT_MESSAGE -> ReportMessageViewHolder(inflate(R.layout.report_message))
            else -> EmptyViewHolder(inflate(R.layout.empty_view))
        }
    }


    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int {
        return when (val message = list[position]) {
            is NormalMessage -> {
                if (message.sent) {
                    TEXT_MESSAGE_SENT
                } else {
                    TEXT_MESSAGE_RECEIVED
                }
            }
            is ReportMessage -> {
                REPORT_MESSAGE
            }
            else -> UNSUPPORTED
        }

    }

    companion object {
        private const val TEXT_MESSAGE_RECEIVED = 0
        private const val TEXT_MESSAGE_SENT = 1
        private const val REPORT_MESSAGE = 2
        private const val UNSUPPORTED = -1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalMessageViewHolder -> {
                holder.bind(list[position] as NormalMessage)
            }
            is ReportMessageViewHolder -> {
                holder.bind(list[position] as ReportMessage)
            }
        }
    }

}

class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NormalMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(message: NormalMessage) {
        with(itemView) {
            tvMessage.text = message.message
            tvTime.text = Date(message.time).formatAsTime()
        }
    }
}

class ReportMessageViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    private val database by lazy {
        FirebaseDatabase.getInstance().reference
    }
    fun bind(message: ReportMessage) {
        with(itemView){

            tvTime.text = Date(message.time).formatAsTime()

            failLayout.visibility = View.GONE
            if(message.isFetched){
                successLayout.visibility = View.VISIBLE
                detailsLayout.visibility = View.GONE
                if(message.isPositive){
                    tvResult.text = "POSITIVE"
                    tvResult.setTextColor(getColor(context, R.color.red))
                }else{
                    tvResult.text = "NEGATIVE"
                    tvResult.setTextColor(getColor(context, R.color.green))
                }
            }else{
                successLayout.visibility = View.GONE
                detailsLayout.visibility = View.VISIBLE
            }

            btnSubmit.setOnClickListener {
                val hosId = etHospitalId.text.toString()
                val opdId = etOpdId.text.toString()
                val phone = etPhoneNo.text.toString()
                message.hospitalId = hosId
                message.opdId = opdId
                message.phoneNo = phone
                database.child(hosId).child(opdId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value == null) {
                                message.isFetched = false
                            } else {
                                if(message.phoneNo == snapshot.child("phone").getValue(Long::class.java).toString()){
                                    message.isPositive = snapshot.child("isPositive").getValue(Boolean::class.java)?:false
                                    message.pdfLink = snapshot.child("pdf").getValue(String::class.java)?:""
                                    message.isFetched = true
                                    showResult(true)
                                }else{
                                    message.isFetched=false
                                    showResult(false)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("TAG", "onCancelled: ${error.details} ${error.message} ${error.code}")
                            message.isFetched = false
                            showResult(false)
                        }

                        fun showResult(isFetched: Boolean){
                            detailsLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_left).apply {
                                setAnimationListener(object: Animation.AnimationListener{
                                    override fun onAnimationStart(animation: Animation?) {

                                    }

                                    override fun onAnimationEnd(animation: Animation?) {
                                        detailsLayout.visibility = View.GONE
                                    }

                                    override fun onAnimationRepeat(animation: Animation?) {
                                    }

                                })
                            })
                            if (isFetched){
                                successLayout.visibility = View.VISIBLE
                                successLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right))
                                if(message.isPositive){
                                    tvResult.text = "POSITIVE"
                                    tvResult.setTextColor(getColor(context, R.color.red))
                                }else{
                                    tvResult.text = "NEGATIVE"
                                    tvResult.setTextColor(getColor(context, R.color.green))
                                }
                            }else{
                                failLayout.visibility = View.VISIBLE
                                failLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right))
                            }

                        }

                    })



            }

            btnRetry.setOnClickListener {
                failLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_right).apply {
                    setAnimationListener(object: Animation.AnimationListener{
                        override fun onAnimationStart(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            failLayout.visibility = View.GONE
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                        }

                    })
                })
                detailsLayout.visibility = View.VISIBLE
                detailsLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_left))
            }

            btnDownload.setOnClickListener {
                context.startService(DownloadPdfService.getIntent(context, message.pdfLink, message.opdId+".pdf"))
            }
        }
    }

}



