package com.samagra.transformer.odk.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "xmessage_state")
public class GupshupStateEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, name = "phone_no")
  private String phoneNo;

  @Column(name = "state")
  private String xmlPrevious;
  
  @Column(name = "previous_path")
  private String previousPath;

  @Column(name = "bot_form_name")
  private String botFormName;
  
}
