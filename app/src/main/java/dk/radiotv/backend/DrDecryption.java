/*
 * Copyright 2019 Martin Kamp Jensen
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

package dk.radiotv.backend;

import java.math.BigInteger;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Kilde: https://github.com/mkjensen/tv/blob/master/app/src/main/java/com/github/mkjensen/tv/util/DrDecryption.java
 */
public class DrDecryption {

  private static Cipher CIPHER;
  private static MessageDigest MESSAGE_DIGEST;

  public static String decrypt(String input) {
    if (input == null) {
      return null;
    }

    try {
      if (CIPHER==null) {
        CIPHER = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256");
      }

      String initializationVectorBeginIndexHex = input.substring(2, 10);
      int initializationVectorBeginIndex = Integer.parseInt(initializationVectorBeginIndexHex, 16);
      String initializationVectorHex = input.substring(10 + initializationVectorBeginIndex);
      String encryptedHex = input.substring(10, initializationVectorBeginIndex + 10);

      byte[] key = MESSAGE_DIGEST.digest((initializationVectorHex + ":sRBzYNXBzkKgnjj8pGtkACch").getBytes());
      byte[] initializationVector = convertHexStringToByteArray(initializationVectorHex);
      byte[] encrypted = convertHexStringToByteArray(encryptedHex);

      CIPHER.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(initializationVector));

      byte[] decrypted = CIPHER.doFinal(encrypted);

      return new String(decrypted);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private static byte[] convertHexStringToByteArray(String input) {

    byte[] bytes = new BigInteger(input, 16).toByteArray();

    if (bytes[0] == 0) {
      byte[] bytesWithoutSignBit = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, bytesWithoutSignBit, 0, bytesWithoutSignBit.length);
      return bytesWithoutSignBit;
    }

    return bytes;
  }

}
