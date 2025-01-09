package io.hpkl.gradle.utils

import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileSystemLocation
import org.pkl.core.util.IoUtils
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

object PluginUtils {
    /**
     * Parses the specified source module notation into a "parsed" notation which is then used for
     * input path tracking and as an argument for the CLI API.
     *
     *
     * This method accepts the following input types:
     *
     *
     *  * [URI] - used as is.
     *  * [File] - used as is.
     *  * [Path] - converted to a [File]. This conversion may fail because not all
     * [Path]s point to the local file system.
     *  * [URL] - converted to a [URI]. This conversion may fail because [URL]
     * allows for URLs which are not compliant URIs.
     *  * [CharSequence] - first, converted to a string. If this string is "URI-like" (see
     * [IoUtils.isUriLike]), then we attempt to parse it as a [URI], which
     * may fail. Otherwise, we attempt to parse it as a [Path], which is then converted to
     * a [File] (both of these operations may fail).
     *  * [FileSystemLocation] - converted to a [File] via the [       ][FileSystemLocation.getAsFile] method.
     *
     *
     * In case the returned value is determined to be a [URI], then this URI is first checked
     * for whether its scheme is `file`, like `file:///example/path`. In such case, this
     * method returns a [File] corresponding to the file path in the URI. Otherwise, a [ ] instance is returned.
     *
     * @throws InvalidUserDataException In case the input is none of the types described above, or
     * when the underlying value cannot be parsed correctly.
     */
    fun parseModuleNotation(notation: Any): Any {
        return when (notation) {
            is URI -> if (notation.scheme == "file") File(notation.path) else notation
            is File -> notation
            is Path -> try {
                return notation.toFile()
            } catch (e: UnsupportedOperationException) {
                throw InvalidUserDataException("Failed to parse Pkl module file path: $notation", e)
            }

            is URL -> try {
                return parseModuleNotation(notation.toURI())
            } catch (e: URISyntaxException) {
                throw InvalidUserDataException("Failed to parse Pkl module URI: $notation", e)
            }

            is CharSequence -> notation.toString().let {
                if (IoUtils.isUriLike(it)) {
                    try {
                        parseModuleNotation(IoUtils.toUri(it))
                    } catch (e: URISyntaxException) {
                        throw InvalidUserDataException("Failed to parse Pkl module URI: $it", e)
                    }
                } else {
                    try {
                        Paths.get(it).toFile()
                    } catch (e: InvalidPathException) {
                        throw InvalidUserDataException("Failed to parse Pkl module file path: $it", e)
                    } catch (e: UnsupportedOperationException) {
                        throw InvalidUserDataException("Failed to parse Pkl module file path: $it", e)
                    }
                }
            }

            is FileSystemLocation -> notation.asFile
            else -> throw InvalidUserDataException(
                (
                    "Unsupported value of type " +
                        notation.javaClass +
                        " used as a module path: " +
                        notation
                    ),
            )
        }
    }

    /**
     * Converts either a file or a URI to a URI. We convert a relative file to a URI via the [ ][IoUtils.createUri] because other ways of conversion can make relative paths into
     * absolute URIs, which may break module loading.
     */
    fun parsedModuleNotationToUri(notation: Any): URI {
        if (notation is File) {
            if (notation.isAbsolute) {
                return notation.toPath().toUri()
            }
            return IoUtils.createUri(IoUtils.toNormalizedPathString(notation.toPath()))
        } else if (notation is URI) {
            return notation
        }
        throw IllegalArgumentException("Invalid parsed module notation: $notation")
    }

    fun parseModuleNotationToUri(m: Any): URI {
        return parsedModuleNotationToUri(parseModuleNotation(m))
    }
}
