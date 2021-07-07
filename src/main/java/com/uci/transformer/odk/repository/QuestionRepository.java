package com.uci.transformer.odk.repository;

import com.uci.transformer.odk.entity.GupshupStateEntity;
import com.uci.transformer.odk.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findQuestionByXPathAndFormIDAndFormVersion(String xPath, String formID, String formVersion);
}
