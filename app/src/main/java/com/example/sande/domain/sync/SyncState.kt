package com.example.sande.domain.sync

/**
 * Synchronisation state of a locally recorded operation.
 *
 * The blueprint requires that "server-accepted" is distinguishable from
 * "written to the local queue". A sale that Firestore has accepted into its
 * offline cache is still LOCAL_PENDING until the trusted server has confirmed
 * it.
 */
enum class SyncState {
    /** Recorded on device only. Not yet handed to the remote layer. */
    LOCAL_PENDING,

    /** Handed to the remote layer, awaiting confirmation. */
    UPLOADING,

    /** Trusted server accepted and applied the operation. */
    SERVER_ACCEPTED,

    /** Upload failed. May be retried. */
    SYNC_FAILED,

    /** Conflict detected. Requires human review. Never discarded silently. */
    REQUIRES_REVIEW;

    val isTerminal: Boolean get() = this == SERVER_ACCEPTED
    val isPending: Boolean get() = this == LOCAL_PENDING || this == UPLOADING
    val isProblem: Boolean get() = this == SYNC_FAILED || this == REQUIRES_REVIEW
}
