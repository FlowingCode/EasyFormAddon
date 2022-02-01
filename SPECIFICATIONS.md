# Easy Form Add-on — Specification

## 1. Overview

The Easy Form Add-on is a Vaadin Flow component that automatically generates a fully functional form from a Java POJO definition. It uses reflection and Bean Validation (JSR-380) annotations to create form fields, configure validations, and manage data binding — all with minimal boilerplate code.

All customization is programmatic through a fluent Java API. The POJO itself stays clean — no add-on-specific annotations are required.

## 2. Core Concepts

### 2.1 Automatic Field Discovery

Given a POJO class `T`, `EasyForm<T>` introspects its properties (via getter/setter conventions) and creates appropriate Vaadin form fields for each one. Properties without both a getter and setter are ignored. Properties whose type has no registered component mapping are also ignored (with a logged warning).

Discovery is **flat**. Nested property paths such as `"address.street"`, which Vaadin's `Binder` does support, are not discovered; `configureField("address.street")` fails like any unknown property, and `findField` returns an empty optional. Reach a nested value through a custom component plus a converter, or flatten the property onto the bean.

A subclass can narrow discovery by overriding `includeProperty(PropertyDescriptor)` — see §3.9.

### 2.2 Type-to-Component Mapping

Each Java type maps to a default Vaadin component:

| Java Type | Vaadin Component | Notes |
|-----------|-----------------|-------|
| `String` | `TextField` | |
| `Integer`, `int` | `IntegerField` | |
| `Long`, `long` | `NumberField` | |
| `Double`, `double`, `Float`, `float` | `NumberField` | |
| `BigDecimal` | `BigDecimalField` | |
| `Boolean`, `boolean` | `Checkbox` | |
| `LocalDate` | `DatePicker` | |
| `LocalDateTime` | `DateTimePicker` | |
| `LocalTime` | `TimePicker` | |
| `Enum<?>` | `ComboBox<E>` | Populated with enum constants |

### 2.3 Customization Levels

Type-to-component mappings can be overridden at three levels, from broadest to most specific:

1. **Global defaults** — Static registry that applies to all `EasyForm` instances. Changed via `EasyForm.setDefaultComponentFactory(Class<V>, SerializableSupplier<C>)`, where `C extends Component & HasValue<?, V>`.
2. **Per-form instance** — Overrides for a specific form. Changed via `form.setComponentFactory(Class<V>, SerializableSupplier<C>)`, with the same bound.
3. **Per-property** — Override for a single property. Changed via `form.configureField("notes").withComponent(new TextArea())`.

The most specific level wins: per-property > per-form > global > built-in defaults.

The `C extends Component & HasValue<?, V>` bound is what makes "the factory must produce a Vaadin `Component`" a compile-time requirement rather than a runtime check.

### 2.4 Data Binding

`EasyForm<T>` internally manages a Vaadin `Binder<T>` that binds each generated field to the corresponding POJO property. The binder supports:

- Automatic two-way binding (read from / write to the POJO)
- Bean Validation integration (`@NotNull`, `@Size`, `@Email`, `@Min`, `@Max`, `@Pattern`, etc.)
- Per-field validators added via the programmatic API
- Per-field converters for type adaptation (e.g. `String` field bound to an `Integer` property)

### 2.5 Field States

Each field can be in one of three states:

| State | Visible | Editable | Included in Binding |
|-------|---------|----------|---------------------|
| **Visible** (default) | Yes | Yes | Yes |
| **Read-only** | Yes | No | Yes (read only) |
| **Excluded** | No | No | No |

The state is per field, set through `configureField(name).visible()` / `.readOnly()` / `.excluded()`. `setVisibleFields(...)` is a bulk operation over that same state — it excludes everything not listed and re-includes everything listed — so there is one source of truth for whether a field is part of the form, and an individual field can always be brought back with `.visible()`.

## 3. API Design

### 3.1 Construction

```java
// Basic: all discovered properties are shown as editable fields
EasyForm<Person> form = new EasyForm<>(Person.class);

// With an existing bean instance (pre-populates fields)
Person person = personService.findById(id);
EasyForm<Person> form = new EasyForm<>(Person.class);
form.setBean(person);
```

Every instance-level configuration method on `EasyForm` returns the form, so form-level and
field-level configuration read the same way:

```java
EasyForm<Person> form = new EasyForm<>(Person.class)
    .setFieldOrder("firstName", "lastName", "email")
    .setLabelGenerator(name -> translate(name))
    .setSaveAction(person -> personService.save(person))
    .setBean(person);
```

