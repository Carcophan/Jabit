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

package ch.dissem.bitmessage.utils

/**
 * A simple utility class that simplifies using the second based time used in Bitmessage.
 */
object UnixTime {
    /**
     * Length of a minute in seconds, intended for use with [.now].
     */
    const val MINUTE = 60L
    /**
     * Length of an hour in seconds, intended for use with [.now].
     */
    const val HOUR = 60L * MINUTE
    /**
     * Length of a day in seconds, intended for use with [.now].
     */
    const val DAY = 24L * HOUR

    /**
     * @return the time in second based Unix time ([System.currentTimeMillis]/1000)
     */
    @JvmStatic val now: Long
        @JvmName("now") get() {
            return System.currentTimeMillis() / 1000L
        }
}