/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samagra.transformer.odk.model;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;
import lombok.*;

import java.io.Serializable;

/**
 * A form definition stored on the device.
 * <p>
 * Objects of this class are created using the builder pattern: https://en.wikipedia.org/wiki/Builder_pattern
 */
@Document(collection = "forms", schemaVersion = "2.0")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Form implements Serializable {
    @Id
    String id;
    String displayName;
    String description;
    String jrFormId;
    String jrVersion;
    String formFilePath;
    String submissionUri;
    String base64RSAPublicKey;
    String md5Hash;
    Long date;
    String jrCacheFilePath;
    String formMediaPath;
    String language;
    String autoSend;
    String autoDelete;
    String lastDetectedFormVersionHash;
    String geometryXPath;

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof Form && this.md5Hash.equals(((Form) other).md5Hash);
    }

}
