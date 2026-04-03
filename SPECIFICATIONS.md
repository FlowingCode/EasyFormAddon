# Easy Form Add-on — Specification

## 1. Overview

The Easy Form Add-on is a Vaadin Flow component that automatically generates a fully functional form from a Java POJO (Plain Old Java Object) definition. It uses reflection and Bean Validation (JSR-380) annotations to create form fields, configure validations, and manage data binding — all with minimal boilerplate code.

The add-on provides a clean, fluent Java API for controlling field visibility, editability, layout, button configuration, and action handling.

## 2. Core Concepts

### 2.1 Automatic Field Discovery

Given a POJO class `T`, `EasyForm<T>` introspects its properties (via getter/setter conventions) and creates appropriate Vaadin form fields for each one.

### 2.2 Type-to-Component Mapping

Each Java type maps to a default Vaadin component:

| Java Type | Vaadin Component | Notes |
|-----------|-----------------|-------|
| `String` | `TextField` | |
| `String` (multiline) | `TextArea` | Activated via annotation or API |
| `Integer`, `int` | `IntegerField` | |
| `Long`, `long` | `NumberField` | |
| `Double`, `double`, `Float`, `float` | `NumberField` | |
| `BigDecimal` | `BigDecimalField` | |
| `Boolean`, `boolean` | `Checkbox` | |
| `LocalDate` | `DatePicker` | |
| `LocalDateTime` | `DateTimePicker` | |
| `LocalTime` | `TimePicker` | |
| `Enum<?>` | `ComboBox<E>` | Populated with enum constants |
| `Set<Enum<?>>` | `CheckboxGroup<E>` | Multi-select for enum sets |

Custom type mappings can be registered globally or per-form instance.

### 2.3 Data Binding

`EasyForm<T>` internally manages a Vaadin `Binder<T>` that binds each generated field to the corresponding POJO property. The binder supports:

- Automatic two-way binding (read from / write to the POJO)
- Bean Validation integration (`@NotNull`, `@Size`, `@Email`, `@Min`, `@Max`, `@Pattern`, etc.)
- Manual validation rules added via the API

### 2.4 Field States

Each field can be in one of three states:

| State | Visible | Editable | Included in Binding |
|-------|---------|----------|---------------------|
| **Visible** (default) | Yes | Yes | Yes |
| **Read-only** | Yes | No | Yes (read only) |
| **Hidden** | No | No | No |

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

### 3.2 Field Configuration — Fluent API

```java
EasyForm<Person> form = new EasyForm<>(Person.class);

// Configure individual fields
form.getField("firstName")
    .withLabel("First Name")
    .withPlaceholder("Enter first name")
    .asRequired("First name is required");

form.getField("email")
    .withLabel("Email Address")
    .withValidator(new EmailValidator("Invalid email"));

// Make a field read-only
form.getField("id").readOnly();

// Hide a field
form.getField("internalCode").hidden();

// Set field order (only listed fields are shown, in this order)
form.setFieldOrder("firstName", "lastName", "email", "birthDate", "address");

// Hide multiple fields at once
form.hideFields("id", "createdAt", "updatedAt");

// Make multiple fields read-only
form.readOnlyFields("createdBy", "version");

// Use a custom component for a specific field
form.getField("notes").withComponent(new TextArea());

// Use a custom component factory for a type
form.setComponentFactory(Address.class, () -> new AddressFormField());
```

### 3.3 Field Wrapper — `EasyFormField<V>`

The `getField(String propertyName)` method returns an `EasyFormField<V>` wrapper that provides a fluent interface for configuring individual fields:

```java
public class EasyFormField<V> {
    // Visibility and state
    EasyFormField<V> visible();
    EasyFormField<V> readOnly();
    EasyFormField<V> hidden();

    // Labels and placeholders
    EasyFormField<V> withLabel(String label);
    EasyFormField<V> withPlaceholder(String placeholder);
    EasyFormField<V> withHelperText(String helperText);

    // Validation
    EasyFormField<V> asRequired(String errorMessage);
    EasyFormField<V> withValidator(Validator<V> validator);

    // Custom component
    EasyFormField<V> withComponent(HasValue<?, V> component);

    // Column span in form layout
    EasyFormField<V> withColSpan(int colSpan);

    // Access the underlying Vaadin component
    HasValue<?, V> getComponent();
}
```

### 3.4 Button Configuration

By default, the form displays **Save** and **Cancel** buttons in a horizontal toolbar below the fields.

```java
// Default buttons with actions
form.setSaveAction(person -> personService.save(person));
form.setCancelAction(() -> navigateBack());

// Add a Remove button
form.addButton("Remove", VaadinIcon.TRASH, ButtonVariant.LUMO_ERROR, () -> {
    personService.delete(form.getBean());
});

// Add a custom button
form.addButton("Send Email", VaadinIcon.ENVELOPE, () -> {
    emailService.send(form.getBean());
});

// Remove the default Cancel button
form.removeCancelButton();

// Customize the Save button label
form.setSaveButtonText("Submit");

// Configure button visibility
form.setSaveButtonVisible(boolean visible);
form.setCancelButtonVisible(boolean visible);

// Completely replace the button bar
form.setButtonBarFactory(binder -> {
    HorizontalLayout bar = new HorizontalLayout();
    // ... custom button layout
    return bar;
});
```

