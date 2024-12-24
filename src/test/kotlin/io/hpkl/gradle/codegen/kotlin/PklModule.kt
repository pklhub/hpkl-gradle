package io.hpkl.gradle.codegen.kotlin

import java.nio.file.Path
import org.pkl.commons.createParentDirectories
import org.pkl.commons.writeString

data class PklModule(val name: String, val content: String) {
    fun writeToDisk(path: Path): Path {
        return path.createParentDirectories().writeString(content)
    }
}