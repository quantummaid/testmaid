/*
 * Copyright (c) 2021 Richard Hauswald - https://quantummaid.de/.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.quantummaid.testmaid.localfs

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedString
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator
import kotlin.io.path.*

/**
 * May need some more validation here, but security should not be such a big deal here, since the code will run in test
 * environments only.
 */
class LocalDirectory(unsafe: String) : ValidatedString(localPathValidator, unsafe) {
    fun file(name: String): LocalFile? {
        val path = Path(mappingValue(), name)

        return if (path.exists()) {
            LocalFile(path.absolutePathString())
        } else {
            null
        }
    }

    fun createFile(name: String, content: ByteArray? = null): LocalFile {
        val filePath = Path(mappingValue(), name)
        filePath.createFile()
        if (content != null) {
            filePath.writeBytes(content)
        }
        return LocalFile(filePath.absolutePathString())
    }

    companion object {
        private val localPathValidator = StringValidator.allOf(StringValidator.length(1, 256))

        fun targetDirectoryOfClass(clazz: Class<*>): LocalDirectory {
            val protectionDomain = clazz.protectionDomain
            val codeSource = protectionDomain.codeSource
            val location = codeSource.location
            val pathString = location.path
            val path = Path(pathString)
            val parent = path.parent ?: path
            if (parent.name == "target") {
                return LocalDirectory(parent.absolutePathString())
            } else {
                throw UnsupportedOperationException(
                    "Could not locate 'target' directory as direct parent of class '${clazz}'. " +
                            "The classes code source was reported to be ${location}"
                )
            }
        }
    }
}

