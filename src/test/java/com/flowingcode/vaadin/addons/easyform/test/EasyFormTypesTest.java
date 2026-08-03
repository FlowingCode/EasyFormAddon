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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.flowingcode.vaadin.addons.easyform.EasyForm;
import com.flowingcode.vaadin.addons.easyform.Person;
import com.flowingcode.vaadin.addons.easyform.Person.Gender;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

/**
 * Tests for the built-in type to component mappings, primitive handling and field discovery rules.
 */
public class EasyFormTypesTest {

  private static List<String> labelsOf(final EasyForm<?> form) {
    return form.getFormLayout().getChildren().map(component -> ((HasLabel) component).getLabel())
        .collect(Collectors.toList());
  }

  // -- built-in type mappings not covered by Person --

  @Test
  public void doublePropertyUsesNumberField() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertTrue(form.field("doubleValue") instanceof NumberField);
    form.readBean(new AllTypes());
    NumberField field = (NumberField) form.field("doubleValue", Double.class);
    field.setValue(1.5);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(1.5, result.get().getDoubleValue().doubleValue(), 0d);
  }

  @Test
  public void floatPropertyUsesNumberFieldWithConverter() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertTrue(form.field("floatValue") instanceof NumberField);
    form.readBean(new AllTypes());
    NumberField field = (NumberField) form.field("floatValue", Double.class);
    field.setValue(2.5);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertNotNull(result.get().getFloatValue());
    assertEquals(2.5f, result.get().getFloatValue().floatValue(), 0f);
  }

  @Test
  public void bigDecimalPropertyUsesBigDecimalField() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertTrue(
        form.field("bigDecimalValue") instanceof BigDecimalField);
    form.readBean(new AllTypes());
    BigDecimalField field =
        (BigDecimalField) form.field("bigDecimalValue", BigDecimal.class);
    BigDecimal expected = new BigDecimal("10.50");
    field.setValue(expected);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(expected, result.get().getBigDecimalValue());
  }

  @Test
  public void localDateTimePropertyUsesDateTimePicker() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertTrue(
        form.field("dateTimeValue") instanceof DateTimePicker);
    form.readBean(new AllTypes());
    DateTimePicker field =
        (DateTimePicker) form.field("dateTimeValue", LocalDateTime.class);
    LocalDateTime expected = LocalDateTime.of(2026, 7, 28, 10, 30);
    field.setValue(expected);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(expected, result.get().getDateTimeValue());
  }

  @Test
  public void localTimePropertyUsesTimePicker() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertTrue(form.field("timeValue") instanceof TimePicker);
    form.readBean(new AllTypes());
    TimePicker field = (TimePicker) form.field("timeValue", LocalTime.class);
    LocalTime expected = LocalTime.of(14, 45);
    field.setValue(expected);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(expected, result.get().getTimeValue());
  }

  // -- primitive auto-boxing --

  @Test
  public void primitivePropertiesUseTheWrapperTypeComponents() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertTrue(form.field("intPrimitive") instanceof IntegerField);
    assertTrue(form.field("longPrimitive") instanceof TextField);
    assertTrue(form.field("doublePrimitive") instanceof NumberField);
    assertTrue(form.field("floatPrimitive") instanceof NumberField);
    assertTrue(form.field("booleanPrimitive") instanceof Checkbox);
  }

  @Test
  public void primitivePropertiesRoundTrip() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    form.readBean(new AllTypes());
    ((IntegerField) form.field("intPrimitive", Integer.class)).setValue(7);
    ((TextField) form.field("longPrimitive", String.class)).setValue("8");
    ((NumberField) form.field("doublePrimitive", Double.class)).setValue(9.5);
    ((Checkbox) form.field("booleanPrimitive", Boolean.class)).setValue(true);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(7, result.get().getIntPrimitive());
    assertEquals(8L, result.get().getLongPrimitive());
    assertEquals(9.5, result.get().getDoublePrimitive(), 0d);
    assertTrue(result.get().isBooleanPrimitive());
  }

  @Test
  public void clearingAPrimitiveFieldWritesTheDefaultInsteadOfFailing() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    AllTypes bean = new AllTypes();
    bean.setIntPrimitive(7);
    bean.setDoublePrimitive(1.5);
    bean.setBooleanPrimitive(true);
    form.readBean(bean);
    form.field("intPrimitive", Integer.class).clear();
    form.field("doublePrimitive", Double.class).clear();
    form.field("booleanPrimitive", Boolean.class).clear();
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(0, result.get().getIntPrimitive());
    assertEquals(0d, result.get().getDoublePrimitive(), 0d);
    assertFalse(result.get().isBooleanPrimitive());
  }

  @Test
  public void clearingAPrimitiveFieldWritesTheDefaultInWriteThroughMode() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    AllTypes bean = new AllTypes();
    bean.setIntPrimitive(7);
    form.setBean(bean);
    // in write-through mode the value reaches the property from the value change listener
    form.field("intPrimitive", Integer.class).clear();
    assertEquals(0, bean.getIntPrimitive());
  }

  @Test
  public void clearingAPrimitiveFieldWithACustomConverterWritesTheDefault() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    form.configureField("intPrimitive").withComponent(new TextField())
        .withConverter(new StringToIntegerConverter("Not a number"));
    AllTypes bean = new AllTypes();
    bean.setIntPrimitive(7);
    form.readBean(bean);
    form.field("intPrimitive", String.class).clear();
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(0, result.get().getIntPrimitive());
  }

  @Test
  public void floatValueIsPresentedAsTheShortestDecimalThatReadsBackTheSame() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    AllTypes bean = new AllTypes();
    bean.setFloatValue(0.1f);
    bean.setFloatPrimitive(0.1f);
    form.readBean(bean);
    // widening the float directly would present the binary value, 0.10000000149011612
    assertEquals(Double.valueOf(0.1), form.field("floatValue", Double.class).getValue());
    assertEquals(Double.valueOf(0.1), form.field("floatPrimitive", Double.class).getValue());
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(0.1f, result.get().getFloatValue().floatValue(), 0f);
    assertEquals(0.1f, result.get().getFloatPrimitive(), 0f);
  }

  @Test
  public void unmappedPrimitiveHasNoComponent() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertNull(form.field("shortPrimitive"));
  }

  @Test
  public void factoryForAWrapperTypeReachesThePrimitiveProperty() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertNull(form.field("shortPrimitive"));
    Converter<Integer, Short> converter =
        Converter.from(value -> Result.ok(value == null ? null : value.shortValue()),
            value -> value == null ? null : value.intValue());
    form.setComponentFactory(Short.class, IntegerField::new, converter);
    assertTrue(form.field("shortPrimitive") instanceof IntegerField);
    form.readBean(new AllTypes());
    ((IntegerField) form.field("shortPrimitive", Integer.class)).setValue(5);
    Optional<AllTypes> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals((short) 5, result.get().getShortPrimitive());
  }

  // -- field discovery rules --

  @Test
  public void unsupportedPropertyTypeIsDiscoveredButHasNoComponent() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertNull(form.field("locale"));
    assertEquals("locale", form.configureField("locale").getPropertyName());
  }

  @Test
  public void fieldsWithoutComponentAreExcludedFromTheLayout() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertEquals(10, form.getFormLayout().getChildren().count());
  }

  @Test
  public void readOnlyPropertyWithoutSetterIsNotDiscovered() {
    EasyForm<AllTypes> form = new EasyForm<>(AllTypes.class);
    assertThrows(IllegalArgumentException.class, () -> form.configureField("readOnlyValue"));
  }

  @Test
  public void inheritedPropertiesAreOrderedBeforeDeclaredOnes() {
    EasyForm<Child> form = new EasyForm<>(Child.class);
    assertEquals(List.of("Zebra", "Alpha"), labelsOf(form));
  }

  // -- enum handling --

  @Test
  public void enumComboBoxIsPopulatedWithTheEnumConstants() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    @SuppressWarnings("unchecked")
    ComboBox<Gender> gender =
        (ComboBox<Gender>) form.field("gender", Gender.class);
    assertEquals(List.of(Gender.values()),
        gender.getListDataView().getItems().collect(Collectors.toList()));
  }

  @Test
  public void enumValueRoundTrips() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    Person person = new Person();
    person.setFirstName("John");
    person.setLastName("Doe");
    person.setGender(Gender.FEMALE);
    form.readBean(person);
    @SuppressWarnings("unchecked")
    ComboBox<Gender> gender =
        (ComboBox<Gender>) form.field("gender", Gender.class);
    assertEquals(Gender.FEMALE, gender.getValue());
    gender.setValue(Gender.OTHER);
    Optional<Person> result = form.getValidBean();
    assertTrue(result.isPresent());
    assertEquals(Gender.OTHER, result.get().getGender());
  }

  @Test
  public void componentFactoryForEnumTypeOverridesTheComboBoxFallback() {
    EasyForm<Person> form = new EasyForm<>(Person.class);
    ComboBox<Gender> custom = new ComboBox<>();
    form.setComponentFactory(Gender.class, () -> custom);
    assertSame(custom, form.field("gender"));
  }

  /**
   * Superclass whose property sorts alphabetically after the subclass property, so that the
   * expected order can only be produced by the declaration-order walk and not by the name
   * tie-breaker.
   */
  @Getter
  @Setter
  public static class Parent {

    private String zebra;
  }

  /** Subclass used to verify that inherited properties keep their declaration order. */
  @Getter
  @Setter
  public static class Child extends Parent {

    private String alpha;
  }
}
