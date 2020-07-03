package com.samagra.transformer.odk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FormManagerParams {
    String previousPath;
    String currentAnswer;
    String instanceXMlPrevious;
}
