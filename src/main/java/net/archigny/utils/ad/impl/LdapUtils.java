/* Copyright 2005-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. */

package net.archigny.utils.ad.impl;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Generic utility methods for working with LDAP. Mainly for internal use within the framework, but also useful for custom code.
 * Taken from Spring LDAP package
 * 
 * @author Ulrik Sandberg
 * @author Mattias Hellborg Arthursson
 * @since 1.0.0
 */
public class LdapUtils {

    /**
     * Converts a byte into its hexadecimal representation, padding with a leading zero to get an even number of characters.
     * 
     * @param b
     *            value to convert
     * @return hex string, possibly padded with a zero
     */
    public static String toHexString(final byte b) {

        String hexString = Integer.toHexString(b & 0xFF);
        if (hexString.length() % 2 != 0) {
            // Pad with 0
            hexString = "0" + hexString;
        }
        return hexString;
    }

    /**
     * Converts a byte array into its hexadecimal representation
     * 
     * @param bytes
     *            Byte array to convert
     * @return hexadecimal representation as a String
     */
    public static final String toHexString(final byte[] bytes) {

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts the given number to a binary representation of the specified length and "endian-ness".
     * 
     * @param number
     *            String with number to convert
     * @param length
     *            How long the resulting binary array should be
     * @param bigEndian
     *            <code>true</code> if big endian (5=0005), or <code>false</code> if little endian (5=5000)
     * @return byte array containing the binary result in the given order
     */
    public static byte[] numberToBytes(String number, int length, boolean bigEndian) {

        BigInteger bi = new BigInteger(number);
        byte[] bytes = bi.toByteArray();
        int remaining = length - bytes.length;
        if (remaining < 0) {
            bytes = Arrays.copyOfRange(bytes, -remaining, bytes.length);
        } else {
            byte[] fill = new byte[remaining];
            bytes = addAll(fill, bytes);
        }
        if (!bigEndian) {
            reverse(bytes);
        }
        return bytes;
    }

    private static byte[] addAll(byte[] array1, byte[] array2) {

        byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    private static void reverse(byte[] array) {

        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

}
