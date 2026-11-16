package com.fincore.account.repository;

import com.fincore.account.model.PixKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PixKeyRepository extends JpaRepository<PixKey, Long> {

    Optional<PixKey> findByKeyValue(String keyValue);

    Optional<PixKey> findByIdAndOwnerEmail(Long id, String ownerEmail);

    boolean existsByKeyValue(String keyValue);

    @EntityGraph(attributePaths = "account")
    List<PixKey> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
}
