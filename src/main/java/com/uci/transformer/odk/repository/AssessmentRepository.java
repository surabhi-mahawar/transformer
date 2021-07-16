package com.uci.transformer.odk.repository;

import com.uci.transformer.odk.entity.Assessment;
import com.uci.transformer.odk.entity.GupshupStateEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface AssessmentRepository extends R2dbcRepository<Assessment, UUID> {

}
