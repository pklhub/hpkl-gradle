# hpkl-gradle

`hpkl-gradle` is a Gradle plugin that enables the generation of Java POJOs based on [PKL](https://pkl-lang.org) files. The plugin offers advanced features like generating mutable POJOs and adding Spring configuration properties annotations with custom prefix values.

## Features

- **POJO Generation:** Generate Java classes based on PKL definitions.
- **Mutable POJOs:** Option to generate mutable Java objects.
- **Spring Config Props Annotations:** Annotate generated classes with Spring configuration properties and custom prefixes.
- **Customizable Annotations:** Support for adding custom annotations like `@Named` and `@NonNull`.
- **Namespace Renaming:** Ability to rename namespaces during generation.

## Installation

Add the plugin to your `build.gradle` file:

```gradle
plugins {    
    id "io.hpkl"
}
```

## Configuration

You can configure the plugin in your `build.gradle` file using the `hpkl` block. Below is an example configuration:

```gradle
hpkl {
    javaCodeGenerators {
        configClasses {
            sourceModules = ["mod.pkl"]
            namedAnnotation = "javax.inject.Named"
            nonNullAnnotation = "javax.annotation.Nonnull"            
            settingsModule = "pkl:settings"
            renames = [
                'org': 'foo.bar'
            ]
        }
    }
}
```

### Configuration Options

- **`sourceModules`:** List of PKL files to generate Java code from.
- **`generateSetters`:** Generate setters in target class and make it mutable
- **`generateGetters`:** Generate getters in target class
- **`generateEmptyConstructor`:** Generate empty constructor
- **`generateJavadoc`:** Generate java doc
- **`durationClass`:** Default: java.time.Duration
- **`dataSizeClass`:** Default: org.pkl.core.DataSize
- **`durationUnitClass`:** Default: java.time.temporal.ChronoUnit
- **`dataSizeUnitClass`:** Default: org.pkl.core.DataSizeUnit
- **`pairClass`:** Default: org.pkl.core.Pair
- **`generateAnnotationClasses`:** Generate classes marked as annotations
- **`springConfigAnnotation`** Default: SpringConfigProperties
- **`namedAnnotation`:** Custom annotation for class naming (e.g., `javax.inject.Named`).
- **`nonNullAnnotation`:** Annotation to mark non-null fields (e.g., `javax.annotation.Nonnull`).
- **`settingsModule`:** Module to use for settings (e.g., `pkl:settings`).
- **`renames`:** Map for renaming namespaces (e.g., `org` to `foo.bar`).

## Example

Here is an example `build.gradle` configuration:

```gradle
plugins {
    id "java"
    id "io.hpkl"        
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "javax.inject:javax.inject:1"
    implementation "com.google.code.findbugs:jsr305:3.0.2"
}

hpkl {
    javaCodeGenerators {
        configClasses {
            sourceModules = ["mod.pkl"]
            namedAnnotation = "javax.inject.Named"
            nonNullAnnotation = "javax.annotation.Nonnull"
            settingsModule = "pkl:settings"
            renames = [
                'org': 'foo.bar'
            ]
        }
    }
}
```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request on the [GitHub repository](https://github.com/hpklio/hpkl-gradle).

## Support

For questions or support, please file an issue on the [GitHub repository](https://github.com/your-repo/hpkl-gradle).

---

Thank you for using `hpkl-gradle`!