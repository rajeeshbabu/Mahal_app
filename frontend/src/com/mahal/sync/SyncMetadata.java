package com.mahal.sync;

import java.time.Instant;

/**
 * Sync metadata fields that should be present in all syncable entities.
 * These fields enable proper conflict resolution and incremental sync.
 */
public interface SyncMetadata {
    /**
     * Unique identifier across all clients (UUID).
     */
    String getId();
    void setId(String id);
    
    /**
     * User who owns this record (JWT user_id from token).
     */
    String getUserId();
    void setUserId(String userId);
    
    /**
     * Timestamp of last modification (UTC).
     * Used for conflict resolution (last-write-wins).
     */
    Instant getUpdatedAt();
    void setUpdatedAt(Instant updatedAt);
    
    /**
     * Timestamp of creation (UTC).
     */
    Instant getCreatedAt();
    void setCreatedAt(Instant createdAt);
    
    /**
     * Whether this record has been synced to cloud.
     * false = local-only, not yet synced
     * true = synced to cloud (may still need to sync if updated locally)
     */
    boolean isSynced();
    void setSynced(boolean synced);
    
    /**
     * Sync version number (incremented on each sync conflict resolution).
     * Used for optimistic locking.
     */
    Long getSyncVersion();
    void setSyncVersion(Long version);
}
