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

package de.quantummaid.testmaid.integrations.aws.support

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.CloudFormationService
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.CloudFormationServiceIC
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackNameBuilder
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackPrefix
import de.quantummaid.testmaid.junit5.TestMaidJunit5Adapter
import de.quantummaid.testmaid.model.TimeoutSettings
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testcase.TestCaseScope
import de.quantummaid.testmaid.util.AutoCleanedCoroutineScope
import de.quantummaid.testmaid.util.AutoCleanupStackPrefix
import de.quantummaid.testmaid.withinTestCaseScope
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import kotlin.time.Duration.Companion.minutes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(AwsEndToEndTestSupport::class)
annotation class AwsEndToEndTest


private class AwsEndToEndTestSupport : TestMaidJunit5Adapter(testMaid) {
    companion object {
        private val testMaid = testMaid()

        fun testMaid(): TestMaid {
            val injectMaidBuilder = InjectMaid.anInjectMaid()
                    .withLifecycleManagement()
                    .closeOnJvmShutdown()
                    .withCustomType(
                            CloudFormationClient::class.java,
                            { CloudFormationClient.builder().build() },
                            ReusePolicy.DEFAULT_SINGLETON
                    )
                    .withCustomType(StackNameBuilder::class.java, {
                        val runtimeId = System.getenv("RuntimeId")!!
                        StackNameBuilder(StackPrefix("testmaid-aws"), StackPrefix(runtimeId))
                    }, ReusePolicy.DEFAULT_SINGLETON)
                    .withCustomType(LogToStdOut::class.java, {
                        val envVariableValue: String = System.getenv("LogToStdOut") ?: ""
                        val enabled = envVariableValue != "disabled"
                        if (!enabled) {
                            println("( •̀ᴗ•́ )و ̑̑ - Log to stdout disable as per your request")
                        }
                        LogToStdOut(enabled)
                    }, ReusePolicy.DEFAULT_SINGLETON)
                    .withConfiguration(CloudFormationServiceIC())
                    .withType(AutoCleanedCoroutineScope::class.java)
                    .withinTestCaseScope {
                        withCustomType(
                                AutoCleanupStackPrefix::class.java,
                                StackNameBuilder::class.java,
                                TestCaseData::class.java,
                                CloudFormationService::class.java,
                                { stackNameBuilder, testCaseData, cloudFormationService ->
                                    AutoCleanupStackPrefix(stackNameBuilder, testCaseData, cloudFormationService)
                                }, ReusePolicy.PROTOTYPE
                        )
                        withCustomType(
                                AutoCleanupStackName::class.java,
                                TestCaseScope::class.java, StackNameBuilder::class.java, CloudFormationClient::class.java,
                                { scope, stackNameBuilder, client ->
                                    val stackName = stackNameBuilder.uniqueForTestCase(scope.testCaseData.name)
                                    AutoCleanupStackName(stackName, client)
                                }, ReusePolicy.PROTOTYPE
                        )
                    }
            return TestMaid.buildTestMaid(
                    injectMaidBuilder,
                    timeoutSettings = TimeoutSettings(
                            createTestParameterTimeout = minutes(5),
                            cleanupTestParametersTimeout = minutes(5),
                    )
            )
        }
    }
}
