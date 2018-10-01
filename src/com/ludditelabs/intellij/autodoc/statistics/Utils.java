/*
 * Copyright 2018 Luddite Labs Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ludditelabs.intellij.autodoc.statistics;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Statistics utils.
 */
public class Utils {
    /** Convert bytes to string. */
    static private String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Get MAC address.
     *
     * This method searches for the first hardware network interface
     * which has MAC and returns it as a string.
     *
     * @return a string representation of MAC or {@code null}.
     */
    static private byte[] getMacAddress() throws SocketException {
        Enumeration<NetworkInterface> nets =
            NetworkInterface.getNetworkInterfaces();

        for (NetworkInterface iface : Collections.list(nets)) {
            byte[] mac = iface.getHardwareAddress();
            if (mac != null)
                return mac;
        }

        return null;
    }

    /**
     * Create plugin's runtime environment UUID.
     *
     * It uses SHA265 digest of the MAC addr.
     *
     * @return UUID or empty string.
     */
    static public String getUuid() {
        String uuid = "";

        try {
            byte[] bytes = getMacAddress();

            if (bytes != null) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(bytes);
                byte[] digest = md.digest();
                uuid = bytesToString(digest);
            }
        }
        catch (SocketException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return uuid;
    }
}
