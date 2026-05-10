package com.example.mxnexus.ui.queries

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Answer
import com.example.mxnexus.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query as FirestoreQuery

class QueryDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private lateinit var tvUserName: TextView
    private lateinit var tvDetailInitial: TextView
    private lateinit var imgDetailAvatar: android.widget.ImageView
    private lateinit var tvQuestion: TextView
    private lateinit var rvAnswers: RecyclerView
    private lateinit var layoutAlumniInput: LinearLayout
    private lateinit var etAnswer: EditText
    private lateinit var btnSend: ImageButton
    
    private var queryId: String = ""
    private var queryOwnerId: String = ""
    private val answerList = mutableListOf<Answer>()
    private lateinit var adapter: AnswerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_query_detail)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        queryId = intent.getStringExtra("queryId") ?: ""

        val toolbar = findViewById<Toolbar>(R.id.queryToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvUserName = findViewById(R.id.tvDetailUserName)
        tvDetailInitial = findViewById(R.id.tvDetailInitial)
        imgDetailAvatar = findViewById(R.id.imgDetailAvatar)
        tvQuestion = findViewById(R.id.tvDetailQuestion)
        rvAnswers = findViewById(R.id.rvAnswers)
        layoutAlumniInput = findViewById(R.id.layoutAlumniInput)
        etAnswer = findViewById(R.id.etAnswer)
        btnSend = findViewById(R.id.btnSendAnswer)

        adapter = AnswerAdapter(answerList)
        rvAnswers.layoutManager = LinearLayoutManager(this)
        rvAnswers.adapter = adapter

        loadQueryDetails()
        loadAnswers()
        checkUserRole()

        btnSend.setOnClickListener { postAnswer() }
    }

    private fun loadQueryDetails() {
        db.collection("queries").document(queryId).get().addOnSuccessListener { doc ->
            if (doc != null) {
                val name = doc.getString("userName") ?: "User"
                tvUserName.text = name
                tvDetailInitial.text = name.firstOrNull()?.uppercase() ?: "U"
                tvQuestion.text = doc.getString("question")
                queryOwnerId = doc.getString("userId") ?: ""

                if (queryOwnerId.isNotEmpty()) {
                    db.collection("users").document(queryOwnerId).get().addOnSuccessListener { userDoc ->
                        if (userDoc != null && userDoc.exists()) {
                            val url = userDoc.getString("profileImageUrl")
                            if (!url.isNullOrEmpty()) {
                                imgDetailAvatar.visibility = View.VISIBLE
                                tvDetailInitial.visibility = View.GONE
                                com.bumptech.glide.Glide.with(this).load(url).circleCrop().into(imgDetailAvatar)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadAnswers() {
        db.collection("queries").document(queryId).collection("answers")
            .orderBy("timestamp", FirestoreQuery.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    answerList.clear()
                    answerList.addAll(snapshot.toObjects(Answer::class.java))
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun checkUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val role = doc.getString("role") ?: ""
            if (role == "Alumni" || role == "Admin") {
                layoutAlumniInput.visibility = View.VISIBLE
            }
        }
    }

    private fun postAnswer() {
        val text = etAnswer.text.toString().trim()
        if (text.isEmpty()) return

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val name = userDoc.getString("name") ?: "Alumni"
            val role = userDoc.getString("role") ?: "Alumni"
            val answerId = db.collection("queries").document(queryId).collection("answers").document().id

            val answer = Answer(
                answerId = answerId,
                queryId = queryId,
                userId = uid,
                userName = name,
                userRole = role,
                answerText = text,
                timestamp = com.google.firebase.Timestamp.now()
            )

            db.collection("queries").document(queryId).collection("answers").document(answerId).set(answer)
                .addOnSuccessListener {
                    etAnswer.text.clear()
                    sendAnswerAlert(name, text)
                    updateAnswerCount()
                }
        }
    }

    private fun sendAnswerAlert(alumniName: String, answerText: String) {
        if (auth.currentUser?.uid == queryOwnerId || queryOwnerId.isEmpty()) return
        NotificationHelper.sendQueryReplyNotification(
            queryOwnerId = queryOwnerId,
            senderName   = alumniName,
            queryId      = queryId,
            answerText   = if (answerText.length > 60) answerText.take(60) + "…" else answerText
        )
    }

    private fun updateAnswerCount() {
        val queryRef = db.collection("queries").document(queryId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(queryRef)
            val currentCount = snapshot.getLong("answerCount") ?: 0
            transaction.update(queryRef, "answerCount", currentCount + 1)
        }.addOnFailureListener {
            // Handle transaction failure
        }
    }
}
