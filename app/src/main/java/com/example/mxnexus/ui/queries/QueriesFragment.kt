package com.example.mxnexus.ui.queries

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Query
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query as FirestoreQuery

class QueriesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvQueries: RecyclerView
    private lateinit var adapter: QueryAdapter
    private val queryList = mutableListOf<Query>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_queries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvQueries = view.findViewById(R.id.rvQueries)
        rvQueries.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = QueryAdapter(queryList) { query ->
            val intent = Intent(requireContext(), QueryDetailActivity::class.java)
            intent.putExtra("queryId", query.queryId)
            startActivity(intent)
        }
        rvQueries.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAskQuery).setOnClickListener {
            showAskQueryDialog()
        }

        loadQueries()
    }

    private fun loadQueries() {
        db.collection("queries")
            .orderBy("timestamp", FirestoreQuery.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val queries = snapshot.toObjects(Query::class.java)
                    adapter.updateQueries(queries)
                }
            }
    }

    private fun showAskQueryDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Type your question here..."
            setPadding(48, 40, 48, 40)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Ask a Question")
            .setView(editText)
            .setPositiveButton("Post") { _, _ ->
                val question = editText.text.toString().trim()
                if (question.isNotEmpty()) postQuery(question)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun postQuery(questionText: String) {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: "Anonymous"
            val queryId = db.collection("queries").document().id
            
            val newQuery = Query(
                queryId = queryId,
                userId = uid,
                userName = userName,
                question = questionText,
                timestamp = com.google.firebase.Timestamp.now()
            )

            db.collection("queries").document(queryId).set(newQuery)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Question posted!", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
