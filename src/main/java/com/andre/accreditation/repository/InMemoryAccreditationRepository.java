package com.andre.accreditation.repository;

import com.andre.accreditation.model.entity.Accreditation;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryAccreditationRepository implements AccreditationRepository {

    private final ConcurrentHashMap<UUID, Accreditation> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<UUID>> userIndex = new ConcurrentHashMap<>();

    @Override
    public Accreditation save(Accreditation accreditation) {
        store.put(accreditation.getId(), accreditation);
        userIndex.computeIfAbsent(accreditation.getUserId(), k -> ConcurrentHashMap.newKeySet())
                .add(accreditation.getId());
        return accreditation;
    }

    @Override
    public Optional<Accreditation> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Accreditation> findByUserId(String userId) {
        Set<UUID> ids = userIndex.getOrDefault(userId, Set.of());
        return ids.stream()
                .map(store::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Accreditation> findAll() {
        return new ArrayList<>(store.values());
    }
}
