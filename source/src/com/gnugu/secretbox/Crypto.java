/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */
package com.gnugu.secretbox;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.text.TextUtils;
import android.util.Log;

/**
 * Provides cryptography services for the application.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class Crypto {
	private SecretKeySpec _secretKey;
	private Cipher _cipher;

	/**
	 * The length of salt in bytes.
	 */
	public static final int SALT_LENGTH = 16;

	private static final String KEY_FACTORY_ALGORITHM = "PBEWITHSHAAND128BITAES-CBC-BC";
	private static final String KEY_ALGORITHM = "AES";
	private static final String CIPHER_PROVIDER = "AES";

	/**
	 * Instantiates an object of this class.
	 * 
	 * @param password
	 *            Encryption password.
	 * @param salt
	 *            Encryption salt.
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeySpecException
	 */
	public Crypto(String password, byte[] salt) {

		if (TextUtils.isEmpty(password)) {
			throw new IllegalArgumentException(
					"password cannot be null or empty.");
		}

		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 20,
				128);

		SecretKeyFactory factory;
		try {
			factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			PBEKey key = (PBEKey) factory.generateSecret(keySpec);

			this.createKeyAndCipher(key.getEncoded());
		} catch (Exception e) {
			Log.e(this.getClass().getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Instantiates an object of this class.
	 * 
	 * @param key
	 *            The key data
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeySpecException
	 */
	public Crypto(byte[] key) {
		try {
			this.createKeyAndCipher(key);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}

	private void createKeyAndCipher(byte[] keyData)
			throws NoSuchAlgorithmException, NoSuchPaddingException {
		_secretKey = new SecretKeySpec(keyData, KEY_ALGORITHM);
		_cipher = Cipher.getInstance(CIPHER_PROVIDER);
	}

	/**
	 * Encrypts original string.
	 * 
	 * @param data
	 *            Original string to encrypt.
	 * @return Encrypted data.
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] encrypt(String data) {
		return this.encrypt(data.getBytes());
	}

	/**
	 * Encrypts original string.
	 * 
	 * @param data
	 *            Original data to encrypt.
	 * @return Encrypted data.
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] encrypt(byte[] data) {
		try {
			_cipher.init(Cipher.ENCRYPT_MODE, _secretKey);
			return _cipher.doFinal(data);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decrypts encrypted data.
	 * 
	 * @param data
	 *            Data to decrypt.
	 * @return Original string.
	 * @throws BadPaddingException 
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public String decryptToString(byte[] data) throws BadPaddingException {
		return new String(this.decrypt(data));
	}

	/**
	 * Decrypts encrypted data.
	 * 
	 * @param data
	 *            Data to decrypt.
	 * @return Original data.
	 * @throws BadPaddingException 
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] decrypt(byte[] data) throws BadPaddingException {
		try {
			_cipher.init(Cipher.DECRYPT_MODE, _secretKey);
			return _cipher.doFinal(data);
		} catch (BadPaddingException b) {
			throw b;
		} catch (Exception e) {
			Log.e(this.getClass().getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates random salt.
	 * 
	 * @return Salt as byte array.
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] generateSalt() {
		SecureRandom rand;
		try {
			rand = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[SALT_LENGTH];
			rand.nextBytes(salt);
			return salt;

		} catch (NoSuchAlgorithmException e) {
			// can't do much here
			Log.e(Crypto.class.getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates random encryption key.
	 * 
	 * @return Key data.
	 */
	public static byte[] generateKey() {
		KeyGenerator keyGen;
		try {
			keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
			keyGen.init(128);

			// generate the secret key specs
			SecretKey key = keyGen.generateKey();
			SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(),
					KEY_ALGORITHM);
			return keySpec.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			Log.e(Crypto.class.getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}
}
