package com.caipiao.live.order.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class ToolKitUtil {

	private static final Logger logger = LoggerFactory.getLogger(ToolKitUtil.class);

	static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	// 支付公钥
	public static final String PAY_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCincPGckpU5ifoz/L+gxPuJ41ehfRkIVOznJgQ" +
			"mSBIg0wjRoPGwgyLa7HK9fw6z3zuoQcqXD3JfxGdzhEknQ6dUSrq6sbBUviApP7AshFauVe0gO6N" +
			"nv2XwKx48P+fTWS5ICc+so62Kvj8yD+zgM10zpsG1RHTcDd8dQwbYqwHCwIDAQAB";
	// 代付公钥
	public static final String REMIT_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC2woYyPPlPpKsCUPFsgvym8v8VtCtBivHWzwMk" +
			"MOibWEq7KhTFdlJJ4CN1Gi3ZPS2dlN0bN8CTQlytGgb3lMK1YkNNQf/zmCjjd/WfJgu7Q3ILH2aR" +
			"RZa1/lghd44APSGeFsANbg8lqHJXcq4ToDWgbVpTssg8XYwDGygKJnrPxQIDAQAB";

	// 商户私钥
	public static final String PRIVATE_KEY =
			"MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJVqiPw1HZANo7N8ZXjjU8fQRALv" +
			"k8z1Lr8P4HZCn0041SsEw1P4o9/E6REeKO80v/vAHUCje40UmYIhgNRbMxau/2wqG7y0ef44aQ6+" +
			"EVrurir2lDi9hki+yZ3wE64PBzmTeupRuN3RjfCXlc/xCbjEkeVl6T1NoFyfiV91CtPRAgMBAAEC" +
			"gYALkgNnnGlcpWG+3WwyL1Bmkb8ihxDn9ziWfRFBtykE3tjLCwRhNUjjZCk0NSSwWWJgiWD6h25i" +
			"32x/wj5qs2FCSpAjDn9N7Fo5wqbucUVULHMAMs7qTmW5Pq7c5QU0FEVIipbXkd6TZ8JqASz1QDuR" +
			"V14+a9fFto+uXbkdCAR4UQJBAO11Aj6r4Ei41xhhECfph4sx/+slvrVgqTYwlqdpL8GSzZBWUHw2" +
			"WeZF4s8fUyO3HmmnEOcBKHEfcOqM3myN5CUCQQChFYFYBfWPG5eRy+lj4AW+BaTf/nyWssgetjS9" +
			"PFHwXlZT+/xqNaAzbLAHkcF/CzJYHQglNWISwsDCYU1fYGs9AkAMxozWXlz0zK0V7LL30sKuJISG" +
			"XEOEcqzn83lHjIs7OPDCYUJ3TF1N3Fi1mBPbChEi0hBAvXk1jOGAXQ/ie0fZAkA+FPW2oQVU1EOE" +
			"M1yN24cbBz3V/lSyF9E81OhajmbeBt2qnJFZtbU6XIczQ5+ZgQ00HGxJOtvDcz+rKqkROy3ZAkB/" +
			"TGTWf7Br5tpL30L42IkoaGDGOm/sdRtU2+T9JbCxZ8DNCtSBZsFXZW9AnZYekvILTPuEOa6fd+vA" +
			"Y1zJnWbo";

	//public static int blockSize = 128;
	// 非对称密钥算法
	public static final String KEY_ALGORITHM = "RSA";
	public final static String CHARSET = "UTF-8";

	/**
	 * 获取响应报文
	 *
	 * @param in
	 * @return
	 */
	private static String getResponseBodyAsString(InputStream in) {
		try {
			BufferedInputStream buf = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuffer data = new StringBuffer();
			int readDataLen;
			while ((readDataLen = buf.read(buffer)) != -1) {
				data.append(new String(buffer, 0, readDataLen, CHARSET));
			}
			return data.toString();
		} catch (Exception e) {
			logger.error("获取响应报文出错",e );
		}
		return null;
	}

	/**
	 * 提交请求
	 *
	 * @param url
	 * @param params
	 * @return
	 */
	public static String request(String url, String params) {
		try {
            logger.info("请求报文:" + params);
			URL urlObj = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setConnectTimeout(1000 * 5);
			//conn.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
			//conn.setRequestProperty("Accept","*/*");
			conn.setRequestProperty("Charset", CHARSET);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(params.length()));
			OutputStream outStream = conn.getOutputStream();
			outStream.write(params.toString().getBytes(CHARSET));
			outStream.flush();
			outStream.close();
			return getResponseBodyAsString(conn.getInputStream());
		} catch (Exception e) {
            logger.error("提交请求出错",e );
			return null;
		}
	}

	/**
	 * MD5加密
	 *
	 * @param s
	 * @param encoding
	 * @return
	 */
	public final static String MD5(String s, String encoding) {
		try {
			byte[] btInput = s.getBytes(encoding);
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			mdInst.update(btInput);
			byte[] md = mdInst.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
				str[k++] = HEX_DIGITS[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
            logger.error("MD5加密出错",e );
			return null;
		}
	}

	/**
	 * 转换成Json格式
	 *
	 * @param map
	 * @return
	 */
	public static String mapToJson(Map<String, String> map) {
		Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
		StringBuffer json = new StringBuffer();
		json.append("{");
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			String key = entry.getKey();
			String value = entry.getValue();
			json.append("\"").append(key).append("\"");
			json.append(":");
			json.append("\"").append(value).append("\"");
			if (it.hasNext()) {
				json.append(",");
			}
		}
		json.append("}");
        logger.info("mapToJson=" + json.toString());
		return json.toString();
	}

	/**
	 * 生成随机字符
	 *
	 * @param num
	 * @return
	 */
	public static String randomStr(int num) {
		char[] randomMetaData = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
				'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
				'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4',
				'5', '6', '7', '8', '9' };
		Random random = new Random();
		String tNonceStr = "";
		for (int i = 0; i < num; i++) {
			tNonceStr += (randomMetaData[random.nextInt(randomMetaData.length - 1)]);
		}
		return tNonceStr;
	}

	/**
	 * 公钥加密
	 *
	 * @param data 待加密数据
	 * @param publicKey
	 *            密钥
	 * @return byte[] 加密数据
	 */
	public static byte[] encryptByPublicKey(byte[] data, String publicKey) {
		byte[] key = Base64.getDecoder().decode(publicKey);
		// 实例化密钥工厂
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			// 密钥材料转换
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(key);
			// 产生公钥
			PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);
			// 数据加密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			int blockSize = cipher.getOutputSize(data.length) - 11;
			return doFinal(data, cipher,blockSize);

		} catch (NoSuchAlgorithmException e) {
            logger.error("公钥加密出错",e );
		} catch (InvalidKeySpecException e) {
            logger.error("公钥加密出错",e );
		} catch (NoSuchPaddingException e) {
            logger.error("公钥加密出错",e );
		} catch (InvalidKeyException e) {
            logger.error("公钥加密出错",e );
		} catch (IllegalBlockSizeException e) {
            logger.error("公钥加密出错",e );
		} catch (BadPaddingException e) {
            logger.error("公钥加密出错",e );
		} catch (IOException e) {
            logger.error("公钥加密出错",e );
		}
		return null;
	}

	/**
	 * 私钥解密
	 *
	 * @param data
	 *            待解密数据
	 * @param privateKeyValue
	 *            密钥
	 * @return byte[] 解密数据
	 */
	public static byte[] decryptByPrivateKey(byte[] data, String privateKeyValue) {
		byte[] key = Base64.getDecoder().decode(privateKeyValue);
		try {
			// 取得私钥
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			// 生成私钥
			PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
			// 数据解密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			int blockSize = cipher.getOutputSize(data.length);
			return doFinal(data, cipher,blockSize);
		} catch (NoSuchAlgorithmException e) {
            logger.error("公钥加密出错",e );
		} catch (InvalidKeySpecException e) {
            logger.error("公钥加密出错",e );
		} catch (NoSuchPaddingException e) {
            logger.error("公钥加密出错",e );
		} catch (InvalidKeyException e) {
            logger.error("公钥加密出错",e );
		} catch (IllegalBlockSizeException e) {
            logger.error("公钥加密出错",e );
		} catch (BadPaddingException e) {
            logger.error("公钥加密出错",e );
		} catch (IOException e) {
            logger.error("公钥加密出错",e );
		}
		return null;
	}

	/**
	 * 加密解密共用核心代码，分段加密解密
	 *
	 * @param decryptData
	 * @param cipher
	 * @return
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws IOException
	 */
	public static byte[] doFinal(byte[] decryptData, Cipher cipher,int blockSize)
			throws IllegalBlockSizeException, BadPaddingException, IOException {
		int offSet = 0;
		byte[] cache = null;
		int i = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while (decryptData.length - offSet > 0) {
			if (decryptData.length - offSet > blockSize) {
				cache = cipher.doFinal(decryptData, offSet, blockSize);
			} else {
				cache = cipher.doFinal(decryptData, offSet, decryptData.length - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * blockSize;
		}
		byte[] encryptedData = out.toByteArray();
		out.close();
		return encryptedData;
	}

}
