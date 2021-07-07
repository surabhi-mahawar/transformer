package com.uci.transformer.odk.repository;

import com.uci.transformer.odk.entity.Assessment;
import com.uci.transformer.odk.entity.GupshupStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

}
