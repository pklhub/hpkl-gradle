package io.hpkl.gradle

import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JavaCodeGeneratorsTest : AbstractTest() {
    @Test
    fun `generate code`() {
        writeBuildFile()
        writePklFile()

        runTask("generatePklPojo")

        val baseDir = testProjectDir.resolve("build/generated/pkl/configclasses/java/foo/bar")
        val moduleFile = baseDir.resolve("Mod.java")

        assertThat(baseDir.listDirectoryEntries().count()).isEqualTo(1)
        assertThat(moduleFile).exists()

        val text = moduleFile.readText()

        // shading must not affect generated code
        assertThat(text).doesNotContain("org.pkl.thirdparty")

        checkTextContains(
            text,
            """
      |public final class Mod {
      |  public final @Nonnull Object other;
    """
        )

        checkTextContains(
            text,
            """
      |  public static final class Person {
      |    public final @Nonnull String name;
      |
      |    public final @Nonnull List<Address> addresses;
    """
        )

        checkTextContains(
            text,
            """
      |  public static final class Address {
      |    public final @Nonnull String street;
      |
      |    public final long zip;
    """
        )
    }

    @Test
    fun `compile generated code`() {
        writeBuildFile()
        writePklFile()

        runTask("compileJava")

        val classesDir = testProjectDir.resolve("build/classes/java/main")
        val moduleClassFile = classesDir.resolve("foo/bar/Mod.class")
        val personClassFile = classesDir.resolve("foo/bar/Mod\$Person.class")
        val addressClassFile = classesDir.resolve("foo/bar/Mod\$Address.class")
        assertThat(moduleClassFile).exists()
        assertThat(personClassFile).exists()
        assertThat(addressClassFile).exists()
    }

    private fun writeBuildFile() {
        writeFile(
            "build.gradle",
            """
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
    """
        )
    }

    private fun writePklFile() {
        writeFile(
            "mod.pkl",
            """
        module org.mod
  
        class SpringConfigProperties extends Annotation { prefix: String }
        class Person {
          name: String
          addresses: List<Address?>
        }
  
        class Address {
          street: String
          zip: Int
          duration: Duration?
        }
  
        other = 42
      """
        )
    }
}