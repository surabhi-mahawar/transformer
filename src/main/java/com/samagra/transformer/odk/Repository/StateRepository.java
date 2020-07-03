package com.samagra.transformer.odk.Repository;

import com.samagra.transformer.odk.Entity.GupshupStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends JpaRepository<GupshupStateEntity, Long> {
  GupshupStateEntity findByPhoneNo(String phoneNo);
}
