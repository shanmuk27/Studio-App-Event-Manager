package com.example.supriyadigitalproducerfinal

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreManager {
    // Initialize Firestore with Offline Persistence
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }

    // Collection References
    private val eventsRef = db.collection("events")
    private val deletedEventsRef = db.collection("deleted_events") // New Trash Bin collection
    private val itemsRef = db.collection("master_items")
    private val takersRef = db.collection("master_takers")

    // --- EVENTS LOGIC ---
    fun getEventsFlow(): Flow<List<Event>> = callbackFlow {
        val listener = eventsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error fetching events", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val events = snapshot.documents.mapNotNull { it.toObject(Event::class.java) }
                trySend(events.sortedByDescending { it.id })
            }
        }
        awaitClose { listener.remove() }
    }

    fun saveEvent(event: Event) {
        eventsRef.document(event.id.toString()).set(event)
    }

    // --- TRASH BIN (SOFT DELETE) LOGIC ---

    // Moves an event from the active 'events' collection to 'deleted_events'
    fun moveToTrash(eventId: Long) {
        val docId = eventId.toString()
        eventsRef.document(docId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val event = document.toObject(Event::class.java)
                if (event != null) {
                    db.runBatch { batch ->
                        batch.set(deletedEventsRef.document(docId), event)
                        batch.delete(eventsRef.document(docId))
                    }.addOnFailureListener { e ->
                        Log.e("FirestoreManager", "Failed to move event to trash", e)
                    }
                }
            }
        }
    }

    // Listens to the 'deleted_events' collection for the Trash Bin UI
    fun getDeletedEventsFlow(): Flow<List<Event>> = callbackFlow {
        val listener = deletedEventsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error fetching deleted events", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val deletedEvents = snapshot.documents.mapNotNull { it.toObject(Event::class.java) }
                trySend(deletedEvents.sortedByDescending { it.id })
            }
        }
        awaitClose { listener.remove() }
    }

    // Moves an event back from 'deleted_events' to the active 'events' collection
    fun restoreEvent(eventId: Long) {
        val docId = eventId.toString()
        deletedEventsRef.document(docId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val event = document.toObject(Event::class.java)
                if (event != null) {
                    db.runBatch { batch ->
                        batch.set(eventsRef.document(docId), event)
                        batch.delete(deletedEventsRef.document(docId))
                    }.addOnFailureListener { e ->
                        Log.e("FirestoreManager", "Failed to restore event", e)
                    }
                }
            }
        }
    }

    // Completely erases the event from the database
    fun permanentlyDeleteEvent(eventId: Long) {
        deletedEventsRef.document(eventId.toString()).delete().addOnFailureListener { e ->
            Log.e("FirestoreManager", "Failed to permanently delete event", e)
        }
    }

    // --- MASTER DATA LOGIC ---
    fun getMasterItemsFlow() = callbackFlow {
        val l = itemsRef.addSnapshotListener { s, _ ->
            val items = s?.documents?.mapNotNull { it.toObject(MasterItem::class.java) } ?: emptyList()
            trySend(items)
        }
        awaitClose { l.remove() }
    }

    fun saveMasterItem(item: MasterItem) {
        itemsRef.document(item.id.toString()).set(item)
    }

    fun getMasterTakersFlow() = callbackFlow {
        val l = takersRef.addSnapshotListener { s, _ ->
            val takers = s?.documents?.mapNotNull { it.toObject(MasterTaker::class.java) } ?: emptyList()
            trySend(takers)
        }
        awaitClose { l.remove() }
    }

    fun saveMasterTaker(taker: MasterTaker) {
        takersRef.document(taker.id.toString()).set(taker)
    }

    // --- MIGRATION LOGIC (Uploads old local data to cloud) ---
    suspend fun migrateLegacyData(
        legacyEvents: List<Event>,
        legacyItems: List<MasterItem>,
        legacyTakers: List<MasterTaker>
    ): Boolean {
        return try {
            val batch = db.batch()

            legacyEvents.forEach { event ->
                val docRef = eventsRef.document(event.id.toString())
                batch.set(docRef, event)
            }
            legacyItems.forEach { item ->
                val docRef = itemsRef.document(item.id.toString())
                batch.set(docRef, item)
            }
            legacyTakers.forEach { taker ->
                val docRef = takersRef.document(taker.id.toString())
                batch.set(docRef, taker)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Migration failed", e)
            false
        }
    }
}