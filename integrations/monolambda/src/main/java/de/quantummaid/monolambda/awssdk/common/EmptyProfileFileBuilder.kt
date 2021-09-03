package de.quantummaid.monolambda.awssdk.common

import software.amazon.awssdk.profiles.ProfileFile
import java.io.ByteArrayInputStream

object EmptyProfileFileBuilder {
    fun buildEmptyProfile(): ProfileFile {
        val byteArray = ByteArray(0)
        return ProfileFile.aggregator()
            .addFile(
                ProfileFile.builder()
                    .type(ProfileFile.Type.CONFIGURATION)
                    .content(ByteArrayInputStream(byteArray))
                    .build()
            )
            .addFile(
                ProfileFile.builder()
                    .type(ProfileFile.Type.CREDENTIALS)
                    .content(ByteArrayInputStream(byteArray))
                    .build()
            )
            .build()
    }
}
