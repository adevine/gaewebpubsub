/*
   Copyright 2012 Alexander Devine

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.gaewebpubsub.util;

import java.security.MessageDigest;

/**
 * Helper class for generating SHA-256 digest hashes of input text.
 */
public class SecureHash {

    private static final char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    public static String hash(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(input.getBytes("UTF-8"));
            byte[] hashBytes = messageDigest.digest();
            return convertBytesToHexString(hashBytes);
        } catch (Exception e) {
            //shouldn't ever happen, UTF-8 and SHA-256 are required to be supported
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public static String convertBytesToHexString(byte[] bytes) {
        char[] retVal = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int theByte = bytes[i] & 0xFF;
            retVal[i * 2] = hexDigits[theByte >>> 4]; //high bits
            retVal[i * 2 + 1] = hexDigits[theByte & 0x0F]; //low bits
        }
        return new String(retVal);
    }
}
