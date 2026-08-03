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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.flowingcode.vaadin.addons.easyform.EasyForm;
import com.flowingcode.vaadin.addons.easyform.Person;
import com.flowingcode.vaadin.addons.easyform.Person.Gender;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.shared.Registration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

public class EasyFormTest {

  private static List<String> labelsOf(final EasyForm<?> form) {
    return form.getFormLayout().getChildren().map(component -> ((HasLabel) component).getLabel())
        .collect(Collectors.toList());
  }

  private static Person validPerson() {
    Person person = new Person();
    person.setFirstName("John");
    person.setLastName("Doe");
    person.setEmail("john.doe@example.com");
    person.setAge(30);
    person.setBirthDate(LocalDate.of(1990, 5, 17));
    return person;
  }

  @Test
  public void fieldsAreDiscoveredWithDefaultComponents() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertTrue(form.field("firstName") instanceof TextField);
    assertTrue(form.field("age") instanceof IntegerField);
    assertTrue(form.field("birthDate") instanceof DatePicker);
    assertTrue(form.field("subscriber") instanceof Checkbox);
    assertTrue(form.field("gender") instanceof ComboBox);
  }

  @Test
  public void unknownPropertyThrows() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(IllegalArgumentException.class, () -> form.configureField("nonexistent"));
    assertThrows(IllegalArgumentException.class,
        () -> form.configureField("nonexistent", String.class));
  }

  @Test
  public void configureFieldWithValueTypeInfersThePresentationType() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    // compiles without an explicit type argument, unlike form.configureField("email")
    assertSame(form.configureField("email"), form.configureField("email", String.class)
        .withValidator(new EmailValidator("Invalid email")));
  }

  @Test
  public void configureFieldWithValueTypeAcceptsSupertypesOfThePresentationType() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertNotNull(form.configureField("email", CharSequence.class));
  }

  @Test
  public void configureFieldWithValueTypeChecksThePresentationTypeAndNotThePropertyType() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    // a Long property is presented by a TextField, whose value type is String
    assertNotNull(form.configureField("id", String.class));
    assertThrows(IllegalArgumentException.class, () -> form.configureField("id", Long.class));
  }

  @Test
  public void configureFieldWithValueTypeRejectsIncompatibleTypes() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(IllegalArgumentException.class, () -> form.configureField("email", Integer.class));
    assertThrows(NullPointerException.class, () -> form.configureField("email", null));
  }

  @Test
  public void configureFieldWithValueTypeFollowsTheCurrentComponent() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertNotNull(form.configureField("age", Integer.class));
    form.setComponentFactory(
        Integer.class, TextField::new, new StringToIntegerConverter("Must be a number"));
    assertNotNull(form.configureField("age", String.class));
    assertThrows(IllegalArgumentException.class, () -> form.configureField("age", Integer.class));
  }

  @Test
  public void configureFieldWithValueTypeSkipsComponentsWithAnUnresolvableValueType() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertTrue(form.field("gender") instanceof ComboBox);
    assertNotNull(form.configureField("gender", Gender.class));
  }

  @Test
  public void configureFieldWithValueTypeSkipsPropertiesWithoutAComponent() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertNull(form.field("parent"));
    assertNotNull(form.configureField("parent", Person.class));
  }

  @Test
  public void componentThatCannotBeWrittenToItsPropertyIsReported() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("age").withComponent(new TextField());
    Person person = validPerson();
    assertThrows(IllegalStateException.class, () -> form.setBean(person));
    assertThrows(IllegalStateException.class, () -> form.readBean(person));
    assertThrows(IllegalStateException.class, () -> form.getValidBean());
  }

  @Test
  public void componentThatCannotBeWrittenToItsPropertyIsAcceptedWithAConverter() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("age").withComponent(new TextField())
        .withConverter(new StringToIntegerConverter("Must be a number"));
    form.readBean(validPerson());
    TextField age = (TextField) form.field("age", String.class);
    assertEquals("30", age.getValue());
    age.setValue("42");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Integer.valueOf(42), result.get().getAge());
  }

  @Test
  public void hiddenFieldWithAnIncompatibleComponentIsNotReported() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("age").withComponent(new TextField()).excluded();
    form.setBean(validPerson());
  }

  @Test
  public void defaultLabelsAreGeneratedFromPropertyNames() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    TextField firstName = (TextField) form.field("firstName", String.class);
    assertEquals("First Name", firstName.getLabel());
    DatePicker birthDate = (DatePicker) form.field("birthDate", LocalDate.class);
    assertEquals("Birth Date", birthDate.getLabel());
  }

  @Test
  public void explicitLabelOverridesGeneratedOne() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("email").withLabel("Email Address");
    TextField email = (TextField) form.field("email", String.class);
    assertEquals("Email Address", email.getLabel());
  }

  @Test
  public void setBeanPopulatesFields() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    assertEquals("John", firstName.getValue());
  }

  @Test
  public void setBeanWritesThrough() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.setBean(person);
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("Jane");
    assertEquals("Jane", person.getFirstName());
  }

  @Test
  public void readBeanDoesNotWriteThrough() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.readBean(person);
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("Jane");
    assertEquals("John", person.getFirstName());
  }

  @Test
  public void getValidBeanReturnsPopulatedBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("Jane");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals("Jane", result.get().getFirstName());
  }

  @Test
  public void beanValidationAnnotationsAreApplied() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    assertFalse(form.getValidBean().isPresent());
  }

  @Test
  public void programmaticValidatorsAreApplied() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("firstName", String.class)
        .withValidator(
            (String value, ValueContext context) ->
                "John".equals(value)
                    ? ValidationResult.ok()
                    : ValidationResult.error("Must be John"));
    Person person = validPerson();
    person.setFirstName("Jane");
    form.readBean(person);
    assertFalse(form.getValidBean().isPresent());
    person.setFirstName("John");
    form.readBean(person);
    assertTrue(form.getValidBean().isPresent());
  }

  @Test
  public void beanLevelValidatorsAreApplied() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.addBeanValidator(
        (person, context) ->
            person.getAge() != null && person.getAge() < 18
                ? ValidationResult.error("Must be an adult")
                : ValidationResult.ok());
    Person person = validPerson();
    person.setAge(10);
    form.readBean(person);
    assertFalse(form.getValidBean().isPresent());
  }

  @Test
  public void hiddenFieldsAreExcludedFromBinding() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    person.setEmail("not an email");
    form.configureField("email").excluded();
    form.readBean(person);
    assertTrue(form.getValidBean().isPresent());
  }

  @Test
  public void hiddenFieldCanBeMadeVisibleAgain() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.readBean(person);
    form.configureField("email").excluded();
    form.configureField("email").visible();
    TextField email = (TextField) form.field("email", String.class);
    assertEquals("john.doe@example.com", email.getValue());
  }

  @Test
  public void readOnlyFieldIsDisplayedButNotEditable() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("firstName").readOnly();
    TextField firstName = (TextField) form.field("firstName", String.class);
    assertTrue(firstName.isReadOnly());
  }

  @Test
  public void setVisibleFieldsShowsOnlyListedFields() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setVisibleFields("firstName", "lastName");
    long count = form.getFormLayout().getChildren().count();
    assertEquals(2, count);
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    // email is excluded from the binding, so its invalid value does not fail validation
    assertTrue(form.getValidBean().isPresent());
  }

  @Test
  public void setFieldOrderDoesNotHideOrUnbindUnlistedFields() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    long before = form.getFormLayout().getChildren().count();
    form.setFieldOrder("firstName", "lastName");
    assertEquals(before, form.getFormLayout().getChildren().count());
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    // email is still bound even though it was not listed, so it still fails validation
    assertFalse(form.getValidBean().isPresent());
  }

  @Test
  public void setVisibleFieldsRejectsUnknownProperties() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(IllegalArgumentException.class, () -> form.setVisibleFields("firstName", "bogus"));
  }

  @Test
  public void setFieldOrderRejectsUnknownProperties() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(IllegalArgumentException.class, () -> form.setFieldOrder("firstName", "bogus"));
  }

  @Test
  public void customComponentReplacesGeneratedOne() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    TextArea notes = new TextArea();
    form.configureField("notes").withComponent(notes);
    assertEquals(notes, form.field("notes"));
    form.setBean(validPerson());
    notes.setValue("some notes");
    assertEquals("some notes", form.getBean().getNotes());
  }

  @Test
  public void perFormComponentFactoryOverridesDefault() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setComponentFactory(String.class, TextArea::new);
    assertTrue(form.field("firstName") instanceof TextArea);
    // explicit components are preserved
    EasyForm<Person> other = new EasyForm<>(Person.class);
    TextField custom = new TextField();
    other.configureField("firstName").withComponent(custom);
    other.setComponentFactory(String.class, TextArea::new);
    assertEquals(custom, other.field("firstName"));
  }

  @Test
  public void componentFactoryWithConverterAppliesConverter() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setComponentFactory(
        Integer.class, TextField::new, new StringToIntegerConverter("Must be a number"));
    assertTrue(form.field("age") instanceof TextField);
    form.readBean(validPerson());
    TextField age = (TextField) form.field("age", String.class);
    assertEquals("30", age.getValue());
    age.setValue("42");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Integer.valueOf(42), result.get().getAge());
  }

  @Test
  public void longPropertyUsesFactoryProvidedConverter() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertTrue(form.field("id") instanceof TextField);
    form.readBean(validPerson());
    TextField id = (TextField) form.field("id", String.class);
    id.setValue("7");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Long.valueOf(7), result.get().getId());
  }

  @Test
  public void longPropertyKeepsValuesThatExceedTheDoubleMantissa() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    TextField id = (TextField) form.field("id", String.class);
    // 2^53 + 1, which cannot be represented as a double
    id.setValue("9007199254740993");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Long.valueOf(9007199254740993L), result.get().getId());
  }

  @Test
  public void longPropertyRejectsFractionalInputInsteadOfTruncatingIt() {
    // the built-in converter reads the number in the locale of the session, as Vaadin's own
    // converters do, so the separator this test types has to be the one of a known locale
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
    try {
      EasyForm<Person> form = new EasyForm<>(Person.class);
      Person person = validPerson();
      person.setId(3L);
      form.readBean(person);
      ((TextField) form.field("id", String.class)).setValue("1.5");
      assertFalse(form.getValidBean().isPresent());
      assertEquals(Long.valueOf(3), person.getId());
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void withComponentDropsTheConverterOfThePreviousComponent() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("age").withComponent(new TextField())
        .withConverter(new StringToIntegerConverter("Not a number"));
    // the String -> Integer converter cannot apply to the new component and must not survive it
    form.configureField("age").withComponent(new IntegerField());
    form.setBean(validPerson());
    assertEquals(Integer.valueOf(30), form.field("age", Integer.class).getValue());
  }

  @Test
  public void withComponentDropsTheValidatorsOfThePreviousComponent() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("age").withComponent(new TextField())
        .withValidator(new StringLengthValidator("Too long", 0, 2))
        .withConverter(new StringToIntegerConverter("Not a number"));
    // validators run on the presentation value, so they are as component bound as the converter
    form.configureField("age").withComponent(new IntegerField());
    form.readBean(validPerson());
    form.field("age", Integer.class).setValue(100);
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Integer.valueOf(100), result.get().getAge());
  }

  @Test
  public void reconfiguringAFieldKeepsTheValueTheUserTyped() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("TypedByUser");
    form.configureField("firstName").asRequired("First name is required");
    assertEquals("TypedByUser", firstName.getValue());
  }

  @Test
  public void changingTheVisibleFieldsKeepsTheValuesTheUserTyped() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    TextField lastName = (TextField) form.field("lastName", String.class);
    firstName.setValue("TypedByUser");
    lastName.clear();
    form.setVisibleFields("firstName", "lastName", "age");
    assertEquals("TypedByUser", firstName.getValue());
    assertEquals("", lastName.getValue());
  }

  @Test
  public void aFieldThatBecomesVisibleAgainIsPopulatedFromTheBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    form.configureField("firstName").excluded();
    firstName.setValue("StaleWhileExcluded");
    form.configureField("firstName").visible();
    assertEquals("John", firstName.getValue());
  }

  // -- binding lifecycle --

  @Test
  public void attachingTheFormBindsTheFields() {
    UI ui = new UI();
    EasyForm<Person> form = new EasyForm<>(Person.class);
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("TypedBeforeAttach");
    ui.add(form);
    // the value typed while the field was unbound survives being bound
    assertEquals("TypedBeforeAttach", firstName.getValue());
    // and what is typed once the form is displayed reaches the binder, which only holds if
    // attaching bound the fields: a value changed before its binding exists goes unnoticed
    firstName.setValue("TypedAfterAttach");
    assertTrue(form.getBinder().hasChanges());
  }

  @Test
  public void configurationCollectedBeforeTheFirstUseIsAppliedInOnePass() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("age").withComponent(new TextField())
        .withValidator(new StringLengthValidator("At most three digits", 0, 3))
        .withConverter(new StringToIntegerConverter("Not a number"));
    form.configureField("email").asRequired("Email is required");
    form.configureField("lastName").excluded();
    form.readBean(validPerson());
    // every piece of the configuration above survived into the single binding of each field
    assertEquals("30", form.field("age", String.class).getValue());
    form.field("age", String.class).setValue("1000");
    assertFalse(form.getValidBean().isPresent());
    form.field("age", String.class).setValue("31");
    form.field("email", String.class).clear();
    assertFalse(form.getValidBean().isPresent());
    form.field("email", String.class).setValue("john.doe@example.com");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Integer.valueOf(31), result.get().getAge());
    // the excluded field was never bound, so it is neither displayed nor written
    assertFalse(labelsOf(form).contains("Last Name"));
    assertEquals("Doe", result.get().getLastName());
  }

  @Test
  public void reconfiguringAFieldAfterTheFormIsBoundTakesEffectImmediately() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    person.setNotes("ok");
    form.setBean(person);
    // the form is bound now, so the validator has to reach the binding right away: in write
    // through mode nothing else calls the form between here and the value change below.
    // 'notes' carries no bean validation annotation, so this validator is the only thing that
    // can keep the value out of the bean
    form.configureField("notes", String.class)
        .withValidator(new StringLengthValidator("At most three characters", 0, 3));
    form.field("notes", String.class).setValue("way too long");
    assertEquals("ok", person.getNotes());
  }

  @Test
  public void requiredIndicatorIsVisibleBeforeTheFieldIsBound() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("email").asRequired("Email is required");
    assertTrue(form.field("email", String.class).isRequiredIndicatorVisible());
  }

  @Test
  public void requiredFieldFailsValidationWhenEmpty() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("email").asRequired("Email is required");
    Person person = validPerson();
    person.setEmail(null);
    form.readBean(person);
    assertFalse(form.getValidBean().isPresent());
  }

  @Test
  public void clearAndReset() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    form.clear();
    assertEquals("", firstName.getValue());
    form.reset();
    assertEquals("John", firstName.getValue());
  }

  @Test
  public void clearDoesNotWriteThroughBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.setBean(person);
    form.clear();
    assertEquals("John", person.getFirstName());
    TextField firstName = (TextField) form.field("firstName", String.class);
    assertEquals("", firstName.getValue());
    form.reset();
    assertEquals("John", firstName.getValue());
    firstName.setValue("Jane");
    // write-through is restored after reset
    assertEquals("Jane", person.getFirstName());
  }

  @Test
  public void clearDetachesTheBeanSoItIsNeverWrittenByGetValidBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.setBean(person);
    form.clear();
    assertNull(form.getBean());
    assertNull(form.getBinder().getBean());
    Optional<Person> result = form.getValidBean();
    // the cleared form is written to a new instance, never to the bean the caller passed in
    assertTrue(result.isEmpty() || result.get() != person);
    assertEquals("John", person.getFirstName());
    assertEquals("Doe", person.getLastName());
  }

  @Test
  public void clearThenGetValidBeanLeavesAnUnconstrainedBeanUntouched() {
    EasyForm<Unconstrained> form = new EasyForm<>(Unconstrained.class);
    Unconstrained bean = new Unconstrained();
    bean.setName("Leonardo");
    form.setBean(bean);
    form.clear();
    Optional<Unconstrained> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertNotSame(bean, result.get());
    assertEquals("Leonardo", bean.getName());
  }

  @Test
  public void resetAfterClearReattachesTheBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.setBean(person);
    form.clear();
    form.reset();
    assertSame(person, form.getBean());
    assertSame(person, form.getBinder().getBean());
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("Jane");
    assertEquals("Jane", person.getFirstName());
  }

  @Test
  public void validateReportsWhichFieldsFailed() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    BinderValidationStatus<Person> status = form.validate();
    assertTrue(status.hasErrors());
    assertEquals(List.of("Must be a valid email address"),
        status.getValidationErrors().stream().map(ValidationResult::getErrorMessage)
            .collect(Collectors.toList()));
    // validate() reports, it does not write
    assertEquals("not an email", person.getEmail());
  }

  @Test
  public void validateOnAValidFormHasNoErrors() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.readBean(validPerson());
    assertFalse(form.validate().hasErrors());
  }

  // -- events --

  @Test
  public void statusChangeListenerFiresOnValidationChanges() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setBean(validPerson());
    List<Boolean> errorStates = new ArrayList<>();
    Registration registration =
        form.addStatusChangeListener(event -> errorStates.add(event.hasValidationErrors()));
    TextField email = (TextField) form.field("email", String.class);
    email.setValue("not an email");
    email.setValue("jane.doe@example.com");
    assertEquals(List.of(true, false), errorStates);
    registration.remove();
    email.setValue("broken again");
    assertEquals(List.of(true, false), errorStates);
  }

  @Test
  public void valueChangeListenerFiresForEveryFieldIncludingInvalidValues() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setBean(validPerson());
    int[] changes = {0};
    form.addValueChangeListener(event -> changes[0]++);
    ((TextField) form.field("email", String.class)).setValue("not an email");
    ((TextField) form.field("firstName", String.class)).setValue("Jane");
    assertEquals(2, changes[0]);
  }

  @Test
  public void listenersRejectNullArguments() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(NullPointerException.class, () -> form.addStatusChangeListener(null));
    assertThrows(NullPointerException.class, () -> form.addValueChangeListener(null));
  }

  // -- component accessors and fluency --

  @Test
  public void fieldReturnsTheComponentAndConfigureFieldItsWrapper() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertSame(form.configureField("email").getComponent(), form.field("email"));
    assertTrue(form.field("email") instanceof TextField);
    HasValue<?, String> typed = form.field("email", String.class);
    assertSame(form.field("email"), typed);
    // the type check of the two-argument overload applies here too
    assertThrows(IllegalArgumentException.class, () -> form.field("email", Integer.class));
    assertThrows(IllegalArgumentException.class, () -> form.field("nonexistent"));
  }

  @Test
  public void fieldIsNullForPropertiesWithoutAComponent() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertNull(form.field("parent"));
  }

  @Test
  public void formLevelConfigurationIsChainable() {
    Person person = validPerson();
    EasyForm<Person> form = new EasyForm<>(Person.class)
        .setFieldOrder("firstName", "lastName", "email")
        .setVisibleFields("firstName", "lastName", "email")
        .setLabelGenerator(name -> name.toUpperCase())
        .setI18n(new EasyForm.EasyFormI18n().setSave("Guardar"))
        .setSaveAction(saved -> {})
        .setCancelAction(() -> {})
        .setSaveButtonVisible(true)
        .setBean(person);
    assertEquals(List.of("FIRSTNAME", "LASTNAME", "EMAIL"), labelsOf(form));
    assertEquals("Guardar", form.getSaveButton().getText());
    assertSame(person, form.getBean());
    // the action methods chain too
    assertSame(form, form.clear().reset());
  }

  // -- collection accessors --

  @Test
  public void getFieldNamesAndGetFieldsFollowTheDisplayOrder() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setFieldOrder("email", "firstName");
    assertEquals(List.of("email", "firstName"), form.getFieldNames().subList(0, 2));
    assertEquals(form.getFieldNames().size(), form.getFields().size());
    assertEquals("email", form.getFields().get(0).getPropertyName());
    // excluded fields are still listed
    form.configureField("email").excluded();
    assertTrue(form.getFieldNames().contains("email"));
  }

  @Test
  public void findFieldDoesNotThrowForUnknownProperties() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertTrue(form.findField("email").isPresent());
    assertSame(form.configureField("email"), form.findField("email").get());
    assertTrue(form.findField("nonexistent").isEmpty());
    // nested property paths are not discovered
    assertTrue(form.findField("parent.firstName").isEmpty());
  }

  // -- i18n and label generation --

  @Test
  public void setI18nChangesTheButtonTexts() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertEquals("Save", form.getI18n().getSave());
    form.setI18n(new EasyForm.EasyFormI18n().setSave("Guardar").setCancel("Cancelar"));
    assertEquals("Guardar", form.getSaveButton().getText());
    assertEquals("Cancelar", form.getCancelButton().getText());
    assertEquals("Guardar", form.getI18n().getSave());
    assertThrows(NullPointerException.class, () -> form.setI18n(null));
  }

  @Test
  public void labelGeneratorReplacesGeneratedLabelsButNotExplicitOnes() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("email").withLabel("Email Address");
    form.setLabelGenerator(name -> name.toUpperCase());
    assertEquals("FIRSTNAME",
        ((TextField) form.field("firstName", String.class)).getLabel());
    assertEquals("Email Address",
        ((TextField) form.field("email", String.class)).getLabel());
    // null restores the default derivation
    form.setLabelGenerator(null);
    assertEquals("First Name",
        ((TextField) form.field("firstName", String.class)).getLabel());
  }

  // -- extension points --

  @Test
  public void subclassCanFilterPropertiesAndDecorateComponents() {
    EasyForm<Person> form = new EasyForm<>(Person.class) {
      @Override
      protected boolean includeProperty(java.beans.PropertyDescriptor property) {
        return !"notes".equals(property.getName());
      }

      @Override
      protected String createLabel(String propertyName) {
        return "[" + propertyName + "]";
      }

      @Override
      protected void configureComponent(String propertyName, HasValue<?, ?> component) {
        ((Component) component).getElement().setAttribute("data-property", propertyName);
      }
    };
    assertTrue(form.findField("notes").isEmpty());
    TextField firstName = (TextField) form.field("firstName", String.class);
    assertEquals("[firstName]", firstName.getLabel());
    assertEquals("firstName", firstName.getElement().getAttribute("data-property"));
  }

  @Test
  public void subclassCanCreateComponentsForOtherwiseUnmappedTypes() {
    EasyForm<Person> form = new EasyForm<>(Person.class) {
      @Override
      protected HasValue<?, ?> createComponent(String propertyName, Class<?> propertyType) {
        return propertyType == Person.class ? new ComboBox<Person>()
            : super.createComponent(propertyName, propertyType);
      }
    };
    // 'parent' has no registered factory, so it would otherwise be left out
    assertTrue(form.field("parent") instanceof ComboBox);
    // the enum default from super is still in place
    assertTrue(form.field("gender") instanceof ComboBox);
  }

  // -- button bar --

  @Test
  public void addButtonAppliesEveryVariantAndRemoveButtonTakesItOut() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    // the button bar is the parent of the built-in buttons
    Component bar = form.getSaveButton().getParent().orElseThrow();
    long before = bar.getChildren().count();
    Button button = form.addButton("Reset", event -> {}, ButtonVariant.LUMO_TERTIARY,
        ButtonVariant.LUMO_ERROR);
    assertEquals(before + 1, bar.getChildren().count());
    assertSame(bar, button.getParent().orElseThrow());
    assertTrue(button.getThemeNames().contains(ButtonVariant.LUMO_TERTIARY.getVariantName()));
    assertTrue(button.getThemeNames().contains(ButtonVariant.LUMO_ERROR.getVariantName()));
    form.removeButton(button);
    assertEquals(before, bar.getChildren().count());
    assertTrue(button.getParent().isEmpty());
  }

  @Test
  public void removeButtonRejectsForeignAndBuiltInButtons() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(NullPointerException.class, () -> form.removeButton(null));
    assertThrows(IllegalArgumentException.class, () -> form.removeButton(form.getSaveButton()));
    assertThrows(IllegalArgumentException.class, () -> form.removeButton(form.getCancelButton()));
    assertThrows(IllegalArgumentException.class, () -> form.removeButton(new Button("Foreign")));
  }

  // -- form-level component interfaces --

  @Test
  public void formSupportsStyle() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.addClassName("my-form");
    assertTrue(form.getClassNames().contains("my-form"));
  }

  @Test
  public void disablingTheFormDisablesTheFieldsAndTheButtons() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setSaveAction(person -> {});
    form.setEnabled(false);
    assertFalse(form.isEnabled());
    // the generated components and the button bar are in this component's element tree
    assertFalse(((TextField) form.field("firstName")).isEnabled());
    assertFalse(form.getSaveButton().isEnabled());
    assertFalse(form.getCancelButton().isEnabled());
  }

  @Test
  public void disablingTheFormStopsValuesBeingWrittenToTheBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.setBean(person);
    form.setEnabled(false);
    ((TextField) form.field("firstName")).setValue("Jane");
    assertEquals("John", person.getFirstName());
    // and the values are still read into the fields
    assertEquals("Jane", ((TextField) form.field("firstName")).getValue());
    form.setEnabled(true);
    ((TextField) form.field("firstName")).setValue("Joanne");
    assertEquals("Joanne", person.getFirstName());
  }

  @Test
  public void reEnablingTheFormKeepsPerFieldReadOnlyState() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.configureField("lastName").readOnly();
    form.setBean(person);
    form.setEnabled(false);
    form.setEnabled(true);
    ((TextField) form.field("firstName")).setValue("Jane");
    ((TextField) form.field("lastName")).setValue("Smith");
    assertEquals("Jane", person.getFirstName());
    // lastName was configured read-only, so re-enabling must not make it writable
    assertEquals("Doe", person.getLastName());
  }

  @Test
  public void fieldsBoundWhileDisabledAreAlsoReadOnly() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = validPerson();
    form.setEnabled(false);
    // rebinding happens here, after the form was disabled
    form.configureField("firstName").withLabel("First");
    form.setBean(person);
    ((TextField) form.field("firstName")).setValue("Jane");
    assertEquals("John", person.getFirstName());
  }

  /** Bean with no Bean Validation constraints, so an emptied form still validates. */
  public static class Unconstrained {

    @Getter
    @Setter
    private String name;
  }

  @Test
  public void readBeanDetachesPreviouslySetBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person first = validPerson();
    Person second = validPerson();
    second.setFirstName("Alice");
    form.setBean(first);
    form.readBean(second);
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("Jane");
    assertEquals("John", first.getFirstName());
    assertEquals("Alice", second.getFirstName());
  }

  @Test
  public void getValidBeanCreatesInstanceWhenNoBeanSet() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    TextField firstName = (TextField) form.field("firstName", String.class);
    TextField lastName = (TextField) form.field("lastName", String.class);
    firstName.setValue("Jane");
    lastName.setValue("Doe");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals("Jane", result.get().getFirstName());
    assertNull(result.get().getId());
  }

  @Test
  public void buttonBarIsHiddenUntilAnActionIsSet() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertFalse(form.getSaveButton().isVisible());
    assertFalse(form.getCancelButton().isVisible());
    form.setSaveAction(person -> {});
    assertTrue(form.getSaveButton().isVisible());
    assertFalse(form.getCancelButton().isVisible());
    form.setSaveButtonVisible(false);
    assertFalse(form.getSaveButton().isVisible());
  }

  // -- accessors --

  @Test
  public void beanTypeAndBinderAreExposed() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertEquals(Person.class, form.getBeanType());
    assertNotNull(form.getBinder());
    assertEquals("email", form.configureField("email").getPropertyName());
  }

  // -- field order --

  @Test
  public void setFieldOrderDeterminesTheLayoutOrder() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    List<String> unordered = labelsOf(form);
    form.setFieldOrder("lastName", "firstName", "email");
    List<String> ordered = labelsOf(form);
    assertEquals(List.of("Last Name", "First Name", "Email"), ordered.subList(0, 3));
    // every other field keeps its declaration order after the listed ones
    assertEquals(unordered.size(), ordered.size());
    List<String> rest = new ArrayList<>(unordered);
    rest.removeAll(List.of("Last Name", "First Name", "Email"));
    assertEquals(rest, ordered.subList(3, ordered.size()));
  }

  @Test
  public void fieldExcludedBySetVisibleFieldsCanBeIncludedAgainIndividually() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setVisibleFields("firstName");
    assertEquals(List.of("First Name"), labelsOf(form));
    // setVisibleFields drives the same per-field state, so visible() reverses it for one field
    form.configureField("email").visible();
    assertEquals(List.of("First Name", "Email"), labelsOf(form));
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    // and the field is bound again, not merely displayed
    assertFalse(form.getValidBean().isPresent());
  }

  @Test
  public void setVisibleFieldsKeepsListedReadOnlyFieldsReadOnly() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("firstName").readOnly();
    form.setVisibleFields("firstName", "lastName");
    assertEquals(List.of("First Name", "Last Name"), labelsOf(form));
    assertTrue(form.field("firstName", String.class).isReadOnly());
    assertFalse(form.field("lastName", String.class).isReadOnly());
  }

  @Test
  public void setVisibleFieldsExcludesEverythingNotListed() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("email").excluded();
    // listing email again brings it back; lastName, not listed, goes away
    form.setVisibleFields("firstName", "email");
    assertEquals(List.of("First Name", "Email"), labelsOf(form));
  }

  @Test
  public void setVisibleFieldsCanReIncludeExcludedFields() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setVisibleFields("firstName");
    form.setVisibleFields("firstName", "email");
    assertEquals(List.of("First Name", "Email"), labelsOf(form));
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    // email is bound again, so its invalid value fails validation
    assertFalse(form.getValidBean().isPresent());
  }

  @Test
  public void excludedFieldsAreRemovedFromTheLayout() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    long before = form.getFormLayout().getChildren().count();
    form.configureField("email").excluded();
    form.configureField("notes").excluded();
    assertEquals(before - 2, form.getFormLayout().getChildren().count());
  }

  // -- converters --

  @Test
  public void conversionErrorFailsValidationAndLeavesTheBeanUntouched() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.setComponentFactory(
        Integer.class, TextField::new, new StringToIntegerConverter("Must be a number"));
    Person person = validPerson();
    form.readBean(person);
    TextField age = (TextField) form.field("age", String.class);
    age.setValue("abc");
    assertFalse(form.getValidBean().isPresent());
    assertEquals(Integer.valueOf(30), person.getAge());
  }

  @Test
  public void withConverterAdaptsThePresentationValue() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    TextField age = new TextField();
    form.configureField("age").withComponent(age)
        .withConverter(new StringToIntegerConverter("Must be a number"));
    form.readBean(validPerson());
    assertEquals("30", age.getValue());
    age.setValue("42");
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Integer.valueOf(42), result.get().getAge());
  }

  // -- bean instantiation --

  @Test
  public void getValidBeanThrowsWhenTheBeanTypeCannotBeInstantiated() {
    EasyForm<NoDefaultConstructor> form = new EasyForm<>(NoDefaultConstructor.class);
    TextField name = (TextField) form.field("name", String.class);
    name.setValue("value");
    assertThrows(IllegalStateException.class, () -> form.getValidBean());
  }

  // -- button actions --

  @Test
  public void saveActionReceivesTheValidatedBean() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    List<Person> saved = new ArrayList<>();
    form.setSaveAction(saved::add);
    form.readBean(validPerson());
    TextField firstName = (TextField) form.field("firstName", String.class);
    firstName.setValue("Jane");
    form.getSaveButton().click();
    assertEquals(1, saved.size());
    assertEquals("Jane", saved.get(0).getFirstName());
  }

  @Test
  public void saveActionIsNotInvokedWhenValidationFails() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    List<Person> saved = new ArrayList<>();
    form.setSaveAction(saved::add);
    Person person = validPerson();
    person.setEmail("not an email");
    form.readBean(person);
    form.getSaveButton().click();
    assertTrue(saved.isEmpty());
  }

  @Test
  public void cancelActionIsInvokedOnCancel() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    boolean[] cancelled = {false};
    form.setCancelAction(() -> cancelled[0] = true);
    form.getCancelButton().click();
    assertTrue(cancelled[0]);
  }

  @Test
  public void cancelButtonTextAndVisibilityCanBeOverridden() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.getCancelButton().setText("Discard");
    assertEquals("Discard", form.getCancelButton().getText());
    assertFalse(form.getCancelButton().isVisible());
    form.setCancelButtonVisible(true);
    assertTrue(form.getCancelButton().isVisible());
  }

  @Test
  public void addButtonWithIconAndVariant() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Icon icon = VaadinIcon.PLUS.create();
    boolean[] clicked = {false};
    Button button =
        form.addButton("Add", icon, event -> clicked[0] = true, ButtonVariant.LUMO_SUCCESS);
    assertEquals("Add", button.getText());
    assertSame(icon, button.getIcon());
    assertTrue(button.getThemeNames().contains(ButtonVariant.LUMO_SUCCESS.getVariantName()));
    button.click();
    assertTrue(clicked[0]);
  }

  // -- presentation --

  @Test
  public void placeholderAndHelperTextAreApplied() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("email").withPlaceholder("user@example.com").withHelperText("Work email");
    TextField email = (TextField) form.field("email", String.class);
    assertEquals("user@example.com", email.getPlaceholder());
    assertEquals("Work email", email.getHelperText());
  }

  @Test
  public void placeholderIsIgnoredWhenTheComponentDoesNotSupportIt() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("subscriber").withPlaceholder("ignored");
    Checkbox subscriber = (Checkbox) form.field("subscriber", Boolean.class);
    assertNull(subscriber.getElement().getProperty("placeholder"));
  }

  @Test
  public void colSpanIsAppliedToTheLayout() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    form.configureField("notes").withColSpan(2);
    Component notes = (Component) form.field("notes");
    assertEquals("2", notes.getElement().getAttribute("colspan"));
  }

  @Test
  public void colSpanRejectsValuesBelowOne() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(
        IllegalArgumentException.class, () -> form.configureField("notes").withColSpan(0));
  }

  // -- defensive contracts --

  @Test
  public void constructorRejectsNullBeanType() {
    assertThrows(NullPointerException.class, () -> new EasyForm<Person>(null));
  }

  @Test
  public void componentFactoryRejectsNullArguments() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(
        NullPointerException.class, () -> form.setComponentFactory(null, TextField::new));
    assertThrows(
        NullPointerException.class, () -> form.setComponentFactory(String.class, null));
  }

  // A HasValue that is not a Component needs no runtime test any more: the intersection bound on
  // setComponentFactory/withComponent turns it into a compile error.
  @Test
  public void fluentConfigurationRejectsNullArguments() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    assertThrows(NullPointerException.class, () -> form.addBeanValidator(null));
    assertThrows(NullPointerException.class, () -> form.configureField("email").asRequired(null));
    assertThrows(NullPointerException.class,
        () -> form.configureField("email").withValidator(null));
    assertThrows(NullPointerException.class,
        () -> form.configureField("email").withConverter(null));
    assertThrows(NullPointerException.class,
        () -> form.configureField("email").withComponent(null));
  }

  /** Bean without an accessible no-args constructor. */
  public static class NoDefaultConstructor {

    @Getter
    @Setter
    private String name;

    public NoDefaultConstructor(final String name) {
      this.name = name;
    }
  }

}