`addButton` returns the created `Button` (needed for `removeButton`), the listener methods return a `Registration`, and the inherited `HasSize`/`HasStyle`/`HasEnabled` setters are void as Vaadin declares them.

### 3.2 Field Configuration — Fluent API

```java
EasyForm<Person> form = new EasyForm<>(Person.class);

// Configure individual fields
form.configureField("firstName")
    .withLabel("First Name")
    .withPlaceholder("Enter first name")
    .asRequired("First name is required");

// the value type is only needed for the methods that take it as a parameter
form.configureField("email", String.class)
    .withLabel("Email Address")
    .withValidator(new EmailValidator("Invalid email"));

// Make a field read-only
form.configureField("id").readOnly();

// Exclude a field from the layout and the binding
form.configureField("internalCode").excluded();

// Reorder only — unlisted fields follow, in declaration order, and stay bound
form.setFieldOrder("firstName", "lastName", "email", "birthDate");

// Restrict the form to a subset of properties: everything not listed is excluded,
// and anything listed that was excluded comes back
form.setVisibleFields("firstName", "lastName", "email");

// Use a custom component for a specific field
form.configureField("notes").withComponent(new TextArea());

// Override the component factory for a type in this form instance
form.setComponentFactory(String.class, TextArea::new);
```

### 3.3 Field Wrapper — `EasyForm.Field<V>`

`configureField` returns an `EasyForm.Field` wrapper for configuring a single field; `field` returns the underlying component directly, for when the component is what's wanted:

```java
Field<?>     configureField(String propertyName);
<V> Field<V> configureField(String propertyName, Class<V> valueType);

HasValue<?, ?> field(String propertyName);
<V> HasValue<?, V> field(String propertyName, Class<V> valueType);
```

`field(name)` is exactly `configureField(name).getComponent()`, and the two-argument overload applies the same presentation-type check. Both return `null` for a property whose type has no component factory.

`V` is the **presentation** value type — the value type of the component, which is not necessarily the property type: a `Long` property presented by the built-in `NumberField` factory has a presentation type of `Double`.

A presentation type cannot be inferred from a property name, so the single-argument overload returns a **wildcard**. The state, presentation and layout methods chain as usual, and so does `withComponent`, which takes its type from its argument; only `withValidator` and `withConverter` need a concrete `V`, and on a wildcard they are simply not callable. This is deliberate — it makes an unsound `Field<String> f = form.configureField("age")` a compile error instead of a `ClassCastException` inside the binder later.

The two-argument overload infers `V` from the class literal, and checks it against the value type of the component currently generated for the property:

```java
// V = String, inferred from the class literal
form.configureField("email", String.class).withValidator(new EmailValidator("Invalid email"));

// throws IllegalArgumentException: the component of 'email' presents String, not Integer
form.configureField("email", Integer.class);
```

The check is skipped for properties without a component, and for components whose value type cannot be resolved reflectively (such as `ComboBox`, which is generic in its value type).

```java
public static final class Field<V> {
    // Visibility and state
    Field<V> visible();
    Field<V> readOnly();
    Field<V> excluded();

    // Labels and presentation
    Field<V> withLabel(String label);
    Field<V> withPlaceholder(String placeholder);
    Field<V> withHelperText(String helperText);

    // Validation
    Field<V> asRequired(String errorMessage);
    Field<V> withValidator(Validator<V> validator);

    // Converter (presentation type V to model/property type P)
    <P> Field<V> withConverter(Converter<V, P> converter);

    // Custom component (replaces the auto-generated one for this property,
    // and re-types the wrapper after the value type of the new component)
    <W, C extends Component & HasValue<?, W>> Field<W> withComponent(C component);

    // Layout hint — column span in the form layout
    Field<V> withColSpan(int colSpan);

    // Access the underlying Vaadin component
    HasValue<?, V> getComponent();
}
```

### 3.4 Validators and Converters

Validators and converters are set on the field binding and follow standard Vaadin `Binder` semantics.

```java
// Add a validator to a field
form.configureField("age", Integer.class)
    .withValidator(new IntegerRangeValidator("Must be 0-150", 0, 150));

// Replace the component and add a converter (String field bound to an Integer property).
// withComponent re-types the wrapper to String, so no type argument is needed here.
form.configureField("zipCode")
    .withComponent(new TextField())
    .withConverter(new StringToIntegerConverter("Must be a number"));

// Bean Validation annotations on the POJO are picked up automatically.
// Programmatic validators are applied in addition to annotation-based ones.
```

A component whose value type cannot be written to its property needs a converter. Since the two are necessarily set one after the other, the combination is not validated while the field is being configured, but when the form first interacts with a bean — `setBean`, `readBean` or `getValidBean` — which throws `IllegalStateException` naming the property and both types. Fields with a converter are not checked, because converter types are erased at runtime.