### 3.5 Save Action and Validation Flow

When the Save button is clicked:

1. All field validators are executed.
2. Bean-level validators (cross-field) are executed.
3. If all validations pass, the binder writes the values to the bean.
4. The registered save action is invoked with the populated bean.
5. If validation fails, error messages are displayed on the relevant fields.

```java
// Save with result handling
form.setSaveAction(person -> {
    try {
        personService.save(person);
        Notification.show("Saved successfully");
    } catch (Exception e) {
        Notification.show("Error: " + e.getMessage(), 3000, Position.MIDDLE);
    }
});

// Cross-field validation
form.addBeanValidator((person, context) -> {
    if (person.getEndDate() != null && person.getEndDate().isBefore(person.getStartDate())) {
        return ValidationResult.error("End date must be after start date");
    }
    return ValidationResult.ok();
});
```

### 3.6 Form Layout Configuration

```java
// Set the number of columns in the FormLayout
form.setResponsiveSteps(
    new ResponsiveStep("0", 1),
    new ResponsiveStep("600px", 2),
    new ResponsiveStep("900px", 3)
);

// Make a field span multiple columns
form.getField("address").withColSpan(2);
```

### 3.7 Events

```java
// Listen for value changes on a specific field
form.getField("country").addValueChangeListener(event -> {
    // Update available cities based on selected country
    form.getField("city").withItems(getCities(event.getValue()));
});

// Listen for form-level changes
form.addValueChangeListener(event -> {
    // Any field changed
});

// Before-save hook (can cancel save)
form.addBeforeSaveListener(event -> {
    if (!confirmAction()) {
        event.cancel();
    }
});

// After-save hook
form.addAfterSaveListener(event -> {
    Notification.show("Saved: " + event.getBean());
});
```

### 3.8 Read and Write

```java
// Set a bean to populate the form (edit mode)
form.setBean(existingPerson);

// Read values without binding (display mode)
form.readBean(existingPerson);

// Manually get the current bean with form values written to it
Optional<Person> result = form.getValidBean(); // validates first

// Reset form to initial state
form.reset();

// Clear all fields
form.clear();
```

## 4. Default Label Generation

When no explicit label is provided, labels are auto-generated from property names using camelCase-to-title-case conversion:

| Property Name | Generated Label |
|--------------|----------------|
| `firstName` | `First Name` |
| `lastName` | `Last Name` |
| `dateOfBirth` | `Date Of Birth` |
| `email` | `Email` |
| `isActive` | `Is Active` |

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

These annotations are automatically applied as validators on the corresponding form fields.

## 6. Nested POJO Support

For properties that are themselves POJOs (e.g., `Address`), the form can:

- **Flatten** nested properties into the form (default for known types)
- **Use a custom component** registered for that type
- **Ignore** them (hidden by default if no mapping exists)

```java
// Register a component factory for Address type
form.setComponentFactory(Address.class, () -> new AddressFormField());

// Or flatten Address properties into the main form with a prefix
form.flattenNestedBean("address", Address.class);
// This creates fields: address.street, address.city, address.postalCode, etc.
```

## 7. Serialization

`EasyForm<T>` must be fully serializable for Vaadin session persistence. All internal state, including field configurations, validators, and component references, must be serializable.

## 8. Accessibility

- All generated fields must have proper labels (either explicit or auto-generated).
- Required fields must be marked with the appropriate ARIA attributes.
- Error messages must be associated with their corresponding fields.
- The form must be navigable via keyboard (tab order follows field order).

## 9. Usage Example — Complete

```java
// Minimal usage
EasyForm<Person> simpleForm = new EasyForm<>(Person.class);
simpleForm.setSaveAction(person -> personService.save(person));
add(simpleForm);

// Customized usage
EasyForm<Person> form = new EasyForm<>(Person.class);

// Configure fields
form.setFieldOrder("firstName", "lastName", "email", "birthDate", "age", "subscriber");
form.hideFields("id", "createdAt", "updatedAt");
form.getField("email").withLabel("Email Address").asRequired("Email is required");
form.getField("birthDate").withLabel("Date of Birth");

// Configure buttons
form.setSaveButtonText("Create Person");
form.setSaveAction(person -> {
    personService.save(person);
    Notification.show("Person created");
});
form.setCancelAction(() -> UI.getCurrent().navigate(PersonListView.class));

// Pre-populate for editing
form.setBean(existingPerson);

add(form);
```

## 10. Dependencies

- Vaadin Flow (24.x)
- Bean Validation API (Jakarta Validation)
- Lombok (per Flowing Code convention for new add-ons)

## 11. Non-Goals (Out of Scope)

- Server-side data persistence (that is the consumer's responsibility)
- Built-in navigation or routing
- Complex multi-page wizard forms (single form per instance)
- File upload fields (can be added via custom component factory)
