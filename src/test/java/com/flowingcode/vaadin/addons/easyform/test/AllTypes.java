/*-
 * #%L
 * Easy Form Add-on
 * %%
 * Copyright (C) 2026 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.flowingcode.vaadin.addons.easyform.test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Test bean that exercises the built-in type mappings that {@code Person} does not cover, the
 * primitive auto-boxing paths, an unsupported property type, and a read-only property.
 */
@Getter
@Setter
@SuppressWarnings("serial")
public class AllTypes implements Serializable {

  private Double doubleValue;

  private Float floatValue;

  private BigDecimal bigDecimalValue;

  private LocalDateTime dateTimeValue;

  private LocalTime timeValue;

  private int intPrimitive;

  private long longPrimitive;

  private double doublePrimitive;

  private float floatPrimitive;

  private boolean booleanPrimitive;

  /** Primitive whose wrapper type has no registered component factory. */
  private short shortPrimitive;

  /** Property type with no registered component factory. */
  private Locale locale;

  /** Read-only property: it has no setter and must therefore be skipped during discovery. */
  @Setter(AccessLevel.NONE)
  private String readOnlyValue = "fixed";
}
