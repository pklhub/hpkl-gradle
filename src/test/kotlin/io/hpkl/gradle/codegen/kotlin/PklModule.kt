package io.hpkl.gradle.codegen.kotlin

import org.pkl.commons.createParentDirectories
import org.pkl.commons.writeString
import java.nio.file.Path

data class PklModule(val name: String, val content: String) {
    fun writeToDisk(path: Path): Path {
        return path.createParentDirectories().writeString(content)
    }
}
