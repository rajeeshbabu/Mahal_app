# Remaining DAOs That Need Sync Integration

The following DAOs need sync integration added to their create(), update(), and delete() methods:

1. ✅ EventDAO - DONE
2. ⚠️ MasjidDAO - Need to add sync
3. ⚠️ StaffDAO - Need to add sync  
4. ⚠️ StaffSalaryDAO - Need to add sync
5. ⚠️ CommitteeDAO - Need to add sync
6. ⚠️ HouseDAO - Need to add sync
7. ⚠️ IncomeTypeDAO - Need to add sync
8. ⚠️ DueTypeDAO - Need to add sync
9. ⚠️ PrayerTimeDAO - Need to add sync
10. ⚠️ CertificateDAO - Need to add sync (for marriage_certificates, death_certificates, jamath_certificates, custom_certificates)

## Pattern to Follow:

For each DAO, add:

1. Import: `import com.mahal.sync.SyncHelper;`

2. In create() method after getting newId:
```java
// Queue for sync if record was created successfully
if (newId != null) {
    object.setId(newId);
    SyncHelper.queueInsert("table_name", newId, object);
}
```

3. In update() method after success check:
```java
boolean success = db.executeUpdate(sql, params) > 0;

// Queue for sync if update was successful
if (success) {
    SyncHelper.queueUpdate("table_name", object.getId(), object);
}

return success;
```

4. In delete() method after success check:
```java
boolean success = db.executeUpdate(sql, new Object[]{id}) > 0;

// Queue for sync if delete was successful
if (success) {
    SyncHelper.queueDelete("table_name", id);
}

return success;
```


