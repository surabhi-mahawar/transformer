/** */
package com.uci.transformer.telemetry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
public class TargetObjectTypes {

  private String id;
  private String type;
  private String ver;
  private Map<String,String> rollup;

}
