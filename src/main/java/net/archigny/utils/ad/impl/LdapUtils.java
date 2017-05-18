/*
 * Copyright 2005-2013 the original author or authors.
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

package net.archigny.utils.ad.impl;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Generic utility methods for working with LDAP. Mainly for internal use within
 * the framework, but also useful for custom code. Taken from Spring LDAP package
 * 
 * @author Ulrik Sandberg
 * @author Mattias Hellborg Arthursson
 * @since 1.0.0
 */
public class LdapUtils {
    
    private static final int HEX = 16;

    /**
     * Converts a binary SID to its String representation, according to the
     * algorithm described <a
     * href="http://blogs.msdn.com/oldnewthing/archive/2004/03/15/89753.aspx"
     * >here</a>. Thanks to <a href=
     * "http://www.jroller.com/eyallupu/entry/java_jndi_how_to_convert">Eyal
     * Lupu</a> for algorithmic inspiration.
     * 
     * <pre>
     * If you have a SID like S-a-b-c-d-e-f-g-...
     * 
     * Then the bytes are
     * a    (revision)
     * N    (number of dashes minus two)
     * bbbbbb   (six bytes of &quot;b&quot; treated as a 48-bit number in big-endian format)
     * cccc (four bytes of &quot;c&quot; treated as a 32-bit number in little-endian format)
     * dddd (four bytes of &quot;d&quot; treated as a 32-bit number in little-endian format)
     * eeee (four bytes of &quot;e&quot; treated as a 32-bit number in little-endian format)
     * ffff (four bytes of &quot;f&quot; treated as a 32-bit number in little-endian format)
     * etc. 
     * 
     * So for example, if your SID is S-1-5-21-2127521184-1604012920-1887927527-72713, then your raw hex SID is
     * 
     * 010500000000000515000000A065CF7E784B9B5FE77C8770091C0100
     * 
     * This breaks down as follows:
     * 01   S-1
     * 05   (seven dashes, seven minus two = 5)
     * 000000000005 (5 = 0x000000000005, big-endian)
     * 15000000 (21 = 0x00000015, little-endian)
     * A065CF7E (2127521184 = 0x7ECF65A0, little-endian)
     * 784B9B5F (1604012920 = 0x5F9B4B78, little-endian)
     * E77C8770 (1887927527 = 0X70877CE7, little-endian)
     * 091C0100 (72713 = 0x00011c09, little-endian)
     * 
     * S-1- version number (SID_REVISION)
     * -5-  SECURITY_NT_AUTHORITY
     * -21- SECURITY_NT_NON_UNIQUE
     * -...-...-...-    these identify the machine that issued the SID
     * 72713    unique user id on the machine
     * </pre>
     * 
     * @param sid binary SID in byte array format
     * @return String version of the given sid
     * @since 1.3.1
     */
    public static String convertBinarySidToString(byte[] sid) {
        // Add the 'S' prefix
        StringBuffer sidAsString = new StringBuffer("S-");

        // bytes[0] : in the array is the version (must be 1 but might
        // change in the future)
        sidAsString.append(sid[0]).append('-');

        // bytes[2..7] : the Authority
        StringBuffer sb = new StringBuffer();
        for (int t = 2; t <= 7; t++) {
            String hexString = Integer.toHexString(sid[t] & 0xFF);
            sb.append(hexString);
        }
        sidAsString.append(Long.parseLong(sb.toString(), HEX));

        // bytes[1] : the sub authorities count
        int count = sid[1];

        // bytes[8..end] : the sub authorities (these are Integers - notice
        // the endian)
        for (int i = 0; i < count; i++) {
            int currSubAuthOffset = i * 4;
            sb.setLength(0);
            sb.append(toHexString((byte) (sid[11 + currSubAuthOffset] & 0xFF)));
            sb.append(toHexString((byte) (sid[10 + currSubAuthOffset] & 0xFF)));
            sb.append(toHexString((byte) (sid[9 + currSubAuthOffset] & 0xFF)));
            sb.append(toHexString((byte) (sid[8 + currSubAuthOffset] & 0xFF)));

            sidAsString.append('-').append(Long.parseLong(sb.toString(), HEX));
        }

        // That's it - we have the SID
        return sidAsString.toString();
    }
    
    /**
     * Converts a String SID to its binary representation, according to the
     * algorithm described <a
     * href="http://blogs.msdn.com/oldnewthing/archive/2004/03/15/89753.aspx"
     * >here</a>. 
     * 
     * @param string SID in readable format
     * @return Binary version of the given sid
     * @see LdapUtils#convertBinarySidToString(byte[])
     * @since 1.3.1
     */
    public static byte[] convertStringSidToBinary(String string) {
        String[] parts = string.split("-");
        byte sidRevision = (byte) Integer.parseInt(parts[1]);
        int subAuthCount = parts.length - 3;

        byte[] sid = new byte[] {sidRevision, (byte) subAuthCount};
        sid = addAll(sid, numberToBytes(parts[2], 6, true));
        for (int i = 0; i < subAuthCount; i++) {
            sid = addAll(sid, numberToBytes(parts[3 + i], 4, false));
        }
        return sid;
    }
    
    /**
     * Converts a byte into its hexadecimal representation, padding with a
     * leading zero to get an even number of characters.
     * 
     * @param b value to convert
     * @return hex string, possibly padded with a zero
     */
    static String toHexString(final byte b) {
        String hexString = Integer.toHexString(b & 0xFF);
        if (hexString.length() % 2 != 0) {
            // Pad with 0
            hexString = "0" + hexString;
        }
        return hexString;
    }

    /**
     * Converts the given number to a binary representation of the specified
     * length and "endian-ness".
     * 
     * @param number String with number to convert
     * @param length How long the resulting binary array should be
     * @param bigEndian <code>true</code> if big endian (5=0005), or
     * <code>false</code> if little endian (5=5000)
     * @return byte array containing the binary result in the given order
     */
    static byte[] numberToBytes(String number, int length, boolean bigEndian) {
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
