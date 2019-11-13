package com.ds.iot.framework.simulation;

import akka.japi.Function2;
import akka.japi.function.Procedure;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class NamedProcedure {
  private String pattern;
  private BiConsumer<String,Object> procedure;
}
