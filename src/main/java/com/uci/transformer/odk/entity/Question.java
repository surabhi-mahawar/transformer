package com.uci.transformer.odk.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
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

    public enum QuestionType {
        SINGLE_SELECT,
        MULTI_SELECT,
        STRING;

        public String toString() {
            return this.name();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, name = "form_id")
    private String formID;

    @Column(name = "form_version")
    private String formVersion;

    @Column(name = "x_path")
    private String XPath;

    @Column(name = "question_type")
    @Enumerated(EnumType.STRING)
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

