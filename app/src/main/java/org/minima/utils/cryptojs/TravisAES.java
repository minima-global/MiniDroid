package org.minima.utils.cryptojs;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.minima.objects.base.MiniData;
import org.minima.utils.MinimaLogger;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class TravisAES {

    public enum DataTypeEnum {
        HEX,
        BASE64
    }

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int IV_SIZE = 128;
    private static final int IV_LENGTH = IV_SIZE / 4;

    private int keySize = 256;
    private int iterationCount = 1989;
    private DataTypeEnum dataType = DataTypeEnum.BASE64;
    private Cipher cipher;
    private int saltLength;

    public TravisAES() {
        try {
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            saltLength = this.keySize / 4;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println(e);
        }
    }

    public TravisAES(int keySize, int iterationCount) {
        this.keySize = keySize;
        this.iterationCount = iterationCount;
        try {
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            saltLength = this.keySize / 4;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println(e);
        }
    }

    public String encrypt(String salt, String iv, String passPhrase, String plainText) {
        try {
            SecretKey key = generateKey(salt, passPhrase);
            byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, key, iv, plainText.getBytes(StandardCharsets.UTF_8));
            String cipherText;
            if (dataType.equals(DataTypeEnum.HEX)) {
                cipherText = toHex(encrypted);
            } else {
                cipherText = toBase64(encrypted);
            }
            return cipherText;
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    public String encrypt(String passphrase, String plainText) {
        try {
            String salt = toHex(generateRandom(keySize / 8));
            System.out.println("salt");
            
            String iv = toHex(generateRandom(IV_SIZE / 8));
            String cipherText = encrypt(salt, iv, passphrase, plainText);
            return salt + iv + cipherText;
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    public String decrypt(String salt, String iv, String passPhrase, String cipherText) {
        try {
            SecretKey key = generateKey(salt, passPhrase);
            byte[] encrypted;
            if (dataType.equals(DataTypeEnum.HEX)) {
                encrypted = fromHex(cipherText);
            } else {
                encrypted = fromBase64(cipherText);
            }
            byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv, encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    public String decrypt(String passPhrase, String cipherText) {
        try {
            String salt = cipherText.substring(0, saltLength);
            String iv = cipherText.substring(saltLength, saltLength + IV_LENGTH);
            String ct = cipherText.substring(saltLength + IV_LENGTH);
            return decrypt(salt, iv, passPhrase, ct);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    private static byte[] generateRandom(int length) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    private byte[] doFinal(int encryptMode, SecretKey key, String iv, byte[] bytes) {
        try {
            cipher.init(encryptMode, key, new IvParameterSpec(fromHex(iv)));
            return cipher.doFinal(bytes);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                | BadPaddingException e) {
            System.err.println(e);
            return null;
        }
    }

    private SecretKey generateKey(String salt, String passphrase) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), fromHex(salt), iterationCount, keySize);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    private static byte[] fromBase64(String str) {
        return Base64.getDecoder().decode(str);
    }

    private static String toBase64(byte[] ba) {
        return Base64.getEncoder().encodeToString(ba);
    }

    private static byte[] fromHex(String str) {
    	MiniData hex = new MiniData(str);
        return hex.getData();
    }

    private static String toHex(byte[] ba) {
    	MiniData hex = new MiniData(ba);
    	return hex.toPureString();
    }

    public DataTypeEnum getDataType() {
        return dataType;
    }

    public void setDataType(DataTypeEnum dataType) {
        this.dataType = dataType;
    }

    
    public static void main(String[] zArgs) {
    	
    	TravisAES travisAes = new TravisAES();
    	String encrypted = travisAes.encrypt("secret", "This is plain text.");
    	String decrypted = travisAes.decrypt("secret", encrypted);

        MinimaLogger.log("encrypted: " + encrypted);
        MinimaLogger.log("decrypted: " + decrypted);

    	String jsenc = "2b7542b48614c64c9a011c5a6479c8a30c1db5e3b941024a631d9f4f57013eff3d56b653238f73a68aa3352841a2fb22SqeRTW3YAafTpOm/wcGPYrcjybszhGi9wXLbswWeuCg=";
    	String jsdecrypted = travisAes.decrypt("secret", jsenc);
    	
    	MinimaLogger.log("jsencrypted: " + jsenc);
        MinimaLogger.log("decrypted: " + jsdecrypted);

    }
}

