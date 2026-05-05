package com.example.mxnexus.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mxnexus.R

/**
 * Fragment that acts as a bridge to launch the Java-based AlertsActivity.
 */
class NotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // This fragment will immediately launch the AlertsActivity
        val intent = Intent(requireContext(), AlertsActivity::class.java)
        startActivity(intent)
        
        // We can return a simple view or the activity will just cover the screen
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }
}
