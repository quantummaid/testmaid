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

package my.company.mymono.user.infra

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.injectmaid.api.customtype.api.CustomType.customType
import de.quantummaid.mapmaid.MapMaid
import de.quantummaid.monolambda.awssdk.dynamodb.DefaultDynamoDbTransaction
import de.quantummaid.monolambda.model.entities.DefaultEntityAdapter
import de.quantummaid.monolambda.model.entities.EntityAdapter
import de.quantummaid.monolambda.model.entities.EntityRepositoryDynamoDb
import de.quantummaid.monolambda.model.entities.EntityTableConfiguration
import de.quantummaid.reflectmaid.GenericType
import de.quantummaid.reflectmaid.ReflectMaid
import my.company.mymono.testsupport.AutoCleanupDynamoDbTable
import my.company.mymono.testsupport.MyDomainSpecificEntityRepositoryTestAnnotation
import my.company.mymono.user.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/*
GraalScope:
    - Reflection/reflectMaid
RuntimeScope
    - configuration

 */
@MyDomainSpecificEntityRepositoryTestAnnotation
internal class UserRepositoryDynamoDbTest {

    @Test
    fun testEntityAdapter(dynamoDbClient: DynamoDbClient, table: AutoCleanupDynamoDbTable) {
        val reflectMaid: ReflectMaid = ReflectMaid.aReflectMaid()
        val adapter: EntityAdapter<User> = DefaultEntityAdapter.buildFromConventionalEntity(
                User::class, reflectMaid, MapMaid.aMapMaid()
        )
        val id = UserId.newUnique()
        val mvccId = UserMvccId.initialMvccId()
        val entity = User(id, mvccId, Username("Rich"), UserNickname("Dic"))

        assertEquals(id.mappingValue(), adapter.id(entity))
        assertEquals(mvccId.mappingValue(), adapter.mvccId(entity))
        assertEquals("1", adapter.mvccId(adapter.incrementMvccId(entity)))
        assertEquals("2", adapter.mvccId(adapter.incrementMvccId(adapter.incrementMvccId(entity))))

        println(adapter.toDynamoDbItemMap(entity))

        val genericType = GenericType.genericType<EntityRepositoryDynamoDb<User>>(EntityRepositoryDynamoDb::class.java, User::class.java)
        val injectMaid = InjectMaid.anInjectMaid(reflectMaid)
                .withType(DefaultDynamoDbTransaction::class.java, ReusePolicy.DEFAULT_SINGLETON)
                .withCustomType(
                        customType(genericType)
                                .withDependency(DefaultDynamoDbTransaction::class.java)
                                .withDependency(GenericType.genericType<EntityTableConfiguration<User>>())
                                .usingFactory { tx, cfg -> EntityRepositoryDynamoDb(adapter, cfg, tx) },
                        ReusePolicy.PROTOTYPE
                )
                .build()
        val repositoryDynamoDb = injectMaid.getInstance(genericType)
        repositoryDynamoDb.create(entity)
        val transaction = injectMaid.getInstance(DefaultDynamoDbTransaction::class.java)
        transaction.commit()
        println(repositoryDynamoDb.findAll())
    }

    @Test
    fun testSmoke(dynamoDbClient: DynamoDbClient) {
        dynamoDbClient.listTablesPaginator().forEach { println(it.tableNames()) }

//        val dynamoDbTransaction = DynamoDbTransaction(EntityRepositoryDynamoDbTransaction(dynamoDbClient, object : RepositoryFactory {
//            override fun <T : Any> obtainRepositoryInstance(repositoryType: KClass<out T>, entityRepositoryDynamoDbTransaction: EntityRepositoryDynamoDbTransaction): T {
//                val mapMaid: MapMaid = MapMaid.aMapMaid().build()
//                return UserRepositoryDynamoDb(entityRepositoryDynamoDbTransaction, UserRepositoryDynamoDbTableName(""), mapMaid) as T
//            }
//
//        })
//        val userRepository = dynamoDbTransaction.userRepository()
//
//        val tableNames = dynamoDbClient.listTablesPaginator().tableNames()
//        println(tableNames.toList())
//        val tableExists = tableNames.contains("RichardTestUsersTable")
//        if (!tableExists) {
//            dynamoDbClient.createTable {
//                it
//                        .tableName("RichardTestUsersTable")
//                        .attributeDefinitions(AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build())
//                        .keySchema(KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build())
//                        .billingMode(BillingMode.PAY_PER_REQUEST)
//            }
//        }
//
//        deleteAll()
//        create()
//        update()
//        updateMvccIdCollision()
    }
//
//    fun deleteAll() {
//        val tx = DynamoDbTransaction(dynamoDbClient)
//        val allUsers = tx.userRepository().allUsers()
//        println("All users before deleting: $allUsers")
//        allUsers.forEach {
//            val tx = DynamoDbTransaction(dynamoDbClient)
//            tx.userRepository().delete(it)
//            tx.commit()
//        }
//        println("All users after deleting: ${tx.userRepository().allUsers()}")
//    }
//
//    fun create(): User {
//        val tx = DynamoDbTransaction(dynamoDbClient)
//
//        val userId = UserId.newUnique()
//        val user = User(userId, MvccId.initialMvccId(), Username("Richard"), UserNickname("Rich"))
//        tx.userRepository().create(user)
//        tx.commit()
//
//        println("All users after creating: ${tx.userRepository().allUsers()}")
//        return user
//    }
//
//    fun update() {
//        val tx = DynamoDbTransaction(dynamoDbClient)
//        val allUsers = tx.userRepository().allUsers()
//
//        val user = allUsers.first()
//        user.username = Username("richard.hauswald")
//        user.userNickname = UserNickname("Dick")
//
//        tx.userRepository().update(user)
//        tx.commit()
//
//        println("All users after update: ${tx.userRepository().allUsers()}")
//    }
//
//    fun updateMvccIdCollision() {
//        val tx1 = DynamoDbTransaction(dynamoDbClient)
//        val allUsers = tx1.userRepository().allUsers()
//
//        val user = allUsers.first()
//        user.username = Username("richard.hauswald")
//        user.userNickname = UserNickname("Dick")
//
//        tx1.userRepository().update(user)
//        tx1.commit()
//
//        val tx2 = DynamoDbTransaction(dynamoDbClient)
//        tx2.userRepository().update(user)
//        tx2.commit()
//    }
}
