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

package com.google.uwb.support.fira;

import com.google.uwb.support.base.ProtocolVersion;

import java.nio.ByteBuffer;

/** Provides parameter versioning for Fira. */
public class FiraProtocolVersion extends ProtocolVersion {
    private static final int FIRA_PACKED_BYTE_COUNT = 2;

    public FiraProtocolVersion(int major, int minor) {
        super(major, minor);
    }

    public static FiraProtocolVersion fromString(String protocol) {
        String[] parts = protocol.split("\\.", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid protocol version: " + protocol);
        }

        return new FiraProtocolVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public byte[] toBytes() {
        return ByteBuffer.allocate(bytesUsed())
                .put((byte) getMajor())
                .put((byte) getMinor())
                .array();
    }

    public static FiraProtocolVersion fromBytes(byte[] data, int startIndex) {
        int major = data[startIndex] & 0xFF;
        int minor = data[startIndex + 1] & 0xFF;
        return new FiraProtocolVersion(major, minor);
    }

    /**
     * Construct the FiraProtocolVersion from a 2-octet value (received as a Little-Endian short),
     * that contains the major, minor and maintenance version numbers.
     * - Octet[0], Bits 0-7: Major version.
     * - Octet[1], Bits 4-7: Minor version.
     * - Octet[1], Bits 0-3: Maintenance version.
     */
    public static FiraProtocolVersion fromLEShort(short version) {
        int major = version & 0xFF;
        int minor = (version >> 12) & 0x0F;
        // We don't currently store or use the maintenance version number.
        return new FiraProtocolVersion(major, minor);
    }

    public static int bytesUsed() {
        return FIRA_PACKED_BYTE_COUNT;
    }
}
