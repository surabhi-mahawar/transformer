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
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "assessment")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class),
        @TypeDef(name = "jsonb", typeClass = JsonType.class)
})
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "question")
    private Question question;

    @Column(name = "answer")
    private String answer;

    @Column(name = "bot_id")
    private UUID botID;

    @Column(name = "user_id")
    private UUID userID;

    @Column(name = "device_id")
    private UUID deviceID;

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

