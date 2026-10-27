package vgu.sales.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vgu.sales.database.StaffDao;
import vgu.sales.database.StaffRecord;

/**
 * In-memory test double for StaffDao.
 * Package-private: used only in controller unit tests.
 */
class FakeStaffDao implements StaffDao {

    private final Map<Integer, StaffRecord> records = new HashMap<>();
    private int nextId = 1;

    @Override
    public Optional<StaffRecord> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(records.get(id));
    }

    @Override
    public List<StaffRecord> findAll() {
        return new ArrayList<>(records.values());
    }

    @Override
    public StaffRecord insert(StaffRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Record must not be null.");
        }
        if (record.staffId() != null) {
            throw new IllegalArgumentException("Cannot insert record with non-null id.");
        }
        int id = nextId++;
        String lowerUsername = record.username() != null ? record.username().toLowerCase() : null;
        StaffRecord saved = new StaffRecord(
            id,
            lowerUsername,
            record.passwordHash(),
            record.passwordSalt(),
            record.fullName(),
            record.email(),
            record.phone(),
            record.role(),
            record.active(),
            LocalDateTime.now()
        );
        records.put(id, saved);
        return saved;
    }

    @Override
    public boolean update(StaffRecord record) {
        if (record == null || record.staffId() == null || !records.containsKey(record.staffId())) {
            return false;
        }
        records.put(record.staffId(), record);
        return true;
    }

    @Override
    public Optional<StaffRecord> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return records.values().stream()
            .filter(r -> username.equalsIgnoreCase(r.username()))
            .findFirst();
    }

    /**
     * Seeds a staff account hashed with PasswordHasher.hash.
     */
    void addSeed(String username, String salt, String password, String role, boolean active) {
        int id = nextId++;
        String hash = PasswordHasher.hash(salt, password.toCharArray());
        StaffRecord record = new StaffRecord(
            id,
            username != null ? username.toLowerCase() : null,
            hash,
            salt,
            username + " User",
            username != null ? username.toLowerCase() + "@example.com" : null,
            "0901234567",
            role,
            active,
            LocalDateTime.now()
        );
        records.put(id, record);
    }
}
