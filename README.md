[![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/easy-form-add-on)
[![Stars on vaadin.com/directory](https://img.shields.io/vaadin-directory/star/easy-form-add-on.svg)](https://vaadin.com/directory/component/easy-form-add-on)
[![Build Status](https://jenkins.flowingcode.com/job/easy-form-addon/badge/icon)](https://jenkins.flowingcode.com/job/easy-form-addon)
[![Maven Central](https://img.shields.io/maven-central/v/com.flowingcode.vaadin.addons/easy-form-addon)](https://mvnrepository.com/artifact/com.flowingcode.vaadin.addons/easy-form-addon)

# Easy Form Add-on

Vaadin Flow component that automatically generates a fully functional form from a Java POJO definition, using reflection and Bean Validation (JSR-380) annotations. All customization is programmatic through a fluent Java API — the POJO stays clean.

## Features

* Automatic field discovery from POJO properties (getter/setter conventions)
* Type-to-component mapping with sensible Vaadin defaults, overridable globally, per form, or per property
* Automatic data binding and Bean Validation (`@NotNull`, `@Size`, `@Email`, `@Min`, `@Max`, ...)
* Fluent per-field API: labels, placeholders, helper texts, validators, converters, custom components
* Field states: visible, read-only, or hidden
* Configurable save/cancel button bar with support for extra buttons
* Responsive multi-column layout with per-field column spanning

## Online demo

[Online demo here](http://addonsv24.flowingcode.com/easy-form)

## Download release

[Available in Vaadin Directory](https://vaadin.com/directory/component/easy-form-add-on)

### Maven install

Add the following dependencies in your pom.xml file:

```xml
<dependency>
   <groupId>com.flowingcode.vaadin.addons</groupId>
   <artifactId>easy-form-addon</artifactId>
   <version>X.Y.Z</version>
</dependency>
```
<!-- the above dependency should be updated with latest released version information -->

```xml
<repository>
   <id>vaadin-addons</id>
   <url>https://maven.vaadin.com/vaadin-addons</url>
</repository>
```

For SNAPSHOT versions see [here](https://maven.flowingcode.com/snapshots/).

## Building and running demo

- git clone repository
- mvn clean install jetty:run

To see the demo, navigate to http://localhost:8080/

## Release notes

See [here](https://github.com/FlowingCode/EasyFormAddon/releases)

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. 

As first step, please refer to our [Development Conventions](https://github.com/FlowingCode/DevelopmentConventions) page to find information about Conventional Commits & Code Style requirements.

Then, follow these steps for creating a contribution:

- Fork this project.
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- For commit message, use [Conventional Commits](https://github.com/FlowingCode/DevelopmentConventions/blob/main/conventional-commits.md) to describe your change.
- Send a pull request for the original project.
- Comment on the original issue that you have implemented a fix for it.

## License & Author

This add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Easy Form Add-on is written by Flowing Code S.A.

# Developer Guide

## Getting started

```java
// Minimal usage — three lines to a working form
EasyForm<Person> form = new EasyForm<>(Person.class);
form.setSaveAction(person -> personService.save(person));
add(form);
```

Customization is done through a fluent API:

```java
EasyForm<Person> form = new EasyForm<>(Person.class);

// Field order (unlisted fields follow, in declaration order)
form.setFieldOrder("firstName", "lastName", "email", "birthDate");

// Field selection (everything not listed is excluded from the layout and the binding)
form.setVisibleFields("firstName", "lastName", "email", "birthDate");

// Or per field
form.configureField("internalCode").excluded();

// Field-level customization (configureField returns the wrapper, field the component)
form.configureField("email").withLabel("Email Address").asRequired("Email is required");
form.configureField("notes").withComponent(new TextArea()).withColSpan(2);

// Events — enable a save button only while the form is valid and dirty
form.addStatusChangeListener(event ->
    form.getSaveButton().setEnabled(!event.hasValidationErrors() && event.getBinder().hasChanges()));

// Layout
form.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("600px", 2));

// Button bar
form.setSaveAction(person -> personService.save(person));
form.setCancelAction(() -> navigateBack());

// Pre-populate for editing
form.setBean(existingPerson);
```

## Special configuration when using Spring

By default, Vaadin Flow only includes ```com/vaadin/flow/component``` to be always scanned for UI components and views. For this reason, the add-on might need to be whitelisted in order to display correctly. 

To do so, just add ```com.flowingcode``` to the ```vaadin.whitelisted-packages``` property in ```src/main/resources/application.properties```, like:

```vaadin.whitelisted-packages = com.vaadin,org.vaadin,dev.hilla,com.flowingcode```
 
More information on Spring whitelisted configuration [here](https://vaadin.com/docs/latest/integrations/spring/configuration/#configure-packages-scanning).
