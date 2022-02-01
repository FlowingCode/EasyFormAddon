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

package com.flowingcode.vaadin.addons.easyform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Example bean used by the Easy Form demos. */
@Getter
@Setter
@SuppressWarnings("serial")
public class Person implements Serializable {

  /** Example enum, rendered as a combo box. */
  public enum Gender {
    MALE,
    FEMALE,
    OTHER
  }

  private Long id;

  @NotNull(message = "First name is required")
  @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
  private String firstName;

  @NotNull(message = "Last name is required")
  private String lastName;

  @Email(message = "Must be a valid email address")
  private String email;

  @Min(value = 0, message = "Age must be positive")
  @Max(value = 150, message = "Age must be less than 150")
  private Integer age;

  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;

  private Gender gender;

  private Boolean subscriber;

  private String notes;

  /**
   * Reference to another person. There is no built-in component factory for this type, so the
   * customized demo maps it explicitly to a combo box.
   */
  private Person parent;
}
