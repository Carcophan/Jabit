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

package ch.dissem.bitmessage.entity.valueobject

import java.io.Serializable
import java.util.*

data class Label(
    private val label: String,
    val type: Label.Type?,
    /**
     * RGBA representation for the color.
     */
    var color: Int
) : Serializable {

    var id: Any? = null

    override fun toString(): String {
        return label
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Label) return false
        return label == other.label
    }

    override fun hashCode(): Int {
        return Objects.hash(label)
    }

    enum class Type {
        INBOX,
        BROADCAST,
        DRAFT,
        OUTBOX,
        SENT,
        UNREAD,
        TRASH
    }
}