### 3.5 Button Bar

When a save action or cancel action is configured, `EasyForm` renders a horizontal button bar below the fields. The bar is only rendered if at least one action is set.

**Defaults:**
- Save button: label "Save", primary theme variant. Shown when `setSaveAction` is called.
- Cancel button: label "Cancel", tertiary theme variant. Shown when `setCancelAction` is called.

```java
// Configure save/cancel actions (this makes the buttons appear)
form.setSaveAction(person -> personService.save(person));
form.setCancelAction(() -> navigateBack());

// Customize button text through the exposed buttons
form.getSaveButton().setText("Submit");
form.getCancelButton().setText("Discard");

// Or through the i18n object, which is the translation seam for both texts
form.setI18n(new EasyForm.EasyFormI18n().setSave("Guardar").setCancel("Cancelar"));

// Customize button visibility
form.setSaveButtonVisible(false);
form.setCancelButtonVisible(false);

// Extra buttons: theme variants are varargs, and can be removed again
Button reset = form.addButton("Reset", event -> form.reset(), ButtonVariant.LUMO_TERTIARY);
form.removeButton(reset);

// Access the button to further customize style/icon
form.getSaveButton().setIcon(VaadinIcon.CHECK.create());
form.getCancelButton().addThemeVariants(ButtonVariant.LUMO_ERROR);

// Add extra buttons to the bar
form.addButton("Delete", event -> {
    personService.delete(form.getBean());
});

// Add extra button with icon and variant
form.addButton("Send Email", VaadinIcon.ENVELOPE.create(), ButtonVariant.LUMO_TERTIARY, event -> {
    emailService.send(form.getBean());
});
```

### 3.6 Save Action and Validation Flow

When the Save button is clicked:

1. All field-level validators (annotation-based and programmatic) are executed.
2. Bean-level validators (cross-field) are executed.
3. If all validations pass, the binder writes the values to the bean.
4. The registered save action is invoked with the populated bean.
5. If validation fails, error messages are displayed on the relevant fields.

```java
form.setSaveAction(person -> {
    try {
        personService.save(person);
        Notification.show("Saved successfully");
    } catch (Exception e) {
        Notification.show("Error: " + e.getMessage(), 3000, Position.MIDDLE);
    }
});

// Cross-field (bean-level) validation
form.addBeanValidator((person, context) -> {
    if (person.getEndDate() != null && person.getEndDate().isBefore(person.getStartDate())) {
        return ValidationResult.error("End date must be after start date");
    }
    return ValidationResult.ok();
});
```

### 3.7 Layout

`EasyForm` uses a `FormLayout` internally by default, which provides responsive multi-column arrangement and per-field column spanning.

```java
// Configure responsive breakpoints
form.setResponsiveSteps(
    new ResponsiveStep("0", 1),
    new ResponsiveStep("600px", 2),
    new ResponsiveStep("900px", 3)
);

// Make a field span multiple columns
form.configureField("address").withColSpan(2);
```

The defaults are sensible for most forms (single column on small screens, two columns on wider screens). Column span defaults to 1 for all fields.

### 3.8 Read and Write

```java
// Set a bean to populate the form (edit mode — changes written through to the bean)
form.setBean(existingPerson);

// Read values without live binding (display/copy mode)
form.readBean(existingPerson);

// Validate and get the bean with current field values written to it
Optional<Person> result = form.getValidBean();

// Validate without writing, and find out what failed
BinderValidationStatus<Person> status = form.validate();

// Reset form to the last-set bean values, re-attaching the bean if it was cleared
form.reset();

// Clear all fields. This also detaches the bean, so the cleared values can never be
// written to it: getBean() returns null afterwards and getValidBean() targets a new
// instance. reset() brings the bean back.
form.clear();
```

### 3.9 Extension Points

`EasyForm` is not final, and a subclass can influence generation by overriding:

| Method | Called for | Default |
|---|---|---|
| `boolean includeProperty(PropertyDescriptor)` | every property with a getter and a setter | accepts all |
| `HasValue<?,?> createComponent(String, Class<?>)` | property types with **no** registered factory | `ComboBox` for enums, else `null` |
| `String createLabel(String)` | fields with no explicit label | the `setLabelGenerator` function, else camelCase-to-title-case |
| `void configureComponent(String, HasValue<?,?>)` | every generated component, after its label is applied | does nothing |

Component precedence is `withComponent` > registered factory > `createComponent`. To replace the component for a type that *does* have a factory, register one with `setComponentFactory` (which also carries the converter) rather than overriding `createComponent`.

