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

    // Offline persistence enabled — reads work without internet after first fetch
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }

    private val eventsRef       = db.collection("events")
    private val deletedEventsRef = db.collection("deleted_events")
    private val itemsRef        = db.collection("master_items")
    private val takersRef       = db.collection("master_takers")

    // ── EVENTS ──────────────────────────────────────────────────────

    fun getEventsFlow(): Flow<List<Event>> = callbackFlow {
        val listener = eventsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error fetching events", error)
                return@addSnapshotListener
            }
            val events = snapshot?.documents?.mapNotNull { it.toObject(Event::class.java) } ?: emptyList()
            trySend(events.sortedByDescending { it.id })
        }
        awaitClose { listener.remove() }
    }

    fun saveEvent(event: Event) {
        eventsRef.document(event.id.toString()).set(event)
            .addOnFailureListener { e -> Log.e("FirestoreManager", "Failed to save event ${event.id}", e) }
    }

    // ── TRASH BIN ────────────────────────────────────────────────────

    /** Soft-delete: moves event from 'events' → 'deleted_events' atomically */
    fun moveToTrash(eventId: Long) {
        val docId = eventId.toString()
        eventsRef.document(docId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val event = document.toObject(Event::class.java) ?: return@addOnSuccessListener
                db.runBatch { batch ->
                    batch.set(deletedEventsRef.document(docId), event)
                    batch.delete(eventsRef.document(docId))
                }.addOnFailureListener { e ->
                    Log.e("FirestoreManager", "Failed to move event $eventId to trash", e)
                }
            }
        }
    }

    fun getDeletedEventsFlow(): Flow<List<Event>> = callbackFlow {
        val listener = deletedEventsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error fetching deleted events", error)
                return@addSnapshotListener
            }
            val deletedEvents = snapshot?.documents?.mapNotNull { it.toObject(Event::class.java) } ?: emptyList()
            trySend(deletedEvents.sortedByDescending { it.id })
        }
        awaitClose { listener.remove() }
    }

    /** Restore: moves event from 'deleted_events' → 'events' atomically */
    fun restoreEvent(eventId: Long) {
        val docId = eventId.toString()
        deletedEventsRef.document(docId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val event = document.toObject(Event::class.java) ?: return@addOnSuccessListener
                db.runBatch { batch ->
                    batch.set(eventsRef.document(docId), event)
                    batch.delete(deletedEventsRef.document(docId))
                }.addOnFailureListener { e ->
                    Log.e("FirestoreManager", "Failed to restore event $eventId", e)
                }
            }
        }
    }

    /** Hard delete from trash — cannot be undone */
    fun permanentlyDeleteEvent(eventId: Long) {
        deletedEventsRef.document(eventId.toString()).delete()
            .addOnFailureListener { e -> Log.e("FirestoreManager", "Failed to permanently delete event $eventId", e) }
    }

    // ── MASTER DATA ──────────────────────────────────────────────────

    fun getMasterItemsFlow(): Flow<List<MasterItem>> = callbackFlow {
        val listener = itemsRef.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.documents?.mapNotNull { it.toObject(MasterItem::class.java) } ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    fun saveMasterItem(item: MasterItem) {
        itemsRef.document(item.id.toString()).set(item)
            .addOnFailureListener { e -> Log.e("FirestoreManager", "Failed to save master item ${item.id}", e) }
    }

    fun getMasterTakersFlow(): Flow<List<MasterTaker>> = callbackFlow {
        val listener = takersRef.addSnapshotListener { snapshot, _ ->
            val takers = snapshot?.documents?.mapNotNull { it.toObject(MasterTaker::class.java) } ?: emptyList()
            trySend(takers)
        }
        awaitClose { listener.remove() }
    }

    fun saveMasterTaker(taker: MasterTaker) {
        takersRef.document(taker.id.toString()).set(taker)
            .addOnFailureListener { e -> Log.e("FirestoreManager", "Failed to save master taker ${taker.id}", e) }
    }

    /** Hard-deletes a master taker document from Firestore. */
    fun deleteMasterTaker(id: Long) {
        takersRef.document(id.toString()).delete()
            .addOnFailureListener { e -> Log.e("FirestoreManager", "Failed to delete master taker $id", e) }
    }

    // ── MIGRATION ────────────────────────────────────────────────────
    /**
     * Uploads legacy SharedPrefs data to Firestore.
     *
     * BUG-20 FIX: Firestore batch() is hard-limited to 500 operations per call.
     * This function chunks all writes into batches of 499 and commits them
     * sequentially, so migration never crashes regardless of data size.
     */
    suspend fun migrateLegacyData(
        legacyEvents: List<Event>,
        legacyItems: List<MasterItem>,
        legacyTakers: List<MasterTaker>
    ): Boolean {
        return try {
            // Build a flat list of (collectionRef, documentId, data) write-ops
            data class WriteOp(val collection: com.google.firebase.firestore.CollectionReference, val id: String, val data: Any)

            val allOps = mutableListOf<WriteOp>()
            legacyEvents.forEach  { allOps.add(WriteOp(eventsRef,  it.id.toString(), it)) }
            legacyItems.forEach   { allOps.add(WriteOp(itemsRef,   it.id.toString(), it)) }
            legacyTakers.forEach  { allOps.add(WriteOp(takersRef,  it.id.toString(), it)) }

            if (allOps.isEmpty()) return true

            // Chunk into batches of 499 (Firestore max is 500; keep one slot of headroom)
            val chunkSize = 499
            allOps.chunked(chunkSize).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { op ->
                    batch.set(op.collection.document(op.id), op.data)
                }
                batch.commit().await()
            }

            Log.i("FirestoreManager", "Migration complete — ${allOps.size} documents written in ${(allOps.size + chunkSize - 1) / chunkSize} batch(es).")
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Migration failed", e)
            false
        }
    }
}