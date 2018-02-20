/*
 * Copyright 2017 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.repository

import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.ports.LabelRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JdbcLabelRepositoryTest : TestBase() {

    private lateinit var repo: LabelRepository

    @BeforeEach
    fun setUp() {
        val config = TestJdbcConfig()
        config.reset()
        repo = JdbcLabelRepository(config)
    }

    @Test
    fun `ensure labels are retrieved`() {
        val labels = repo.getLabels()
        assertEquals(5, labels.size.toLong())
    }

    @Test
    fun `ensure labels can be retrieved by type`() {
        val labels = repo.getLabels(Label.Type.INBOX)
        assertEquals(1, labels.size.toLong())
        assertEquals("Inbox", labels[0].toString())
    }

}