These are called while the constructor discovers the properties, so an override must not depend on state initialized in the subclass constructor body.

### 3.10 Events and Introspection

```java
// Keep a save button in step with validity and dirty state
form.addStatusChangeListener(event ->
    saveButton.setEnabled(!event.hasValidationErrors() && event.getBinder().hasChanges()));

// Every value change, valid or not
form.addValueChangeListener(event -> markDirty());

// What fields exist, in display order
List<String> names = form.getFieldNames();
List<EasyForm.Field<?>> fields = form.getFields();

// Non-throwing lookup
form.findField("email").ifPresent(field -> field.withLabel("E-mail"));
```

Both listener methods return a `Registration`.

`EasyForm` implements `HasSize`, `HasStyle` and `HasEnabled`, so the whole form can be sized, styled and disabled as one component. `setEnabled(false)` does two things:

- the generated components and the button bar are disabled, which the inherited `HasEnabled` behaviour already covers — they are all in the form's element tree, so `isEnabled()` returns `false` for each of them;
- every binding is made read-only, so a disabled form cannot be written to the bean at all, not even programmatically. Values are still *read* into the fields.

Re-enabling restores the read-only state each field was configured with, so a field made read-only through `readOnly()` stays read-only. A binding's read-only state is always derived from the field state and the form's enabled flag together — there is no separate stored copy.

## 4. Default Label Generation

When no explicit label is provided, labels are auto-generated from property names using camelCase-to-title-case conversion:

| Property Name | Generated Label |
|--------------|----------------|
| `firstName` | `First Name` |
| `lastName` | `Last Name` |
| `dateOfBirth` | `Date Of Birth` |
| `email` | `Email` |
| `isActive` | `Is Active` |

Override this in bulk with `setLabelGenerator(name -> ...)`, per field with `withLabel(...)`, or in a subclass by overriding `createLabel(String)`. An explicit `withLabel` always wins.

## 5. Bean Validation Support

The form automatically picks up JSR-380 (Bean Validation) annotations from the POJO:

```java
public class Person {
    @NotNull(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String firstName;

    @Email(message = "Must be a valid email address")
    private String email;

    @Min(value = 0, message = "Age must be positive")
    @Max(value = 150, message = "Age must be less than 150")
    private Integer age;

    @NotNull
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}
```

These annotations are automatically applied as validators on the corresponding form fields. Programmatic validators added via `withValidator()` run in addition to annotation-based ones.

## 6. Serialization

`EasyForm<T>` must be fully serializable for Vaadin session persistence. All internal state, including field configurations, validators, and component references, must be serializable.

## 7. Accessibility

- All generated fields have proper labels (explicit or auto-generated).
- Required fields are marked with appropriate ARIA attributes (handled by Vaadin components when `asRequired` is set).
- Error messages are associated with their corresponding fields.
- Tab order follows field order.

## 8. Usage Example — Complete

```java
// Minimal usage — three lines to a working form
EasyForm<Person> simpleForm = new EasyForm<>(Person.class);
simpleForm.setSaveAction(person -> personService.save(person));
add(simpleForm);

// Customized usage
EasyForm<Person> form = new EasyForm<>(Person.class);

// Field selection and order
form.setFieldOrder("firstName", "lastName", "email", "birthDate", "age", "subscriber");
form.setVisibleFields("firstName", "lastName", "email", "birthDate", "age", "subscriber");

// Field-level customization
form.configureField("email").withLabel("Email Address").asRequired("Email is required");
form.configureField("birthDate").withLabel("Date of Birth");
form.configureField("notes").withComponent(new TextArea()).withColSpan(2);

// Layout
form.setResponsiveSteps(
    new ResponsiveStep("0", 1),
    new ResponsiveStep("600px", 2)
);

// Button bar
form.getSaveButton().setText("Create Person");
form.setSaveAction(person -> {
    personService.save(person);
    Notification.show("Person created");
});
form.setCancelAction(() -> UI.getCurrent().navigate(PersonListView.class));

// Pre-populate for editing
form.setBean(existingPerson);

add(form);
```

## 9. Dependencies

- Vaadin Flow (24.x / 25.x)
- Bean Validation API (Jakarta Validation)
- Lombok (per Flowing Code convention for new add-ons)

## 10. Non-Goals (v1)

- Nested POJO support (flatten or component-factory based — planned for a future version)
- Annotation-based field configuration on the POJO
- Server-side data persistence (consumer's responsibility)
- Built-in navigation or routing
- Complex multi-page wizard forms (single form per instance)
- File upload fields (can be added via custom component factory)
