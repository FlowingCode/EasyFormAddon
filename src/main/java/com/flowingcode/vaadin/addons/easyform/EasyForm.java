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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.StatusChangeListener;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.util.SharedUtil;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A form component that is automatically generated from a POJO definition.
 *
 * <p>
 * {@code EasyForm} introspects the properties of the given bean type (via getter/setter
 * conventions) and creates an appropriate Vaadin form field for each one, configures validations
 * based on JSR-380 (Bean Validation) annotations, and manages data binding through an internal
 * {@link Binder}. Properties without both a getter and a setter, or whose type has no registered
 * component factory, are ignored.
 *
 * <p>
 * All customization is programmatic through a fluent API:
 *
 * <pre>{@code
 * EasyForm<Person> form = new EasyForm<>(Person.class);
 * form.configureField("email").withLabel("Email Address").asRequired("Email is required");
 * form.setSaveAction(person -> personService.save(person));
 * add(form);
 * }</pre>
 *
 * <p>
 * Configuring a field before the form is used only collects the configuration: the fields are
 * bound in one pass the first time the form interacts with a bean or with its {@link Binder}, and
 * when the form is attached. A form is therefore configured completely before anything is bound,
 * whatever order the configuration is written in. Reconfiguring a field after that rebuilds its
 * binding immediately, so the change always takes effect on the next value change.
 *
 * <p>
 * Discovery is flat: only the direct properties of the bean type are used. Nested property paths
 * such as {@code "address.street"}, which {@link Binder} itself supports, are not discovered, and
 * asking for one through {@link #configureField(String)} fails as for any unknown property.
 *
 * <p>
 * Subclasses can influence generation by overriding {@link #includeProperty(PropertyDescriptor)},
 * {@link #createComponent(String, Class)}, {@link #createLabel(String)} and
 * {@link #configureComponent(String, HasValue)}. These are called while the constructor discovers
 * the properties, so an override must not depend on state initialized in the subclass constructor
 * body.
 *
 * @param <T> the bean type
 */
@SuppressWarnings("serial")
public class EasyForm<T> extends Composite<VerticalLayout>
    implements HasSize, HasStyle, HasEnabled {

  private static final Logger logger = LoggerFactory.getLogger(EasyForm.class);

  private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = Map.of(
      boolean.class, Boolean.class,
      byte.class, Byte.class,
      char.class, Character.class,
      double.class, Double.class,
      float.class, Float.class,
      int.class, Integer.class,
      long.class, Long.class,
      short.class, Short.class);

  /**
   * The default value of every primitive type, written to the property when the component of a
   * primitive-backed field is left empty. A primitive cannot hold {@code null}, so without this the
   * empty component value would reach the setter and the binding would throw.
   */
  private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = Map.of(
      boolean.class, Boolean.FALSE,
      byte.class, (byte) 0,
      char.class, (char) 0,
      double.class, 0d,
      float.class, 0f,
      int.class, 0,
      long.class, 0L,
      short.class, (short) 0);

  /** The conversion error message of the built-in {@code Long} factory. */
  private static final String NOT_A_NUMBER = "Not a valid number";

  private static final Map<Class<?>, ComponentFactory> globalFactories = new ConcurrentHashMap<>();

  static {
    setDefaultComponentFactory(String.class, TextField::new);
    setDefaultComponentFactory(Integer.class, IntegerField::new);
    setDefaultComponentFactory(Double.class, NumberField::new);
    setDefaultComponentFactory(Long.class, TextField::new, createLongConverter());
    setDefaultComponentFactory(Float.class, NumberField::new, createFloatConverter());
    setDefaultComponentFactory(BigDecimal.class, BigDecimalField::new);
    setDefaultComponentFactory(Boolean.class, Checkbox::new);
    setDefaultComponentFactory(LocalDate.class, DatePicker::new);
    setDefaultComponentFactory(LocalDateTime.class, DateTimePicker::new);
    setDefaultComponentFactory(LocalTime.class, TimePicker::new);
  }

  /** The bean type this form was created for. */
  @Getter
  private final Class<T> beanType;

  /** The internal binder that manages data binding and validation. */
  private final Binder<T> binder;

  /**
   * Whether the fields have been bound. Until then, configuring a field only collects the
   * configuration; see {@link #bindPendingFields()}.
   */
  private boolean bound;

  private final Map<String, Field<?>> fields = new LinkedHashMap<>();

  private final Map<Class<?>, ComponentFactory> instanceFactories = new HashMap<>();

  /**
   * The internal layout where the generated fields are placed.
   *
   * <p>
   * The layout is owned by the form: it is emptied and repopulated whenever the set of visible
   * fields or their order changes — which every field state change, component replacement, column
   * span and {@code setFieldOrder} call can cause. Components added to it directly do not survive
   * that refresh, so extra content belongs next to the form and not inside its layout. What is
   * safe to use here is the configuration of the layout itself, such as
   * {@link FormLayout#setResponsiveSteps(ResponsiveStep...)}, which the refresh does not touch.
   */
  @Getter
  private final FormLayout formLayout = new FormLayout();

  private final HorizontalLayout buttonBar = new HorizontalLayout();

  /** The save button. Only displayed after a save action is set. */
  @Getter
  private final Button saveButton = new Button("Save");

  /** The cancel button. Only displayed after a cancel action is set. */
  @Getter
  private final Button cancelButton = new Button("Cancel");

  /**
   * The bean the form is currently editing or was populated from, and the instance that
   * {@link #getValidBean()} writes to. It is {@code null} when no bean was set, and also after
   * {@link #clear()}, which detaches it.
   */
  @Getter
  private T bean;

  /** The last bean set, remembered so that {@link #reset()} can restore it after a clear. */
  private T lastBean;

  /** The texts of the button bar. */
  @Getter
  private EasyFormI18n i18n = new EasyFormI18n();

  private Set<String> fieldOrder;
  private SerializableFunction<String, String> labelGenerator;
  private boolean writeThrough;
  private SerializableConsumer<T> saveAction;
  private SerializableRunnable cancelAction;
  private Boolean saveButtonVisibleOverride;
  private Boolean cancelButtonVisibleOverride;

  /**
   * Creates a form whose fields are generated from the properties of the given bean type.
   *
   * @param beanType the bean type to generate the form for, not {@code null}
   * @throws NullPointerException if {@code beanType} is {@code null}
   * @throws IllegalArgumentException if {@code beanType} cannot be introspected
   */
  public EasyForm(final Class<T> beanType) {
    this.beanType = Objects.requireNonNull(beanType, "beanType cannot be null");
    binder = createBinder(beanType);
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    saveButton.addClickListener(event -> save());
    cancelButton.addClickListener(event -> cancel());
    buttonBar.add(saveButton, cancelButton);
    getContent().setPadding(false);
    getContent().add(formLayout, buttonBar);
    discoverFields();
    refreshLayout();
    updateButtonBar();
  }

  // -- component factories --

  /**
   * Registers a global default component factory for the given value type. The factory applies to
   * all {@code EasyForm} instances created after this call, unless overridden per form instance or
   * per property.
   *
   * <p>
   * The built-in type mappings (e.g. {@code String} to {@link TextField}) are registered through
   * this same registry and can be replaced by calling this method.
   *
   * @param <V> the value type
   * @param <C> the component type, which must be a {@link Component} and a {@link HasValue} of the
   *     value type
   * @param type the value type to register the factory for, not {@code null}
   * @param factory the factory that creates a component for the type, not {@code null}
   * @throws NullPointerException if {@code type} or {@code factory} is {@code null}
   */
  public static <V, C extends Component & HasValue<?, V>> void setDefaultComponentFactory(
      final Class<V> type, final SerializableSupplier<C> factory) {
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(factory, "factory cannot be null");
    globalFactories.put(wrap(type), new ComponentFactory(factory, null));
  }

  /**
   * Registers a global default component factory for the given property type, together with a
   * converter that adapts the component presentation type to the property type (e.g. a
   * {@link TextField} whose {@code String} value is converted to a {@code Long} property). The
   * factory applies to all {@code EasyForm} instances created after this call, unless overridden
   * per form instance or per property.
   *
   * @param <V> the presentation value type of the created components
   * @param <P> the property type
   * @param <C> the component type, which must be a {@link Component} and a {@link HasValue} of the
   *     presentation value type
   * @param propertyType the property type to register the factory for, not {@code null}
   * @param factory the factory that creates a component for the type, not {@code null}
   * @param converter the converter from the presentation type to the property type, not {@code
   *     null}
   * @throws NullPointerException if {@code propertyType}, {@code factory} or {@code converter} is
   *     {@code null}
   */
  public static <V, P, C extends Component & HasValue<?, V>> void setDefaultComponentFactory(
      final Class<P> propertyType, final SerializableSupplier<C> factory,
      final Converter<V, P> converter) {
    Objects.requireNonNull(propertyType, "propertyType cannot be null");
    Objects.requireNonNull(factory, "factory cannot be null");
    Objects.requireNonNull(converter, "converter cannot be null");
    globalFactories.put(wrap(propertyType), new ComponentFactory(factory, converter));
  }

  /**
   * Registers a component factory for the given value type in this form instance, overriding the
   * global defaults. Fields already generated for properties of this type are recreated, unless a
   * custom component was set for them via {@link Field#withComponent}.
   *
   * @param <V> the value type
   * @param <C> the component type, which must be a {@link Component} and a {@link HasValue} of the
   *     value type
   * @param type the value type to register the factory for, not {@code null}
   * @param factory the factory that creates a component for the type, not {@code null}
   * @throws NullPointerException if {@code type} or {@code factory} is {@code null}
   */
  public <V, C extends Component & HasValue<?, V>> void setComponentFactory(final Class<V> type,
      final SerializableSupplier<C> factory) {
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(factory, "factory cannot be null");
    setComponentFactory(wrap(type), new ComponentFactory(factory, null));
  }

  /**
   * Registers a component factory for the given property type in this form instance, together with
   * a converter that adapts the component presentation type to the property type, overriding the
   * global defaults. Fields already generated for properties of this type are recreated, unless a
   * custom component was set for them via {@link Field#withComponent}.
   *
   * @param <V> the presentation value type of the created components
   * @param <P> the property type
   * @param <C> the component type, which must be a {@link Component} and a {@link HasValue} of the
   *     presentation value type
   * @param propertyType the property type to register the factory for, not {@code null}
   * @param factory the factory that creates a component for the type, not {@code null}
   * @param converter the converter from the presentation type to the property type, not {@code
   *     null}
   * @throws NullPointerException if {@code propertyType}, {@code factory} or {@code converter} is
   *     {@code null}
   */
  public <V, P, C extends Component & HasValue<?, V>> void setComponentFactory(
      final Class<P> propertyType, final SerializableSupplier<C> factory,
      final Converter<V, P> converter) {
    Objects.requireNonNull(propertyType, "propertyType cannot be null");
    Objects.requireNonNull(factory, "factory cannot be null");
    Objects.requireNonNull(converter, "converter cannot be null");
    setComponentFactory(wrap(propertyType), new ComponentFactory(factory, converter));
  }

  private void setComponentFactory(final Class<?> propertyType, final ComponentFactory factory) {
    instanceFactories.put(propertyType, factory);
    fields.values().stream()
        .filter(field -> !field.componentExplicit && field.propertyType == propertyType)
        .forEach(field -> {
          resolveComponent(field);
          invalidateBinding(field);
        });
    refreshLayout();
  }

  // -- field access and bulk configuration --

  /**
   * Returns the configuration wrapper for the field generated for the given property.
   *
   * <p>
   * The presentation value type of a field cannot be inferred from a property name, so the wrapper
   * is returned with a wildcard type. The state, presentation and layout methods chain as usual, as
   * does {@link Field#withComponent}, which takes its type from its argument. The two
   * methods that take the presentation value type as a parameter,
   * {@link Field#withValidator(Validator)} and
   * {@link Field#withConverter(Converter)}, are only callable on a wrapper obtained through
   * {@link #configureField(String, Class)}.
   *
   * @param propertyName the name of the bean property
   * @return the field wrapper
   * @throws IllegalArgumentException if no property with the given name was discovered
   */
  public Field<?> configureField(final String propertyName) {
    final Field<?> field = fields.get(propertyName);
    if (field == null) {
      throw new IllegalArgumentException(
          "No property '" + propertyName + "' in " + beanType.getName());
    }
    return field;
  }

  /**
   * Returns the configuration wrapper for the field generated for the given property, typed to the
   * given presentation value type. The type is inferred from the argument, so the wrapper can be
   * configured in a fluent chain without an explicit type argument:
   *
   * <pre>{@code
   * form.configureField("email", String.class).withValidator(new EmailValidator("Invalid email"));
   * }</pre>
   *
   * <p>
   * The given type is checked against the value type of the component currently generated for the
   * property, which is the type the field's validators and converters see. Note that this is the
   * presentation type and not necessarily the property type: a {@code Long} property bound through
   * the built-in {@link TextField} factory has a presentation type of {@code String}. The check is
   * skipped for properties without a component, and for components whose value type cannot be
   * resolved (such as {@link ComboBox}).
   *
   * <p>
   * Because the check reflects the component in place at the time of the call, replacing the
   * component for a property is done through {@link #configureField(String)} and
   * {@link Field#withComponent}, which types the returned wrapper after the new
   * component.
   *
   * @param <V> the presentation value type of the field
   * @param propertyName the name of the bean property
   * @param valueType the expected presentation value type, not {@code null}
   * @return the field wrapper
   * @throws NullPointerException if {@code valueType} is {@code null}
   * @throws IllegalArgumentException if no property with the given name was discovered, or if the
   *     component of the property does not have the given presentation value type
   */
  @SuppressWarnings("unchecked")
  public <V> Field<V> configureField(final String propertyName, final Class<V> valueType) {
    Objects.requireNonNull(valueType, "valueType cannot be null");
    final Field<?> field = configureField(propertyName);
    final Class<?> presentationType = presentationTypeOf(field.component);
    if (presentationType != null && !valueType.isAssignableFrom(presentationType)) {
      throw new IllegalArgumentException("The component of property '" + propertyName + "' has "
          + presentationType.getName() + " as its presentation value type, which is not compatible "
          + "with " + valueType.getName());
    }
    return (Field<V>) field;
  }

  /**
   * Returns the component generated for the given property, which is what
   * {@link #configureField(String)} wraps. Use this when the component itself is wanted rather than
   * its configuration:
   *
   * <pre>{@code
   * TextField email = (TextField) form.field("email");
   * }</pre>
   *
   * @param propertyName the name of the bean property
   * @return the component, or {@code null} if the property type has no component factory
   * @throws IllegalArgumentException if no property with the given name was discovered
   */
  public HasValue<?, ?> field(final String propertyName) {
    return configureField(propertyName).getComponent();
  }

  /**
   * Returns the component generated for the given property, typed to the given presentation value
   * type:
   *
   * <pre>{@code
   * HasValue<?, String> email = form.field("email", String.class);
   * }</pre>
   *
   * @param <V> the presentation value type of the component
   * @param propertyName the name of the bean property
   * @param valueType the expected presentation value type, not {@code null}
   * @return the component, or {@code null} if the property type has no component factory
   * @throws NullPointerException if {@code valueType} is {@code null}
   * @throws IllegalArgumentException if no property with the given name was discovered, or if the
   *     component of the property does not have the given presentation value type
   * @see #configureField(String, Class)
   */
  public <V> HasValue<?, V> field(final String propertyName, final Class<V> valueType) {
    return configureField(propertyName, valueType).getComponent();
  }

  /**
   * Returns the configuration wrapper for the given property, or an empty optional if the property
   * was not discovered. This is the non-throwing counterpart of {@link #configureField(String)}.
   *
   * @param propertyName the name of the bean property
   * @return the field wrapper, or an empty optional
   */
  public Optional<Field<?>> findField(final String propertyName) {
    return Optional.ofNullable(fields.get(propertyName));
  }

  /**
   * Returns the names of the discovered properties, in the order the fields are displayed.
   *
   * @return an unmodifiable list of property names
   */
  public List<String> getFieldNames() {
    return orderedFields().stream().map(Field::getPropertyName)
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Returns the configuration wrappers of the discovered properties, in the order the fields are
   * displayed. Excluded fields are included in the result.
   *
   * @return an unmodifiable list of field wrappers
   */
  public List<Field<?>> getFields() {
    return List.copyOf(orderedFields());
  }

  /**
   * Sets the display order of the fields. The listed properties are shown first, in the given
   * order, followed by every field that was not listed, in declaration order. Repeated names are
   * ignored after their first occurrence.
   *
   * <p>
   * This method only reorders: a field left out of the list is still shown and still bound. Use
   * {@link #setVisibleFields(String...)} to restrict which fields the form shows.
   *
   * @param propertyNames the names of the properties to display first, in order
   * @return this, for method chaining
   * @throws NullPointerException if the array or any of its elements is {@code null}
   * @throws IllegalArgumentException if any property name is unknown
   */
  public EasyForm<T> setFieldOrder(final String... propertyNames) {
    final Set<String> order = new LinkedHashSet<>(List.of(propertyNames));
    requireKnownProperties(order);
    fieldOrder = order;
    refreshLayout();
    return this;
  }

  /**
   * Restricts the form to the given properties: every field that is not listed is excluded, and
   * every listed field that was excluded is included again. This is the bulk complement of
   * {@link Field#excluded()} and operates on the same per-field state, so an individual
   * field can still be brought back afterwards with {@link Field#visible()}. A listed field
   * that is read-only stays read-only.
   *
   * <p>
   * The display order is unaffected — use {@link #setFieldOrder(String...)} for that.
   *
   * @param propertyNames the names of the properties to show
   * @return this, for method chaining
   * @throws NullPointerException if the array or any of its elements is {@code null}
   * @throws IllegalArgumentException if any property name is unknown
   */
  public EasyForm<T> setVisibleFields(final String... propertyNames) {
    final Set<String> visible = new LinkedHashSet<>(List.of(propertyNames));
    requireKnownProperties(visible);
    for (final Field<?> field : fields.values()) {
      if (!visible.contains(field.propertyName)) {
        field.state = Field.State.EXCLUDED;
      } else if (field.state == Field.State.EXCLUDED) {
        field.state = Field.State.VISIBLE;
      }
    }
    fields.values().forEach(this::invalidateBinding);
    refreshLayout();
    return this;
  }

  // -- binding --

  /**
   * Returns the internal binder, for the parts of {@link Binder} this form does not wrap —
   * bean-level validators, {@code hasChanges}, or the {@link Binding} of an individual field.
   *
   * <p>
   * Reading the binder builds the bindings of the fields configured so far, if they were not built
   * already. Configuration applied here to the binding of a field is therefore kept only until that
   * field is reconfigured through this form — {@link Field#withComponent}, {@link Field#excluded()}
   * or {@link #setComponentFactory(Class, SerializableSupplier)} rebuild the binding and start it
   * over. Configure the fields first and reach for the binder afterwards.
   *
   * @return the internal binder
   */
  public Binder<T> getBinder() {
    bindPendingFields();
    return binder;
  }

  /**
   * Builds the binding of every field that is waiting for one, and marks the form as bound. Called
   * the first time the form interacts with the binder or with a bean, and when the form is
   * attached, so that a form is configured completely before anything is bound: while the fields
   * are unbound, configuring one only collects the configuration, and the whole form is bound in
   * one pass afterwards.
   */
  private void bindPendingFields() {
    bound = true;
    for (final Field<?> field : fields.values()) {
      if (field.bindingPending) {
        field.bindingPending = false;
        rebind(field);
      }
    }
  }

  /**
   * Marks the binding of the given field as out of date. Before the form is bound this only
   * records that the field is waiting for a binding; afterwards the binding is rebuilt right away,
   * so that reconfiguring a field always takes effect on the next value change.
   */
  private void invalidateBinding(final Field<?> field) {
    if (bound) {
      field.bindingPending = false;
      rebind(field);
    } else {
      field.bindingPending = true;
      // there is no binding to carry these yet, so they go on the component, which keeps the
      // deferral invisible to whoever looks at the field before the form is bound
      applyReadOnly(field);
      if (field.requiredMessage != null && field.component != null) {
        field.component.setRequiredIndicatorVisible(true);
      }
    }
  }

  @Override
  protected void onAttach(final AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    bindPendingFields();
  }

  // -- bean handling --

  /**
   * Binds the given bean to the form in edit mode: fields are populated from the bean and valid
   * value changes are written through to it.
   *
   * @param bean the bean to edit, or {@code null} to clear the form
   * @return this, for method chaining
   * @throws IllegalStateException if a field has a component whose value type cannot be written to
   *     its property and no converter was set for it
   */
  public EasyForm<T> setBean(final T bean) {
    checkFieldTypes();
    this.bean = bean;
    lastBean = bean;
    writeThrough = bean != null;
    binder.setBean(bean);
    return this;
  }

  /**
   * Populates the fields with values from the given bean without live binding. Changes are not
   * written to the bean until {@link #getValidBean()} or the save action runs.
   *
   * @param bean the bean to read values from, or {@code null} to clear the form
   * @return this, for method chaining
   * @throws IllegalStateException if a field has a component whose value type cannot be written to
   *     its property and no converter was set for it
   */
  public EasyForm<T> readBean(final T bean) {
    checkFieldTypes();
    this.bean = bean;
    lastBean = bean;
    writeThrough = false;
    if (binder.getBean() != null) {
      binder.removeBean();
    }
    binder.readBean(bean);
    return this;
  }

  /**
   * Validates the form and returns the bean with the current field values written to it. When no
   * bean is attached — because none was set, or because {@link #clear()} detached it — the values
   * are written to a new instance, and the bean type must have an accessible no-args constructor.
   *
   * <p>
   * Use {@link #validate()} instead when the reason for a validation failure is needed.
   *
   * @return the populated bean, or an empty optional if validation failed
   * @throws IllegalStateException if no bean has been set and the bean type cannot be instantiated,
   *     or if a field has a component whose value type cannot be written to its property and no
   *     converter was set for it
   */
  public Optional<T> getValidBean() {
    checkFieldTypes();
    if (binder.getBean() != null) {
      return binder.validate().isOk() ? Optional.of(binder.getBean()) : Optional.empty();
    }
    final T target = bean != null ? bean : createBeanInstance();
    return binder.writeBeanIfValid(target) ? Optional.of(target) : Optional.empty();
  }

  /**
   * Validates every bound field and returns the resulting status, which carries the individual
   * error messages. Nothing is written to a bean, so this can be called to drive the state of the
   * surrounding UI. Follows {@link Binder#validate()} semantics.
   *
   * @return the validation status
   * @throws IllegalStateException if a field has a component whose value type cannot be written to
   *     its property and no converter was set for it
   */
  public BinderValidationStatus<T> validate() {
    checkFieldTypes();
    return binder.validate();
  }

  /**
   * Adds a listener notified whenever the validation status of the form changes, which is the
   * supported way to keep the surrounding UI — a save button, a summary — in step with the form.
   * The event carries the errors, and its binder answers {@code hasChanges()} for dirty state.
   *
   * <pre>{@code
   * form.addStatusChangeListener(
   *     event -> saveButton.setEnabled(!event.hasValidationErrors() && event.getBinder()
   *         .hasChanges()));
   * }</pre>
   *
   * @param listener the listener to add, not {@code null}
   * @return a registration for removing the listener
   */
  public Registration addStatusChangeListener(final StatusChangeListener listener) {
    bindPendingFields();
    return binder.addStatusChangeListener(Objects.requireNonNull(listener,
        "listener cannot be null"));
  }

  /**
   * Adds a listener notified whenever the value of any bound field changes, whether or not the new
   * value is valid.
   *
   * @param listener the listener to add, not {@code null}
   * @return a registration for removing the listener
   */
  public Registration addValueChangeListener(
      final HasValue.ValueChangeListener<? super HasValue.ValueChangeEvent<?>> listener) {
    bindPendingFields();
    return binder.addValueChangeListener(Objects.requireNonNull(listener,
        "listener cannot be null"));
  }

  /**
   * Resets the fields to the values of the last bean set through {@link #setBean(Object)} or
   * {@link #readBean(Object)}, re-attaching it if it was detached by {@link #clear()}. If no bean
   * was ever set, all fields are cleared.
   *
   * @return this, for method chaining
   */
  public EasyForm<T> reset() {
    bindPendingFields();
    bean = lastBean;
    if (writeThrough) {
      binder.setBean(lastBean);
    } else {
      binder.readBean(lastBean);
    }
    return this;
  }

  /**
   * Clears all fields and detaches the current bean, so that the cleared values are never written
   * to it: after this call {@code getBean()} returns {@code null} and {@link #getValidBean()}
   * writes to a new instance. The last bean set is remembered and can be restored with
   * {@link #reset()}.
   *
   * @return this, for method chaining
   */
  public EasyForm<T> clear() {
    bindPendingFields();
    if (binder.getBean() != null) {
      // detach the bean before clearing, so that cleared values are not written through
      binder.setBean(null);
    } else {
      binder.readBean(null);
    }
    bean = null;
    return this;
  }

  /**
   * Adds a bean-level (cross-field) validator. Bean validators run after all field-level validators
   * have passed.
   *
   * @param validator the bean validator to add, not {@code null}
   * @return this, for method chaining
   * @throws NullPointerException if {@code validator} is {@code null}
   */
  public EasyForm<T> addBeanValidator(final Validator<? super T> validator) {
    binder.withValidator(Objects.requireNonNull(validator, "validator cannot be null"));
    return this;
  }

  // -- button bar --

  /**
   * Sets the action invoked with the validated bean when the save button is clicked, and makes the
   * save button visible.
   *
   * @param saveAction the save action, or {@code null} to remove it
   * @return this, for method chaining
   */
  public EasyForm<T> setSaveAction(final SerializableConsumer<T> saveAction) {
    this.saveAction = saveAction;
    updateButtonBar();
    return this;
  }

  /**
   * Sets the action invoked when the cancel button is clicked, and makes the cancel button visible.
   *
   * @param cancelAction the cancel action, or {@code null} to remove it
   * @return this, for method chaining
   */
  public EasyForm<T> setCancelAction(final SerializableRunnable cancelAction) {
    this.cancelAction = cancelAction;
    updateButtonBar();
    return this;
  }

  /**
   * Sets the texts of the button bar.
   *
   * @param i18n the texts to use, not {@code null}
   * @return this, for method chaining
   * @throws NullPointerException if {@code i18n} is {@code null}
   */
  public EasyForm<T> setI18n(final EasyFormI18n i18n) {
    this.i18n = Objects.requireNonNull(i18n, "i18n cannot be null");
    saveButton.setText(i18n.getSave());
    cancelButton.setText(i18n.getCancel());
    return this;
  }

  /**
   * Sets the function that generates the label of every field whose label was not set explicitly
   * through {@link Field#withLabel(String)}, replacing the default derivation from the
   * property name. Pass {@code null} to restore the default.
   *
   * <p>
   * This is the bulk counterpart of overriding {@link #createLabel(String)}, and applies
   * immediately to the fields already generated.
   *
   * @param labelGenerator the generator, taking a property name and returning a label, or
   * @return this, for method chaining
   *     {@code null} for the default
   */
  public EasyForm<T> setLabelGenerator(final SerializableFunction<String, String> labelGenerator) {
    this.labelGenerator = labelGenerator;
    fields.values().forEach(this::applyPresentation);
    return this;
  }

  /**
   * Overrides the visibility of the save button. By default the button is visible if and only if a
   * save action has been set.
   *
   * @param visible whether the save button is visible
   * @return this, for method chaining
   */
  public EasyForm<T> setSaveButtonVisible(final boolean visible) {
    saveButtonVisibleOverride = visible;
    updateButtonBar();
    return this;
  }

  /**
   * Overrides the visibility of the cancel button. By default the button is visible if and only if
   * a cancel action has been set.
   *
   * @param visible whether the cancel button is visible
   * @return this, for method chaining
   */
  public EasyForm<T> setCancelButtonVisible(final boolean visible) {
    cancelButtonVisibleOverride = visible;
    updateButtonBar();
    return this;
  }

  /**
   * Adds an extra button to the button bar.
   *
   * @param text the button text
   * @param clickListener the click listener
   * @param variants the theme variants to apply, if any
   * @return the added button
   */
  public Button addButton(final String text,
      final ComponentEventListener<ClickEvent<Button>> clickListener,
      final ButtonVariant... variants) {
    final Button button = new Button(text, clickListener);
    button.addThemeVariants(variants);
    buttonBar.add(button);
    updateButtonBar();
    return button;
  }

  /**
   * Adds an extra button with an icon to the button bar.
   *
   * @param text the button text
   * @param icon the button icon
   * @param clickListener the click listener
   * @param variants the theme variants to apply, if any
   * @return the added button
   */
  public Button addButton(final String text, final Component icon,
      final ComponentEventListener<ClickEvent<Button>> clickListener,
      final ButtonVariant... variants) {
    final Button button = addButton(text, clickListener, variants);
    button.setIcon(icon);
    return button;
  }

  /**
   * Removes a button previously added with {@link #addButton}.
   *
   * @param button the button to remove, not {@code null}
   * @return this, for method chaining
   * @throws NullPointerException if {@code button} is {@code null}
   * @throws IllegalArgumentException if the button is not in the button bar, or is the save or
   *     cancel button — use {@link #setSaveButtonVisible(boolean)} or
   *     {@link #setCancelButtonVisible(boolean)} for those
   */
  public EasyForm<T> removeButton(final Button button) {
    Objects.requireNonNull(button, "button cannot be null");
    if (button == saveButton || button == cancelButton) {
      throw new IllegalArgumentException(
          "The save and cancel buttons cannot be removed; hide them instead");
    }
    if (buttonBar.getChildren().noneMatch(child -> child == button)) {
      throw new IllegalArgumentException("The given button is not in the button bar");
    }
    buttonBar.remove(button);
    updateButtonBar();
    return this;
  }

  // -- layout --

  /**
   * Enables or disables the form. In addition to disabling the generated components and the button
   * bar — which the inherited {@link HasEnabled} behaviour already does, since they are all in this
   * component's element tree — this makes every binding read-only, so that a disabled form cannot
   * be written to the bean even programmatically.
   *
   * <p>
   * Re-enabling restores the read-only state each field was configured with, so a field made
   * read-only through {@link Field#readOnly()} stays read-only.
   *
   * @param enabled whether the form is enabled
   */
  @Override
  public void setEnabled(final boolean enabled) {
    HasEnabled.super.setEnabled(enabled);
    fields.values().forEach(this::applyReadOnly);
  }

  /**
   * Configures the responsive steps of the internal form layout.
   *
   * @param steps the responsive steps
   * @return this, for method chaining
   * @see FormLayout#setResponsiveSteps(ResponsiveStep...)
   */
  public EasyForm<T> setResponsiveSteps(final ResponsiveStep... steps) {
    formLayout.setResponsiveSteps(steps);
    return this;
  }

  // -- internals --

  private Binder<T> createBinder(final Class<T> beanType) {
    try {
      return new BeanValidationBinder<>(beanType);
    } catch (final IllegalStateException e) {
      logger.debug("No JSR-380 implementation found; Bean Validation annotations will be ignored",
          e);
      return new Binder<>(beanType);
    }
  }

  private void discoverFields() {
    PropertyDescriptor[] descriptors;
    try {
      descriptors = Introspector.getBeanInfo(beanType, Object.class).getPropertyDescriptors();
    } catch (final IntrospectionException e) {
      throw new IllegalArgumentException("Cannot introspect " + beanType.getName(), e);
    }
    final Map<String, Integer> declarationOrder = declarationOrder();
    final Comparator<PropertyDescriptor> comparator = Comparator
        .<PropertyDescriptor>comparingInt(
            pd -> declarationOrder.getOrDefault(pd.getName(), Integer.MAX_VALUE))
        .thenComparing(PropertyDescriptor::getName);
    final List<PropertyDescriptor> properties = List.of(descriptors).stream()
        .filter(pd -> pd.getReadMethod() != null && pd.getWriteMethod() != null)
        .filter(this::includeProperty).sorted(comparator).collect(Collectors.toList());
    for (final PropertyDescriptor property : properties) {
      final Class<?> propertyType = property.getPropertyType();
      final Field<?> field = new Field<>(this, property.getName(), wrap(propertyType),
          PRIMITIVE_DEFAULTS.get(propertyType));
      resolveComponent(field);
      if (field.component == null) {
        logger.warn("Property '{}' of type {} has no component factory and will be ignored",
            property.getName(), property.getPropertyType().getName());
      }
      fields.put(property.getName(), field);
    }
  }

  private Map<String, Integer> declarationOrder() {
    final List<Class<?>> hierarchy = new ArrayList<>();
    for (Class<?> clazz = beanType; clazz != null && clazz != Object.class; clazz =
        clazz.getSuperclass()) {
      hierarchy.add(0, clazz);
    }
    final Map<String, Integer> order = new HashMap<>();
    int index = 0;
    for (final Class<?> clazz : hierarchy) {
      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        order.putIfAbsent(field.getName(), index++);
      }
    }
    return order;
  }

  @SuppressWarnings("unchecked")
  private <V> void resolveComponent(final Field<V> field) {
    if (field.componentExplicit) {
      return;
    }
    final ComponentFactory factory = factoryFor(field.propertyType);
    final HasValue<?, ?> component = factory != null ? factory.supplier.get()
        : createComponent(field.propertyName, field.propertyType);
    field.component = (HasValue<?, V>) component;
    field.factoryConverter = factory != null ? factory.converter : null;
    applyPresentation(field);
    if (component != null) {
      configureComponent(field.propertyName, component);
    }
  }

  private ComponentFactory factoryFor(final Class<?> propertyType) {
    final ComponentFactory factory = instanceFactories.get(propertyType);
    return factory != null ? factory : globalFactories.get(propertyType);
  }

  /**
   * Decides whether a discovered property becomes a field. Called during construction for every
   * property that has both a getter and a setter; returning {@code false} leaves the property out
   * of the form and the binding entirely. The default implementation accepts every property.
   *
   * @param property the property being considered
   * @return whether to generate a field for the property
   */
  protected boolean includeProperty(final PropertyDescriptor property) {
    return true;
  }

  /**
   * Creates the component for a property that has no registered component factory. The default
   * implementation returns a {@link ComboBox} of the constants for an enum property, and
   * {@code null} for anything else, which leaves the property out of the form with a logged
   * warning.
   *
   * <p>
   * Components for property types that <em>do</em> have a factory are not created here — override
   * them with {@link #setComponentFactory(Class, SerializableSupplier)}, which also carries the
   * converter, or per property with {@link Field#withComponent}. The precedence is
   * {@code withComponent} &gt; registered factory &gt; this method.
   *
   * @param propertyName the name of the property
   * @param propertyType the property type, with primitives already wrapped
   * @return the component to use, or {@code null} to leave the property out of the form
   */
  protected HasValue<?, ?> createComponent(final String propertyName,
      final Class<?> propertyType) {
    return propertyType.isEnum() ? createEnumComboBox(propertyType) : null;
  }

  /**
   * Returns the label for a property whose label was not set explicitly through
   * {@link Field#withLabel(String)}. The default implementation applies the function given
   * to {@link #setLabelGenerator(SerializableFunction)}, or derives the label from the property
   * name when there is none.
   *
   * @param propertyName the name of the property
   * @return the label to use
   */
  protected String createLabel(final String propertyName) {
    return labelGenerator != null ? labelGenerator.apply(propertyName)
        : SharedUtil.camelCaseToHumanFriendly(propertyName);
  }

  /**
   * Called after a generated component has been created and its label, placeholder and helper text
   * applied, for decoration that applies to every field — style names, widths, theme variants. The
   * default implementation does nothing. Not called for components set through
   * {@link Field#withComponent}.
   *
   * @param propertyName the name of the property the component was generated for
   * @param component the generated component
   */
  protected void configureComponent(final String propertyName, final HasValue<?, ?> component) {
    // for subclasses
  }

  /**
   * Returns the value type of the given component as declared by its {@link HasValue}
   * implementation, or {@code null} if the component is {@code null} or its value type cannot be
   * resolved (as is the case for components that are generic in their value type, such as
   * {@link ComboBox}).
   */
  private static Class<?> presentationTypeOf(final HasValue<?, ?> component) {
    if (component == null) {
      return null;
    }
    final List<Class<?>> typeArguments =
        ReflectTools.getGenericInterfaceTypes(component.getClass(), HasValue.class);
    return typeArguments.size() > 1 ? typeArguments.get(1) : null;
  }

  /**
   * Verifies that the value of every bound component can be written to its property. Fields with a
   * converter are skipped, since the converter types cannot be resolved at runtime. This is checked
   * when the form interacts with a bean, and not while a field is being configured, because a
   * component and its converter are necessarily set one after the other.
   */
  private void checkFieldTypes() {
    // an unbound field has nothing to check, so the pending bindings are built first
    bindPendingFields();
    for (final Field<?> field : fields.values()) {
      if (field.binding == null || field.converter != null || field.factoryConverter != null) {
        continue;
      }
      final Class<?> presentationType = presentationTypeOf(field.component);
      if (presentationType != null && !field.propertyType.isAssignableFrom(presentationType)) {
        throw new IllegalStateException("The component of property '" + field.propertyName
            + "' has " + presentationType.getName() + " as its presentation value type, which "
            + "cannot be written to a property of type " + field.propertyType.getName()
            + "; set a converter with Field.withConverter");
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static HasValue<?, ?> createEnumComboBox(final Class<?> type) {
    final ComboBox comboBox = new ComboBox<>();
    comboBox.setItems(type.getEnumConstants());
    return comboBox;
  }

  /**
   * The converter of the built-in {@code Long} factory. {@code Long} is presented as text rather
   * than through a {@link NumberField}, because a {@code double} carries only a 53-bit mantissa:
   * identifiers above 2^53 would be silently rounded, and fractional input silently truncated.
   * {@link StringToLongConverter} is exact over the whole {@code long} range and reports anything
   * else as a conversion error. Register another factory for {@code Long.class} to localize the
   * message or to present the property differently.
   */
  private static Converter<String, Long> createLongConverter() {
    return new LongConverter();
  }

  /**
   * The converter of the built-in {@code Long} factory: a {@link StringToLongConverter} that
   * presents {@code null} as an empty string, which is the null representation a text field needs.
   */
  private static final class LongConverter extends StringToLongConverter {

    private LongConverter() {
      super(NOT_A_NUMBER);
    }

    @Override
    public String convertToPresentation(final Long value, final ValueContext context) {
      return value == null ? "" : super.convertToPresentation(value, context);
    }
  }

  /**
   * The converter of the built-in {@code Float} factory. The presented value is round-tripped
   * through {@link Float#toString(float)}, the shortest decimal that reads back as the same
   * {@code float}, rather than widened directly: plain widening exposes the binary representation,
   * which would present a stored {@code 0.1f} as {@code 0.10000000149011612}.
   */
  private static Converter<Double, Float> createFloatConverter() {
    return Converter.from(value -> Result.ok(value == null ? null : value.floatValue()),
        value -> value == null ? null : Double.valueOf(Float.toString(value)));
  }

  /**
   * Returns a converter that replaces an empty component value with the given default, leaving
   * everything else untouched. Chained last, after any converter of the field, so that it operates
   * on the model value that is about to be written to the property.
   */
  private static <V> Converter<V, V> nullToDefault(final V defaultValue) {
    return Converter.from(value -> Result.ok(value == null ? defaultValue : value),
        value -> value);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private <V> void rebind(final Field<V> field) {
    final HasValue<?, V> component = field.component;
    // A field that is already bound is being reconfigured, and its component may hold input that
    // was not committed to the bean yet; a field that is not is being bound for the first time, or
    // is coming back from being excluded, and takes its value from the bean.
    final boolean rebuild = field.binding != null;
    final V uncommittedValue = rebuild && component != null ? component.getValue() : null;
    if (field.binding != null) {
      binder.removeBinding((Binding<T, ?>) field.binding);
      field.binding = null;
    }
    if (component == null || !isEffectivelyVisible(field)) {
      return;
    }
    BindingBuilder<T, V> builder = binder.forField(component);
    if (field.requiredMessage != null) {
      builder = builder.asRequired(field.requiredMessage);
    }
    for (final Validator<?> validator : field.validators) {
      builder = builder.withValidator((Validator<V>) validator);
    }
    BindingBuilder<T, ?> finalBuilder = builder;
    final Converter<?, ?> converter = field.converter != null ? field.converter : field.factoryConverter;
    if (converter != null) {
      finalBuilder = builder.withConverter((Converter) converter);
    }
    if (field.primitiveDefault != null) {
      finalBuilder = finalBuilder.withConverter((Converter) nullToDefault(field.primitiveDefault));
    }
    final Binding<T, ?> binding = finalBuilder.bind(field.propertyName);
    field.binding = binding;
    applyReadOnly(field);
    if (!writeThrough) {
      if (rebuild) {
        component.setValue(uncommittedValue != null ? uncommittedValue
            : component.getEmptyValue());
      } else if (bean != null) {
        binding.read(bean);
      }
    }
  }

  /**
   * Applies the read-only state of a field, which is read-only when the field was configured that
   * way or when the whole form is disabled. A field that is not bound yet carries the state on its
   * component, and takes it through its binding once it has one.
   */
  private void applyReadOnly(final Field<?> field) {
    final boolean readOnly = !isEnabled() || field.state == Field.State.READ_ONLY;
    if (field.binding != null) {
      field.binding.setReadOnly(readOnly);
    } else if (field.component != null) {
      field.component.setReadOnly(readOnly);
    }
  }

  private void applyPresentation(final Field<?> field) {
    final HasValue<?, ?> component = field.component;
    if (component == null) {
      return;
    }
    if (component instanceof final HasLabel hasLabel) {
      hasLabel.setLabel(field.label != null ? field.label : createLabel(field.propertyName));
    }
    if (field.placeholder != null && component instanceof final HasPlaceholder hasPlaceholder) {
      hasPlaceholder.setPlaceholder(field.placeholder);
    }
    if (field.helperText != null && component instanceof final HasHelper hasHelper) {
      hasHelper.setHelperText(field.helperText);
    }
  }

  private void refreshLayout() {
    formLayout.removeAll();
    for (final Field<?> field : orderedFields()) {
      if (field.component != null && isEffectivelyVisible(field)) {
        final Component component = (Component) field.component;
        formLayout.add(component);
        formLayout.setColspan(component, field.colSpan);
      }
    }
  }

  private void onFieldStateChanged(final Field<?> field) {
    invalidateBinding(field);
    refreshLayout();
  }

  private void onComponentChanged(final Field<?> field) {
    applyPresentation(field);
    invalidateBinding(field);
    refreshLayout();
  }

  /**
   * Rejects unknown property names before any state is mutated, so that a typo cannot leave the
   * form half-configured.
   *
   * @throws IllegalArgumentException if any of the given names is not a discovered property
   */
  private void requireKnownProperties(final Set<String> propertyNames) {
    propertyNames.forEach(this::configureField);
  }

  private List<Field<?>> orderedFields() {
    if (fieldOrder == null) {
      return new ArrayList<>(fields.values());
    }
    final List<Field<?>> ordered =
        fieldOrder.stream().map(fields::get).collect(Collectors.toCollection(ArrayList::new));
    fields.values().stream().filter(field -> !fieldOrder.contains(field.propertyName))
        .forEach(ordered::add);
    return ordered;
  }

  private boolean isEffectivelyVisible(final Field<?> field) {
    return field.state != Field.State.EXCLUDED;
  }

  private void save() {
    if (saveAction != null) {
      getValidBean().ifPresent(saveAction);
    }
  }

  private void cancel() {
    if (cancelAction != null) {
      cancelAction.run();
    }
  }

  private void updateButtonBar() {
    saveButton.setVisible(
        saveButtonVisibleOverride != null ? saveButtonVisibleOverride : saveAction != null);
    cancelButton.setVisible(
        cancelButtonVisibleOverride != null ? cancelButtonVisibleOverride : cancelAction != null);
    buttonBar.setVisible(buttonBar.getChildren().anyMatch(Component::isVisible));
  }

  private T createBeanInstance() {
    try {
      return beanType.getDeclaredConstructor().newInstance();
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot instantiate " + beanType.getName(), e);
    }
  }

  private static Class<?> wrap(final Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    return PRIMITIVE_WRAPPERS.getOrDefault(type, type);
  }

  /** A component factory registration: a component supplier and an optional converter. */
  private static final class ComponentFactory implements Serializable {

    private final SerializableSupplier<? extends HasValue<?, ?>> supplier;
    private final Converter<?, ?> converter;

    private ComponentFactory(final SerializableSupplier<? extends HasValue<?, ?>> supplier,
        final Converter<?, ?> converter) {
      this.supplier = supplier;
      this.converter = converter;
    }
  }

  /** The texts of the button bar, following the Vaadin i18n convention. */
  public static class EasyFormI18n implements Serializable {

    /** The text of the save button. */
    @Getter
    private String save = "Save";

    /** The text of the cancel button. */
    @Getter
    private String cancel = "Cancel";

    /**
     * Sets the text of the save button.
     *
     * @param save the text to use
     * @return this, for method chaining
     */
    public EasyFormI18n setSave(final String save) {
      this.save = save;
      return this;
    }

    /**
     * Sets the text of the cancel button.
     *
     * @param cancel the text to use
     * @return this, for method chaining
     */
    public EasyFormI18n setCancel(final String cancel) {
      this.cancel = cancel;
      return this;
    }
  }

  /**
   * Fluent configuration wrapper for a single field of an {@code EasyForm}.
   *
   * <p>
   * Instances are obtained through {@link EasyForm#configureField(String)} and allow configuring
   * the visibility, presentation, validation, and component of the field generated for a bean
   * property:
   *
   * <pre>{@code
   * form.configureField("email").withLabel("Email Address").withPlaceholder("user@example.com")
   *     .asRequired("Email is required");
   * }</pre>
   *
   * @param <V> the presentation value type of the field component
   */
  public static final class Field<V> implements Serializable {

    /** The states a field can be in. */
    private enum State {
      VISIBLE, READ_ONLY, EXCLUDED
    }

    private final EasyForm<?> form;

    /** The name of the bean property this field is generated for. */
    @Getter
    private final String propertyName;

    private final Class<?> propertyType;

    /**
     * The default value of the property type when the property is primitive, and {@code null}
     * otherwise. Primitive properties are bound through their wrapper, so the component can be
     * empty while the setter cannot take {@code null}.
     */
    private final Object primitiveDefault;

    /**
     * The underlying Vaadin component of this field, or {@code null} if the property type has no
     * component factory.
     */
    @Getter
    private HasValue<?, V> component;

    private boolean componentExplicit;
    private Converter<?, ?> factoryConverter;
    private Converter<?, ?> converter;
    private final List<Validator<?>> validators = new ArrayList<>();
    private String requiredMessage;
    private State state = State.VISIBLE;
    private String label;
    private String placeholder;
    private String helperText;
    private int colSpan = 1;
    private Binding<?, ?> binding;

    /** Whether the binding of this field has still to be built; see {@code bindPendingFields}. */
    private boolean bindingPending = true;

    private Field(final EasyForm<?> form, final String propertyName, final Class<?> propertyType,
        final Object primitiveDefault) {
      this.form = form;
      this.propertyName = propertyName;
      this.propertyType = propertyType;
      this.primitiveDefault = primitiveDefault;
    }

    // -- visibility and state --

    /**
     * Makes this field visible and editable (the default state), reversing {@link #readOnly()} or
     * {@link #excluded()}.
     *
     * @return this, for method chaining
     */
    public Field<V> visible() {
      return setState(State.VISIBLE);
    }

    /**
     * Makes this field read-only: it is displayed and populated from the bean, but cannot be
     * edited.
     *
     * @return this, for method chaining
     */
    public Field<V> readOnly() {
      return setState(State.READ_ONLY);
    }

    /**
     * Excludes this field from the layout and from the binding. The property is neither displayed
     * nor written when the form writes to a bean, so an excluded property keeps whatever value the
     * target instance already had. Reverse it with {@link #visible()}.
     *
     * @return this, for method chaining
     */
    public Field<V> excluded() {
      return setState(State.EXCLUDED);
    }

    // -- labels and presentation --

    /**
     * Sets the label of this field, replacing the label generated from the property name.
     *
     * @param label the label to set
     * @return this, for method chaining
     */
    public Field<V> withLabel(final String label) {
      this.label = label;
      form.applyPresentation(this);
      return this;
    }

    /**
     * Sets the placeholder of this field. Ignored if the component does not support placeholders.
     *
     * @param placeholder the placeholder to set
     * @return this, for method chaining
     */
    public Field<V> withPlaceholder(final String placeholder) {
      this.placeholder = placeholder;
      form.applyPresentation(this);
      return this;
    }

    /**
     * Sets the helper text of this field. Ignored if the component does not support helper texts.
     *
     * @param helperText the helper text to set
     * @return this, for method chaining
     */
    public Field<V> withHelperText(final String helperText) {
      this.helperText = helperText;
      form.applyPresentation(this);
      return this;
    }

    // -- validation --

    /**
     * Makes this field required, showing the given message when it is empty.
     *
     * @param errorMessage the error message to show when the field is empty, not {@code null}
     * @return this, for method chaining
     * @throws NullPointerException if {@code errorMessage} is {@code null}
     */
    public Field<V> asRequired(final String errorMessage) {
      requiredMessage = Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
      form.invalidateBinding(this);
      return this;
    }

    /**
     * Adds a validator to this field. Validators run on the presentation value, in the order they
     * were added and in addition to any Bean Validation annotations on the property. The validator
     * presentation type must match the value type of the field component.
     *
     * @param validator the validator to add, not {@code null}
     * @return this, for method chaining
     * @throws NullPointerException if {@code validator} is {@code null}
     */
    public Field<V> withValidator(final Validator<V> validator) {
      validators.add(Objects.requireNonNull(validator, "validator cannot be null"));
      form.invalidateBinding(this);
      return this;
    }

    /**
     * Sets a converter that adapts the presentation value of the component to the model type of the
     * bean property (e.g. a {@code String} field bound to an {@code Integer} property). The
     * converter presentation type must match the value type of the field component: if the
     * auto-generated component does not match, also set one via {@link #withComponent}.
     *
     * @param <P> the model (property) type
     * @param converter the converter to use, not {@code null}
     * @return this, for method chaining
     * @throws NullPointerException if {@code converter} is {@code null}
     */
    public <P> Field<V> withConverter(final Converter<V, P> converter) {
      this.converter = Objects.requireNonNull(converter, "converter cannot be null");
      form.invalidateBinding(this);
      return this;
    }

    // -- component --

    /**
     * Replaces the auto-generated component of this field with the given one, and types this
     * wrapper after the value type of the new component.
     *
     * <p>
     * A component whose value type differs from the property type needs a converter, which is set
     * afterwards on the returned wrapper:
     *
     * <pre>{@code
     * form.configureField("age").withComponent(new TextField())
     *     .withConverter(new StringToIntegerConverter("Must be a number"));
     * }</pre>
     *
     * The two are therefore not validated here, but when the form first interacts with a bean,
     * through {@link EasyForm#setBean(Object)}, {@link EasyForm#readBean(Object)} or
     * {@link EasyForm#getValidBean()}.
     *
     * <p>
     * The converter and the validators of this field are configured against the presentation type
     * of its component, so replacing the component <em>discards</em> them: whatever
     * {@link #withConverter} and {@link #withValidator} were given no longer applies to the new
     * presentation type, and keeping them would fail with a {@code ClassCastException} the next
     * time the form interacts with a bean. Set them again on the returned wrapper, which is typed
     * after the new component. Everything that does not depend on the presentation type — the
     * label, the placeholder, the helper text, the column span, the required message and the state
     * of the field — is kept.
     *
     * @param <W> the presentation value type of the given component
     * @param <C> the component type, which must be a {@link Component} and a {@link HasValue} of
     *     the presentation value type
     * @param component the component to use, not {@code null}
     * @return this, for method chaining
     * @throws NullPointerException if {@code component} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <W, C extends Component & HasValue<?, W>> Field<W> withComponent(
        final C component) {
      Objects.requireNonNull(component, "component cannot be null");
      this.component = (HasValue<?, V>) component;
      componentExplicit = true;
      factoryConverter = null;
      // Both are typed after the presentation type of the component being replaced.
      converter = null;
      validators.clear();
      form.onComponentChanged(this);
      return (Field<W>) this;
    }

    // -- layout --

    /**
     * Sets the number of columns this field spans in the form layout. Defaults to 1.
     *
     * @param colSpan the column span, at least 1
     * @return this, for method chaining
     * @throws IllegalArgumentException if the given span is less than 1
     */
    public Field<V> withColSpan(final int colSpan) {
      if (colSpan < 1) {
        throw new IllegalArgumentException("colSpan must be at least 1");
      }
      this.colSpan = colSpan;
      form.refreshLayout();
      return this;
    }

    private Field<V> setState(final State state) {
      if (this.state != state) {
        this.state = state;
        form.onFieldStateChanged(this);
      }
      return this;
    }
  }
}
