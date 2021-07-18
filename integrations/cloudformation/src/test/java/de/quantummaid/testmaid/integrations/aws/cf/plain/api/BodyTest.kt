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

package de.quantummaid.testmaid.integrations.aws.cf.plain.api

import de.quantummaid.testmaid.integrations.aws.cf.plain.TemplateFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class BodyTest {
    @Test
    internal fun canBeCreated() {
        assertEquals(Body(TemplateFixtures.EIP_V1), Body(TemplateFixtures.EIP_V1))
        assertNotEquals(Body(TemplateFixtures.EIP_V1), Body(TemplateFixtures.EIP_V2))
    }

    @Test
    internal fun md5Sum() {
        assertEquals(Body(TemplateFixtures.EIP_V1).md5Sum(), Body(TemplateFixtures.EIP_V1).md5Sum())
        assertNotEquals(Body(TemplateFixtures.EIP_V1).md5Sum(), Body(TemplateFixtures.EIP_V2).md5Sum())
    }
}
