/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.uwb.support.base;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Flags backed by long value.
 * If all the enum values fit in a integer, you can use
 * {@link #toInt(Set)} and {@link #toEnumSet(int, Enum[])} safely. Otherwise, those methods will
 * throw an {@link ArithmeticException} on overflow.
 */
public interface FlagEnum {
    long getValue();

    static <E extends Enum<E> & FlagEnum> int toInt(Set<E> enumSet) {
        int value = 0;
        for (E flag : enumSet) {
            value |= Math.toIntExact(flag.getValue());
        }
        return value;
    }

    static <E extends Enum<E> & FlagEnum> EnumSet<E> toEnumSet(int flags, E[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Empty FlagEnum");
        }
        List<E> flagList = new ArrayList<>();
        for (E value : values) {
            if ((flags & Math.toIntExact(value.getValue())) != 0) {
                flagList.add(value);
            }
        }
        if (flagList.isEmpty()) {
            Class<E> c = values[0].getDeclaringClass();
            return EnumSet.noneOf(c);
        } else {
            return EnumSet.copyOf(flagList);
        }
    }

    static <E extends Enum<E> & FlagEnum> long toLong(Set<E> enumSet) {
        long value = 0;
        for (E flag : enumSet) {
            value |= flag.getValue();
        }
        return value;
    }

    static <E extends Enum<E> & FlagEnum> EnumSet<E> longToEnumSet(long flags, E[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Empty FlagEnum");
        }
        List<E> flagList = new ArrayList<>();
        for (E value : values) {
            if ((flags & value.getValue()) != 0) {
                flagList.add(value);
            }
        }
        if (flagList.isEmpty()) {
            Class<E> c = values[0].getDeclaringClass();
            return EnumSet.noneOf(c);
        } else {
            return EnumSet.copyOf(flagList);
        }
    }
}
