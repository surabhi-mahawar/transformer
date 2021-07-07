package com.uci.transformer.odk.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "question")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class),
        @TypeDef(name = "jsonb", typeClass = JsonType.class)
})
public class Question {

    enum QuestionType {
        SINGLE_SELECT,
        MULTI_SELECT,
        STRING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false, name = "form_id")
    private String formID;

    @Column(name = "form_version")
    private String formVersion;

    @Column(name = "xPath")
    private String xPath;

    @Column(name = "question_type")
    private QuestionType questionType;

    @Type(type = "jsonb")
    @Column(name = "meta", columnDefinition = "jsonb")
    private Meta meta;

    @Column(name = "updated")
    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @Column(name = "created")
    @CreationTimestamp
    private LocalDateTime createdOn;

}

