package com.caipiao.live.order.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Encoder;

public class RSAUtil {

	/**
	 * 加载公钥
	 *
	 * @param in
	 * @throws Exception
	 */
	public void loadPublicKey(InputStream in) throws Exception {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String readLine = null;
			StringBuilder sb = new StringBuilder();
			while ((readLine = br.readLine()) != null) {
				if (readLine.charAt(0) == '-') {
					continue;
				} else {
					sb.append(readLine);
					sb.append('\r');
				}
			}
			loadPublicKey(sb.toString());
		} catch (IOException e) {
			throw new Exception("公钥数据流读取错误");
		} catch (NullPointerException e) {
			throw new Exception("公钥输入流为空");
		}
	}

	/**
	 * 从字符串中加载公钥
	 *
	 * @param publicKeyStr 公钥数据字符串
	 * @throws Exception 加载公钥时产生的异常
	 */
	public static RSAPublicKey loadPublicKey(String publicKeyStr) throws Exception {
		try {
			byte[] buffer = Base64.getDecoder().decode(publicKeyStr);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
			return (RSAPublicKey) keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此算法");
		} catch (InvalidKeySpecException e) {
			throw new Exception("公钥非法");
		} catch (NullPointerException e) {
			throw new Exception("公钥数据为空");
		}
	}

	/**
	 * 从文件中加载私钥
	 *
	 * @return 是否成功
	 * @throws Exception
	 */
	public void loadPrivateKey(InputStream in) throws Exception {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String readLine = null;
			StringBuilder sb = new StringBuilder();
			while ((readLine = br.readLine()) != null) {
				if (readLine.charAt(0) == '-') {
					continue;
				} else {
					sb.append(readLine);
					sb.append('\r');
				}
			}
			loadPrivateKey(sb.toString());
		} catch (IOException e) {
			throw new Exception("私钥数据读取错误");
		} catch (NullPointerException e) {
			throw new Exception("私钥输入流为空");
		}
	}

	public static RSAPrivateKey loadPrivateKey(String privateKeyStr) throws Exception {
		try {
			byte[] buffer = Base64.getDecoder().decode(privateKeyStr);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此算法");
		} catch (InvalidKeySpecException e) {
			throw new Exception("私钥非法");
		} catch (NullPointerException e) {
			throw new Exception("私钥数据为空");
		}
	}

	/**
	 * 签名
	 *
	 * @param privateKey 私钥
	 * @return
	 * @throws SignatureException
	 */
	public static String rsaSign(String privateKey, String signStr) throws SignatureException {
		try {
			RSAPrivateKey rsaPrivateKey = loadPrivateKey(privateKey);
			if (privateKey == null) {
				throw new Exception("加密公钥为空, 请设置");
			}
			Signature signature = Signature.getInstance("SHA1WithRSA");
			signature.initSign(rsaPrivateKey);
			signature.update(signStr.getBytes());

			byte[] signed = signature.sign();
			Encoder encoder = Base64.getEncoder();
			String output = encoder.encodeToString(signed);
			return output;
		} catch (Exception e) {
			throw new SignatureException("RSAcontent = " + signStr);
		}
	}

	/**
	 * 验签
	 */
	public static boolean rsaVerify(String publicKey, String sign, String signStr) {
		try {
			RSAPublicKey rsaPublicKey = loadPublicKey(publicKey);
			if (publicKey == null) {
				throw new Exception("解密私钥为空, 请设置");
			}
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify(rsaPublicKey);
			signature.update(signStr.getBytes());

			// 把签名反解析，并验证
			byte[] decodeSign = Base64.getDecoder().decode(sign);
			return signature.verify(decodeSign);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 加密过程
	 *
	 * @param privateKey 公钥
	 * @param encryptStr 明文数据
	 * @return
	 * @throws Exception 加密过程中的异常信息
	 */
	public static String encrypt(String privateKey, String encryptStr) throws Exception {
		byte[] plainTextData = encryptStr.getBytes();
		RSAPrivateKey rsaPrivateKey = loadPrivateKey(privateKey);

		if (privateKey == null) {
			throw new Exception("加密公钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, rsaPrivateKey);
			byte[] crypted = cipher.doFinal(plainTextData);

			Encoder encoder = Base64.getEncoder();
			String output = encoder.encodeToString(crypted);
			return output;
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此加密算法");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			throw new Exception("加密公钥非法,请检查");
		} catch (IllegalBlockSizeException e) {
			throw new Exception("明文长度非法");
		} catch (BadPaddingException e) {
			throw new Exception("明文数据已损坏");
		}
	}

	/**
	 * 解密过程
	 *
	 * @param publicKey 私钥
	 * @param data      密文数据
	 * @return 明文
	 * @throws Exception 解密过程中的异常信息
	 */
	public static String decrypt(String publicKey, String data) throws Exception {
		RSAPublicKey rsaPublicKey = loadPublicKey(publicKey);
		if (publicKey == null) {
			throw new Exception("解密私钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
			byte[] cipherData = Base64.getDecoder().decode(data);
			byte[] output = cipher.doFinal(cipherData);
			return new String(output);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此解密算法");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			throw new Exception("解密私钥非法,请检查");
		} catch (IllegalBlockSizeException e) {
			throw new Exception("密文长度非法");
		} catch (BadPaddingException e) {
			throw new Exception("密文数据已损坏");
		}
	}

}
