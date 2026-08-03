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

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.util.List;

@DemoSource
@PageTitle("Customized")
@SuppressWarnings("serial")
@Route(value = "easy-form/customized", layout = EasyFormDemoView.class)
public class EasyFormCustomizedDemo extends Div {

  public EasyFormCustomizedDemo() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    List<Person> candidates = parentCandidates();

    // Field selection and order: only the listed fields are shown, in this order
    form.setFieldOrder("firstName", "lastName", "email", "birthDate", "age", "gender", "parent",
        "subscriber", "notes");

    // Field-level customization
    form.configureField("email").withLabel("Email Address").asRequired("Email is required");
    form.configureField("birthDate").withLabel("Date of Birth");
    form.configureField("age").withHelperText("Optional");
    form.configureField("notes").withComponent(new TextArea()).withColSpan(2);

    // A reference to another entity: Person has no built-in component factory, so the field is
    // mapped explicitly to a combo box that renders each candidate by first and last name
    ComboBox<Person> parentComboBox = new ComboBox<>();
    parentComboBox.setItems(candidates);
    parentComboBox.setItemLabelGenerator(
        candidate -> candidate.getFirstName() + " " + candidate.getLastName());
    form.configureField("parent").withComponent(parentComboBox).withLabel("Parent");

    // Layout
    form.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("600px", 2));

    // Button bar
    form.getSaveButton().setText("Create Person");
    form.setSaveAction(person -> Notification.show("Person created"));
    form.setCancelAction(() -> Notification.show("Cancelled"));
    form.addButton("Reset", event -> form.reset());

    // Pre-populate for editing
    Person person = new Person();
    person.setFirstName("John");
    person.setLastName("Doe");
    person.setEmail("john.doe@example.com");
    person.setBirthDate(LocalDate.of(1990, 5, 17));
    person.setGender(Person.Gender.MALE);
    person.setSubscriber(true);
    // the selected value must be one of the combo box items, so reuse the same instances
    person.setParent(candidates.get(0));
    form.setBean(person);

    add(form);
  }

  private static List<Person> parentCandidates() {
    return List.of(newPerson("Anna", "Smith"), newPerson("Robert", "Smith"),
        newPerson("Maria", "Garcia"), newPerson("James", "Wilson"), newPerson("Laura", "Brown"));
  }

  private static Person newPerson(final String firstName, final String lastName) {
    Person person = new Person();
    person.setFirstName(firstName);
    person.setLastName(lastName);
    return person;
  }
}
