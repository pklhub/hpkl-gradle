package io.hpkl.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class KotlinCodeGeneratorsTest : AbstractTest() {
    @Test
    fun `generate code`() {
        writeBuildFile()
        writePklFile()

        runTask("generatePklPojo")

        val baseDir = testProjectDir.resolve("build/generated/pkl/configClasses/kotlin/foo/bar")
        val moduleFile = baseDir.resolve("Mod.kt")

        assertThat(baseDir.listDirectoryEntries().count()).isEqualTo(1)
        assertThat(moduleFile).exists()

        val text = moduleFile.readText()

        // shading must not affect generated code
        assertThat(text).doesNotContain("org.pkl.thirdparty")

        checkTextContains(
            text,
            """
                |data class Mod(
                |  var other: Any? = 42
                |) {""",
        )

        checkTextContains(
            text,
            """
      |  data class Person(
      |    var name: String = "defaultName",
      |    var addresses: List<Address?> = listOf()
    """,
        )

        checkTextContains(
            text,
            """
      |  data class Address(
      |    var street: String?,
      |    var zip: Long?,
      |    var duration: Duration?,
      |    var region: String = "Unknown",
      |    var code: Long = 10,
      |    var enabled: Boolean = true,
      |    var width: Double = 1.0,
      |    var timeout: Duration = Duration.parseIsoString("PT1S"),
      |    var size: DataSize = org.springframework.util.unit.DataSize.ofBytes(1000),
      |    var intList: List<Long> = listOf(1L,2L,3L),
      |    var int8List: List<Byte> = listOf(1.toByte(),2.toByte(),3.toByte()),
      |    var int16List: List<Short> = listOf(1.toShort(),2.toShort(),3.toShort()),
      |    var int32List: List<Int> = listOf(1,2,3),
      |    var intListing: List<Long> = listOf(1L,2L,3L),
      |    var strList: List<String> = listOf("1","2","3"),
      |    var boolList: List<Boolean> = listOf(true,false,true),
      |    var floatList: List<Double> = listOf(1.0,2.0,3.0),
      |    var durationList: List<Duration> =
      |        listOf(Duration.parseIsoString("PT1S"),Duration.parseIsoString("PT2S"),Duration.parseIsoString("PT3S")),
      |    var dataSizeList: List<DataSize> =
      |        listOf(org.springframework.util.unit.DataSize.ofBytes(1000),org.springframework.util.unit.DataSize.ofBytes(2000),org.springframework.util.unit.DataSize.ofBytes(3000)),
      |    var codeObj: AddressPostCode = AddressPostCode(1, null),
      |    var codes: List<AddressPostCode> = listOf(AddressPostCode(1, null),AddressPostCode(2, "1")),
      |    var map: Map<String, Long> = mapOf("1" to 2L,"2" to 3L,"3" to 4L),
      |    var optional: OptionalClass = OptionalClass(1, null),
      |    var diet: Diet = foo.bar.Mod.Diet.BERRIES,
      |    var email: EmailAddress = "test@test.com"
    """,
        )
    }

    @Test
    fun `compile generated code`() {
        writeBuildFile()
        writePklFile()

        runTask("compileKotlin")

        val classesDir = testProjectDir.resolve("build/classes/kotlin/main")
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
        id 'org.jetbrains.kotlin.jvm' version '1.9.22'
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
        kotlinCodeGenerators {
          configClasses {
            sourceModules = ["mod.pkl"]
            durationClass = "kotlin.time.Duration"
            dataSizeClass = "org.springframework.util.unit.DataSize"
            dataSizeConverter = "org.springframework.util.unit.DataSize.ofBytes"
            mutableObjects = true
            settingsModule = "pkl:settings"
            setDefaultValues = true
            renames = [
              'org': 'foo.bar'
            ]
          }
        }
      }
      
      sourceSets {
        main {
            kotlin {
                srcDirs files(generatePklPojo.outputs.files)
            }
        }
      }
            
      compileKotlin.dependsOn(generatePklPojo)
    """,
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
