package com.samagra.transformer.odk;

import lombok.*;

import javax.print.DocFlavor;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Form {

    String id;
    String name;
    String path;

}
