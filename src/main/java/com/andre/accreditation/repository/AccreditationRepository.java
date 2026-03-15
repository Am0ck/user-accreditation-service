package com.andre.accreditation.repository;

import com.andre.accreditation.model.entity.Accreditation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccreditationRepository {

    Accreditation save(Accreditation accreditation);

    Optional<Accreditation> findById(UUID id);

    List<Accreditation> findByUserId(String userId);

    List<Accreditation> findAll();
}
