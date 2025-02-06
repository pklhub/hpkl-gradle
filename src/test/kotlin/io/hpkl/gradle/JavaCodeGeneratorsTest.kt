package io.hpkl.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class JavaCodeGeneratorsTest : AbstractTest() {
    @Test
    fun `generate code`() {
        writeBuildFile()
        writeProjectFile()
        writePklFile()

        runTask("generatePklPojo")

        val baseDir = testProjectDir.resolve("build/generated/pkl/configClasses/java/foo/bar")
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
      |  public @Nonnull Object other = 42;
    """,
        )

        checkTextContains(
            text,
            """
      |  public static final class Person {
      |    public @Nonnull String name = "defaultName";
      |
      |    public @Nonnull List<Address> addresses = List.of();
    """,
        )

        checkTextContains(
            text,
            """
      |  public static final class Address {
      |    public String street;
      |
      |    public Long zip;
      |
      |    public Duration duration;
      |
      |    public @Nonnull String region = "Unknown";
      |
      |    public long code = 10;
      |
      |    public boolean enabled = true;
      |
      |    public double width = 1.0;
      |
      |    public @Nonnull Duration timeout = Duration.parse("PT1S");
      |
      |    public @Nonnull DataSize size = org.springframework.util.unit.DataSize.ofBytes(1000);
    """,
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
        implementation "org.pkl-lang:pkl-core:0.27.1"
        implementation "org.springframework:spring-core:6.1.8"        
      }

      hpkl {
        javaCodeGenerators {
          configClasses {
            projectDir = project.projectDir
            sourceModules = ["mod.pkl"]
            durationClass = "java.time.Duration"
            dataSizeClass = "org.springframework.util.unit.DataSize"
            dataSizeConverter = "org.springframework.util.unit.DataSize.ofBytes"
            namedAnnotation = "javax.inject.Named"
            nonNullAnnotation = "javax.annotation.Nonnull"
            settingsModule = "pkl:settings"
            generateSetters = true
            setDefaultValues = true
            renames = [
              'org': 'foo.bar'
            ]
          }
        }
      }      
    """,
        )
    }

    private fun writeProjectFile() {
        writeFile(
            "PklProject",
            """
                amends "pkl:Project"
                
                
                package {
                  name = "Test"
                  baseUri = "package://packages.pklhub.io/\(name)"
                  version =  "0.0.1"
                  packageZipUrl = "https://repo.pklhub.ai/\(name)/-/archive/\(version)/\(name)-\(version).zip"
                }
                
                dependencies {
                    ["k8s"] { uri = "package://pkg.pkl-lang.org/pkl-k8s/k8s@1.1.1" }
                }                
            """.trimIndent(),
        )
    }

    private fun writePklFile() {
        writeFile(
            "mod.pkl",
            """
        module org.mod
  
        class SpringConfigProperties extends Annotation { prefix: String }
        typealias EmailAddress = String(matches(Regex(#".+@.+"#)))
        class Person {
          name: String = "defaultName"
          addresses: List<Address?>
        }
  
        class Address {
          street: String?
          zip: Int?
          duration: Duration?
          region: String = "Unknown"
          code: Int = 10
          enabled: Boolean = true
          width: Float = 1.0
          timeout: Duration = 1.s
          size: DataSize = 1.kb
          intList: List<Int> = List(1, 2, 3)
          int8List: List<Int8> = List(1, 2, 3)
          int16List: List<Int16> = List(1, 2, 3)
          int32List: List<Int32> = List(1, 2, 3)
          intListing: Listing<Int> = (Listing) { 1; 2; 3 }
          strList: List<String> = List("1", "2", "3")
          boolList: List<Boolean> = List(true, false, true)
          floatList: List<Float> = List(1.0, 2.0, 3.0)
          durationList: List<Duration> = List(1.s, 2.s, 3.s)
          dataSizeList: List<DataSize> = List(1.kb, 2.kb, 3.kb)
          codeObj : AddressPostCode = new AddressPostCode { code = 1 }
          codes: List<AddressPostCode> = List(new AddressPostCode { code = 1 }, new AddressPostCode { code = 2; strCode = "1"})
          map: Map<String, Int> = Map("1", 2, "2", 3, "3", 4)
          optional: OptionalClass = new OptionalClass {}
          diet: Diet = "Berries"
          email: EmailAddress = "test@test.com"
        }
        
        typealias Diet = "Seeds"|"Berries"|"Insects"
        
        class OptionalClass {
            a: Int = 1
            b: String?
        }
        
        class AddressPostCode {
           code: Int
           strCode: String? 
        }
        
        
  
        other = 42
      """,
        )
    }
}
