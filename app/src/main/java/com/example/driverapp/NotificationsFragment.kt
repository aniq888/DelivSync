package com.example.driverapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.driverapp.models.Notification
import com.example.driverapp.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {
    private val notificationRepository = NotificationRepository()
    private var notificationsList = mutableListOf<Notification>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewNotifications)
        emptyStateText = view.findViewById(R.id.tvEmptyState)

        playPageAnimations(view)

        adapter = NotificationsAdapter(notificationsList) { notification ->
            // Mark as read when clicked
            if (!notification.read) {
                CoroutineScope(Dispatchers.Main).launch {
                    notificationRepository.markAsRead(notification.id)
                    loadNotifications()
                }
            }

            // Navigate to delivery details if deliveryId is present
            if (notification.deliveryId.isNotEmpty()) {
                val fragment = DeliveryDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putString("deliveryId", notification.deliveryId)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Setup toolbar
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Mark all as read button
        view.findViewById<View>(R.id.btnMarkAllRead)?.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    notificationRepository.markAllAsRead(userId)
                    loadNotifications()
                    Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadNotifications()
        } else {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val notifications = notificationRepository.getNotificationsForDriver(userId)
                notificationsList.clear()
                notificationsList.addAll(notifications)
                adapter.notifyDataSetChanged()
                
                // Show/hide empty state
                if (notificationsList.isEmpty()) {
                    emptyStateText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading notifications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private class NotificationsAdapter(
        private val notifications: List<Notification>,
        private val onItemClick: (Notification) -> Unit
    ) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            holder.bind(notifications[position], onItemClick)
        }

        override fun getItemCount() = notifications.size

        class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.tvNotificationTitle)
            private val messageText: TextView = itemView.findViewById(R.id.tvNotificationMessage)
            private val timeText: TextView = itemView.findViewById(R.id.tvNotificationTime)
            private val typeIndicator: View = itemView.findViewById(R.id.viewTypeIndicator)

            fun bind(notification: Notification, onItemClick: (Notification) -> Unit) {
                titleText.text = notification.title
                messageText.text = notification.message
                
                // Format timestamp
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                timeText.text = dateFormat.format(Date(notification.timestamp))

                // Set type indicator color
                val color = when (notification.type) {
                    com.example.driverapp.models.NotificationType.NEW_DELIVERY -> 0xFF10A87D.toInt()
                    com.example.driverapp.models.NotificationType.DELIVERY_UPDATE -> 0xFF2196F3.toInt()
                    com.example.driverapp.models.NotificationType.URGENT_ALERT -> 0xFFFF5722.toInt()
                }
                typeIndicator.setBackgroundColor(color)

                // Set background based on read status
                itemView.setBackgroundColor(
                    if (notification.read) 0xFFFFFFFF.toInt() else 0xFFF0F9F7.toInt()
                )

                itemView.setOnClickListener {
                    onItemClick(notification)
                }
            }
        }
    }

    private fun playPageAnimations(view: View) {
        val slideUpFade = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

        // Animate toolbar
        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.startAnimation(slideUpFade)

        // Animate header section
        view.findViewById<ViewGroup>(R.id.headerSection)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 100
            it.startAnimation(anim)
        }

        // Animate recycler view
        recyclerView?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 200
            it.startAnimation(anim)
        }
    }
}

