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

package my.company.mymono.testsupport

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.monolambda.awssdk.common.*
import de.quantummaid.monolambda.awssdk.dynamodb.DynamoDbClientFactory
import de.quantummaid.monolambda.cf.parts.dynamodb.DynamoDbTableName
import de.quantummaid.monolambda.model.cfg.EnvVariables
import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.junit5.TestMaidJunit5Adapter
import de.quantummaid.testmaid.withinTestCaseScope
import de.quantummaid.testmaid.withinTestSuiteScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.util.*

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(MyDomainEntityRepositoryTestExtension::class)
annotation class MyDomainSpecificEntityRepositoryTestAnnotation

private class MyDomainEntityRepositoryTestExtension : TestMaidJunit5Adapter(testMaid) {
    companion object {
        private val testMaid = testMaid()

        fun testMaid(): TestMaid {
            /*
                - Divide SUT and Inspector scopes
                - we need backdoors?
                - should we test in memory next to dynamoDB repo to discover bugs in the in mem layer?
                - Use the domain specific MonoLambdaFactory which contains a List<Entity>
                    => 1. generate separate CF Template containing only the DB tables
                       2. install
                       3. use outputs as config source
                -
             */
            val injectMaidBuilder = InjectMaid.anInjectMaid()
                    .withLifecycleManagement()
                    .closeOnJvmShutdown()
                    .withinTestSuiteScope {
                        withCustomType(EnvVariables::class.java, { EnvVariables.fromSystem() }, ReusePolicy.EAGER_SINGLETON)
                        withFactory(Region::class.java, AwsRegionFactory::class.java, ReusePolicy.EAGER_SINGLETON)
                        withFactory(
                                AwsCredentialsProvider::class.java,
                                AwsCredentialsProviderFactory::class.java,
                                ReusePolicy.EAGER_SINGLETON
                        )
                        withFactory(
                                ConfiguredSdkHttpClient::class.java, ConfiguredSdkHttpClientFactory::class.java,
                                ReusePolicy.EAGER_SINGLETON
                        )
                        withFactory(
                                AwsConfiguration::class.java, AwsConfigurationFactory::class.java,
                                ReusePolicy.EAGER_SINGLETON
                        )
                        withFactory(
                                DynamoDbClient::class.java,
                                DynamoDbClientFactory::class.java,
                                ReusePolicy.EAGER_SINGLETON
                        )
                    }
                    .withinTestCaseScope {
                        withType(AutoCleanupDynamoDbTable::class.java, ReusePolicy.DEFAULT_SINGLETON)
                    }
            return TestMaid.buildTestMaid(injectMaidBuilder)
        }
    }
}

class AutoCleanupDynamoDbTable(private val dynamoDbClient: DynamoDbClient) : AutoCloseable {
    val tableName = DynamoDbTableName("AutoCleanupDynamoDbTable" + UUID.randomUUID().toString())

    init {
        println("Creating table ${tableName}")
        dynamoDbClient.createTable {
            it.tableName(tableName.mappingValue())
            it.billingMode(BillingMode.PAY_PER_REQUEST)
            it.attributeDefinitions(
                    AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
            )
            it.keySchema(
                    KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()
            )
        }
        pollForTableExists(20)
        println("Created table ${tableName}")
    }

    private fun pollForTableExists(remainingAttempts: Int): Boolean {
        return if (remainingAttempts == 0) {
            false
        } else {
            if (tableExists()) {
                true
            } else {
                runBlocking { delay(500) }
                pollForTableExists(remainingAttempts - 1)
            }
        }
    }

    private fun tableExists(): Boolean {
        val tableNames = dynamoDbClient.listTablesPaginator().tableNames().toList()
        tableNames
                .filter { it.startsWith("AutoCleanupDynamoDbTable") }
                .filterNot { it == tableName.mappingValue() }
                .forEach { doDelete(DynamoDbTableName(it)) }
        val exists = tableNames.singleOrNull { it == tableName.mappingValue() } != null
        println("Table found: ${exists} in tables ${tableNames}")
        runBlocking { delay(5000) }
        return exists
    }

    override fun close() {
        println("Deleting table ${tableName}")
        delete()
        println("Deleted table ${tableName}")
    }

    private fun delete(remainingAttempts: Int = 20, retryInMs: Int = 500) {
        if (remainingAttempts > 0) {
            try {
                doDelete(tableName)
            } catch (e: Exception) {
                println("Table is still fiddled with by aws, will retry ${remainingAttempts - 1} more times...")
                runBlocking { delay(retryInMs.toLong()) }
                delete(remainingAttempts - 1, retryInMs)
            }
        } else {
            doDelete(tableName)
        }
    }

    private fun doDelete(toDelete: DynamoDbTableName) {
        dynamoDbClient.deleteTable { it.tableName(toDelete.mappingValue()) }
    }


}