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

import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.InjectorConfiguration
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.testmaid.integrations.aws.cf.plain.impl.*
import de.quantummaid.testmaid.util.TimeoutPoller

class CloudFormationServiceIC(
    private val logFacade: CloudFormationServiceLogFacade = StubLogFacade(true),
    private val timeoutPoller: TimeoutPoller = TimeoutPoller()
) : InjectorConfiguration {

    override fun apply(builder: InjectMaidBuilder) {
        builder.withImplementation(
            CloudFormationService::class.java, CloudFormationServiceImpl::class.java, ReusePolicy.DEFAULT_SINGLETON
        )
        builder.withInstance(this.timeoutPoller)
        builder.withCustomType(
            CloudFormationServiceLogFacade::class.java,
            { this.logFacade },
            ReusePolicy.DEFAULT_SINGLETON
        )

        builder.withType(DescribeStackOperation::class.java, ReusePolicy.DEFAULT_SINGLETON)
        builder.withType(StackProgressOperation::class.java, ReusePolicy.DEFAULT_SINGLETON)
        builder.withType(ListStacksWithPrefixOperation::class.java, ReusePolicy.DEFAULT_SINGLETON)
        builder.withType(CreateStackOperation::class.java, ReusePolicy.DEFAULT_SINGLETON)
        builder.withType(UpdateStackOperation::class.java, ReusePolicy.DEFAULT_SINGLETON)
        builder.withType(DeleteStackOperation::class.java, ReusePolicy.DEFAULT_SINGLETON)
    }
}
