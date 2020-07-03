package com.samagra.transformer.odk.Repository;

import com.samagra.Entity.GupshupMessageEntity;
import com.samagra.transformer.odk.Entity.GupshupMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<GupshupMessageEntity, Long> {
  GupshupMessageEntity findByPhoneNo(String phoneNo);
}
