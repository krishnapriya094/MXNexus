package com.example.mxnexus.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.util.EmailSender
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AlumniApprovalsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvApprovals: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumni_approvals)

        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.alumniApprovalsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvApprovals = findViewById(R.id.rvAlumniApprovals)
        tvEmpty     = findViewById(R.id.tvNoApprovals)

        rvApprovals.layoutManager = LinearLayoutManager(this)
        loadPendingAlumni()
    }

    private fun loadPendingAlumni() {
        db.collection("users")
            .whereEqualTo("role", "Alumni")
            .whereEqualTo("status", "pending_approval")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { doc ->
                    val uid  = doc.id
                    val name = doc.getString("name")    ?: return@mapNotNull null
                    AlumniItem(
                        uid         = uid,
                        name        = name,
                        email       = doc.getString("email")       ?: "",
                        company     = doc.getString("company")     ?: "",
                        designation = doc.getString("designation") ?: "",
                        gradYear    = doc.getString("gradYear")    ?: "",
                        workType    = doc.getString("workType")    ?: ""
                    )
                }
                tvEmpty.visibility     = if (list.isEmpty()) View.VISIBLE else View.GONE
                rvApprovals.visibility = if (list.isEmpty()) View.GONE   else View.VISIBLE
                rvApprovals.adapter    = AlumniApprovalAdapter(list) { item, action ->
                    handleAction(item, action)
                }
            }
    }

    private fun handleAction(item: AlumniItem, action: String) {
        when (action) {
            "approve" -> {
                db.collection("users").document(item.uid)
                    .update("status", "approved")
                    .addOnSuccessListener {
                        Toast.makeText(this, "${item.name} approved!", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            try {
                                EmailSender.sendApprovalEmail(item.email, item.name)
                                Log.d("AlumniApprovals", "Approval email sent to ${item.email}")
                            } catch (e: Exception) {
                                Log.e("AlumniApprovals", "Email failed: ${e.message}")
                                // Email failing should not block the approval itself
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to approve", Toast.LENGTH_SHORT).show()
                    }
            }
            "reject" -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Reject ${item.name}?")
                    .setMessage("This will deny their access to MX Nexus. A rejection email will be sent.")
                    .setPositiveButton("Reject") { _, _ ->
                        db.collection("users").document(item.uid)
                            .update("status", "rejected")
                            .addOnSuccessListener {
                                Toast.makeText(this, "${item.name} rejected.", Toast.LENGTH_SHORT).show()
                                lifecycleScope.launch {
                                    try {
                                        EmailSender.sendRejectionEmail(item.email, item.name)
                                    } catch (e: Exception) {
                                        Log.e("AlumniApprovals", "Rejection email failed: ${e.message}")
                                    }
                                }
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}

data class AlumniItem(
    val uid: String,
    val name: String,
    val email: String,
    val company: String,
    val designation: String,
    val gradYear: String,
    val workType: String
)

class AlumniApprovalAdapter(
    private val items: List<AlumniItem>,
    private val onAction: (AlumniItem, String) -> Unit
) : RecyclerView.Adapter<AlumniApprovalAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitial:     TextView      = v.findViewById(R.id.tvApprovalInitial)
        val tvName:        TextView      = v.findViewById(R.id.tvApprovalName)
        val tvEmail:       TextView      = v.findViewById(R.id.tvApprovalEmail)
        val tvCompany:     TextView      = v.findViewById(R.id.tvApprovalCompany)
        val tvGradYear:    TextView      = v.findViewById(R.id.tvApprovalGradYear)
        val btnApprove:    MaterialButton = v.findViewById(R.id.btnApprove)
        val btnReject:     MaterialButton = v.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_alumni_approval, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvInitial.text  = item.name.firstOrNull()?.uppercase() ?: "A"
        holder.tvName.text     = item.name
        holder.tvEmail.text    = item.email
        holder.tvCompany.text  = if (item.company.isNotBlank())
            "${item.workType}: ${item.company} — ${item.designation}"
            else "No company info"
        holder.tvGradYear.text = if (item.gradYear.isNotBlank()) "Batch of ${item.gradYear}" else ""
        holder.btnApprove.setOnClickListener { onAction(item, "approve") }
        holder.btnReject.setOnClickListener  { onAction(item, "reject")  }
    }
}
