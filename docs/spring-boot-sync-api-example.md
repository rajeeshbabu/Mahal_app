# Spring Boot REST API Examples for Sync

## Controller: SyncController.java

```java
package com.mahal.api.controller;

import com.mahal.api.dto.*;
import com.mahal.api.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    
    private final SyncService syncService;
    
    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }
    
    /**
     * Upload local changes to cloud (push sync).
     * Client sends batch of records that need to be synced.
     * 
     * POST /api/sync/upload
     * Authorization: Bearer <JWT>
     * 
     * Body: {
     *   "table": "members",
     *   "records": [
     *     {
     *       "id": "uuid-here",
     *       "name": "John Doe",
     *       "updated_at": "2025-12-19T10:30:00Z",
     *       ...
     *     }
     *   ]
     * }
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @AuthenticationPrincipal String userId, // From JWT
            @RequestBody UploadRequest request) {
        
        List<SyncResult> results = syncService.uploadRecords(
            userId,
            request.getTable(),
            request.getRecords()
        );
        
        return ResponseEntity.ok(new UploadResponse(results));
    }
    
    /**
     * Download changes from cloud (pull sync).
     * Client requests records modified after lastSyncTime.
     * 
     * GET /api/sync/download?table=members&lastSyncTime=2025-12-19T10:00:00Z
     * Authorization: Bearer <JWT>
     */
    @GetMapping("/download")
    public ResponseEntity<DownloadResponse> download(
            @AuthenticationPrincipal String userId, // From JWT
            @RequestParam String table,
            @RequestParam(required = false) String lastSyncTime) {
        
        List<RecordDTO> records = syncService.downloadRecords(
            userId,
            table,
            lastSyncTime != null ? Instant.parse(lastSyncTime) : null
        );
        
        return ResponseEntity.ok(new DownloadResponse(table, records));
    }
    
    /**
     * Bidirectional sync (upload + download in one request).
     * 
     * POST /api/sync/bidirectional
     * Authorization: Bearer <JWT>
     */
    @PostMapping("/bidirectional")
    public ResponseEntity<BidirectionalSyncResponse> bidirectionalSync(
            @AuthenticationPrincipal String userId,
            @RequestBody BidirectionalSyncRequest request) {
        
        // Upload local changes
        List<SyncResult> uploadResults = syncService.uploadRecords(
            userId, request.getTable(), request.getRecordsToUpload()
        );
        
        // Download cloud changes
        List<RecordDTO> downloadedRecords = syncService.downloadRecords(
            userId,
            request.getTable(),
            request.getLastSyncTime() != null ? 
                Instant.parse(request.getLastSyncTime()) : null
        );
        
        return ResponseEntity.ok(new BidirectionalSyncResponse(
            uploadResults,
            downloadedRecords,
            Instant.now().toString()
        ));
    }
}
```

## Service: SyncService.java

