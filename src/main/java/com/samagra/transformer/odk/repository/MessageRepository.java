package com.samagra.transformer.odk.repository;

import com.samagra.transformer.odk.entity.GupshupMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<GupshupMessageEntity, Long> {
  GupshupMessageEntity findByPhoneNo(String phoneNo);
}
