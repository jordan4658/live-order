package com.caipiao.live.order.utils;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;

public class LongRunUtil {
	
	/**
  	 * 获取签名字符串
  	 * 签名字符串拼装规则   对键排序  对值竖线拼接  value1|value2|value3|....|md5Key
  	 * @param reqDataMap
  	 * @param secretKey
  	 * @return
  	 */
  	public static String getSignDataStr(Map<String, String> resultMap,String md5Key) {
  		 if (resultMap != null) {
			Object[] keys = resultMap.keySet().toArray();
			Arrays.sort(keys);
			StringBuilder  builder = new StringBuilder();
			for (Map.Entry<String, String> mapEntry:resultMap.entrySet()) {
				builder.append(mapEntry.getValue());
				builder.append("|");
			}
			builder.append(md5Key);
			return builder.toString();
		}
  		 return  null;
  	}
  	
  	
  	/**
  	* MD5 加密方法
  	* @param s
  	* @param encoding
  	* @return
  	*/
	static final char HEX_DIGITS[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
  	
	public final static String md5Key(String data, String encoding) {
		try {
			byte [] byteIn  = data.getBytes(encoding);
			MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(byteIn);
			byte []  md =  mDigest.digest();
			int j = md.length;
			char str []  = new char[j *2];
			int k  = 0 ;
			for (int i = 0; i < j; i++) {
			   byte byte1 = md[i];
			   str[k++] = HEX_DIGITS[byte1>>>4 & 0xf];
			   str[k++] = HEX_DIGITS[byte1 & 0xf];
			}
			
			return new String(str);
		} catch (Exception e) {
			return null;
		}
	}
	
	
	/**
	* Http 请求
	* @param url  请求地址
	* @param jsonStr  请求json数据
	* @param timeOut 请求连接超时时间
	* @return
	*/
	public static String requestPostByJson(String url, String jsonStr,int timeOut){
		HttpURLConnection conn = null;
		try {
			URL urlObj = new URL(url);
			conn = (HttpURLConnection) urlObj.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setConnectTimeout(timeOut);
			// 设置文件类型:
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Content-Length",String.valueOf(jsonStr.length()));
			OutputStream outStream = conn.getOutputStream();
			outStream.write(jsonStr.toString().getBytes("utf-8"));
			outStream.flush();
			outStream.close();
			if (conn.getResponseCode() ==200) {
				BufferedInputStream buf = new BufferedInputStream(conn.getInputStream());
				byte[] buffer = new  byte[1024];
				StringBuilder builder = new StringBuilder();
				int readDateLen;
				while ((readDateLen = buf.read(buffer))!= -1) {
					builder.append(new String(buffer,0,readDateLen,"UTF-8"));
				}
				return builder.toString();
			 }
		}catch (Exception e) {
			return null;
		}
		return null;
	}

}