```java
package com.mahal.api.service;

import com.mahal.api.dto.*;
import com.mahal.api.entity.Member;
import com.mahal.api.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SyncService {
    
    private final MemberRepository memberRepository;
    // Add other repositories as needed
    
    public SyncService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    
    /**
     * Upload records from client to cloud database.
     * Implements conflict resolution: last-write-wins using updated_at.
     */
    @Transactional
    public List<SyncResult> uploadRecords(String userId, String table, List<Map<String, Object>> records) {
        List<SyncResult> results = new ArrayList<>();
        
        for (Map<String, Object> recordData : records) {
            try {
                String recordId = (String) recordData.get("id");
                Instant clientUpdatedAt = Instant.parse((String) recordData.get("updated_at"));
                
                // Ensure user_id matches (security check)
                String recordUserId = (String) recordData.get("user_id");
                if (!userId.equals(recordUserId)) {
                    results.add(SyncResult.error(recordId, "User ID mismatch"));
                    continue;
                }
                
                // Get existing record from database
                Member existing = memberRepository.findByIdAndUserId(recordId, userId)
                    .orElse(null);
                
                if (existing == null) {
                    // New record - insert
                    Member newMember = mapToEntity(recordData, userId);
                    memberRepository.save(newMember);
                    results.add(SyncResult.success(recordId, "INSERTED"));
                } else {
                    // Existing record - conflict resolution
                    Instant serverUpdatedAt = existing.getUpdatedAt();
                    
                    if (clientUpdatedAt.isAfter(serverUpdatedAt)) {
                        // Client version is newer - update with client data
                        updateEntity(existing, recordData);
                        memberRepository.save(existing);
                        results.add(SyncResult.success(recordId, "UPDATED"));
                    } else if (clientUpdatedAt.equals(serverUpdatedAt)) {
                        // Same timestamp - already in sync
                        results.add(SyncResult.success(recordId, "NO_CHANGE"));
                    } else {
                        // Server version is newer - reject client update
                        // (client should pull server version in download)
                        results.add(SyncResult.conflict(recordId, "SERVER_VERSION_NEWER"));
                    }
                }
            } catch (Exception e) {
                results.add(SyncResult.error(recordId, e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * Download records modified after lastSyncTime.
     * Only returns records for the authenticated user.
     */
    public List<RecordDTO> downloadRecords(String userId, String table, Instant lastSyncTime) {
        List<Member> records;
        
        if (lastSyncTime != null) {
            // Incremental sync: only records modified after lastSyncTime
            records = memberRepository.findByUserIdAndUpdatedAtAfter(userId, lastSyncTime);
        } else {
            // Full sync: all records for user
            records = memberRepository.findByUserId(userId);
        }
        
        return records.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    private Member mapToEntity(Map<String, Object> data, String userId) {
        Member member = new Member();
        member.setId((String) data.get("id"));
        member.setUserId(userId); // Ensure user_id matches authenticated user
        member.setName((String) data.get("name"));
        // ... map other fields
        member.setUpdatedAt(Instant.parse((String) data.get("updated_at")));
        member.setCreatedAt(data.containsKey("created_at") ? 
            Instant.parse((String) data.get("created_at")) : Instant.now());
        return member;
    }
    
    private void updateEntity(Member existing, Map<String, Object> data) {
        existing.setName((String) data.get("name"));
        // ... update other fields
        existing.setUpdatedAt(Instant.parse((String) data.get("updated_at")));
    }
    
    private RecordDTO mapToDTO(Member member) {
        RecordDTO dto = new RecordDTO();
        dto.setId(member.getId());
        dto.setUserId(member.getUserId());
        dto.setName(member.getName());
        // ... map other fields
        dto.setUpdatedAt(member.getUpdatedAt().toString());
        dto.setCreatedAt(member.getCreatedAt().toString());
        return dto;
    }
}
```

## DTOs

```java
// UploadRequest.java
package com.mahal.api.dto;

import java.util.List;
import java.util.Map;

public class UploadRequest {
    private String table;
    private List<Map<String, Object>> records;
    // getters/setters
}

// DownloadResponse.java
package com.mahal.api.dto;

import java.util.List;

public class DownloadResponse {
    private String table;
    private List<RecordDTO> records;
    private String syncTime; // ISO 8601 UTC
    
    // constructors/getters/setters
}

// SyncResult.java
package com.mahal.api.dto;

public class SyncResult {
    private String recordId;
    private String status; // SUCCESS, ERROR, CONFLICT
    private String message;
    
    public static SyncResult success(String recordId, String message) {
        SyncResult r = new SyncResult();
        r.recordId = recordId;
        r.status = "SUCCESS";
        r.message = message;
        return r;
    }
    
    public static SyncResult conflict(String recordId, String message) {
        SyncResult r = new SyncResult();
        r.recordId = recordId;
        r.status = "CONFLICT";
        r.message = message;
        return r;
    }
    
    public static SyncResult error(String recordId, String message) {
        SyncResult r = new SyncResult();
        r.recordId = recordId;
        r.status = "ERROR";
        r.message = message;
        return r;
    }
    // getters/setters
}

// RecordDTO.java
package com.mahal.api.dto;

public class RecordDTO {
    private String id;
    private String userId;
    private String name;
    // ... other fields
    private String updatedAt; // ISO 8601 UTC
    private String createdAt; // ISO 8601 UTC
    // getters/setters
}
```

## Repository Example

```java
package com.mahal.api.repository;

import com.mahal.api.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
    
    // Find by user_id (security: user isolation)
    List<Member> findByUserId(String userId);
    
    // Find by user_id and updated_at > timestamp (incremental sync)
    List<Member> findByUserIdAndUpdatedAtAfter(String userId, Instant updatedAt);
    
    // Find by id AND user_id (security: ensure user owns record)
    Optional<Member> findByIdAndUserId(String id, String userId);
}
```

## Entity Example (JPA)

```java
package com.mahal.api.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "members")
public class Member {
    
    @Id
    @Column(name = "id", length = 36) // UUID length
    private String id; // UUID
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId; // JWT user_id - CRITICAL for isolation
    
    @Column(name = "name", nullable = false)
    private String name;
    
    // ... other fields
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // UTC timestamp - CRITICAL for conflict resolution
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt; // UTC timestamp
    
    @Column(name = "sync_version", nullable = false)
    private Long syncVersion = 0L; // Optimistic locking
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        syncVersion++;
    }
    
    // getters/setters
}
```
