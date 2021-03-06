package com.caipiao.live.order.utils;


import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 */
public class JwtHttpUtil {
	private static final Logger logger = LoggerFactory.getLogger(JwtHttpUtil.class);
	/**
	 * 发起https请求并获取结果
	 * 
	 * @param requestUrl
	 *            请求地址
	 * @param requestMethod
	 *            请求方式（GET、POST）
	 * @param outputStr
	 *            提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 */
	public static JSONObject httpRequest(String requestUrl, String requestMethod, String outputStr, String sign) {
		JSONObject jsonObject = null;
		HttpResponse response = null;
		CloseableHttpClient client =  HttpClients.createDefault();
		try {
			HttpPost post = new HttpPost(requestUrl);
		    post.addHeader("X-AUTH-TOKEN",sign);
			post.addHeader("Content-Type","application/json");
		    post.setEntity(new StringEntity(outputStr,"UTF-8"));
		    response = client.execute(post);
			HttpEntity entity = response.getEntity();
			String  json = EntityUtils.toString(entity,"UTF-8");
			jsonObject = JSONObject.parseObject(json);
		} catch (ConnectException ce) {
		} catch (Exception e) {
			logger.error("httpRequest occur error.", e);
		} finally {
			try {
				client.close();
			} catch (Exception e) {
				logger.error("httpRequest occur error.", e);
			}
		}
		return jsonObject;
	}
	
	
	/**
	 * 发起https请求并获取结果
	 * 
	 * @param requestUrl
	 *            请求地址
	 * @param requestMethod
	 *            请求方式（GET、POST）
	 * @param outputStr
	 *            提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 */
	public static String httpRequest(String requestUrl, String requestMethod, String outputStr) {
		String res = "";
		StringBuffer buffer = new StringBuffer();
		HttpURLConnection httpUrlConn = null;
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			URL url = new URL(requestUrl);
			httpUrlConn = (HttpURLConnection) url.openConnection();
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			httpUrlConn.setRequestProperty("Accept", "text/plain");
			 httpUrlConn.setRequestProperty("Content-Type", "application/json");
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);
			if ("GET".equalsIgnoreCase(requestMethod)) {
                httpUrlConn.connect();
            }

			// 当有数据需要提交时
			if (null != outputStr) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			res = buffer.toString();
		} catch (ConnectException ce) {
		} catch (Exception e) {
			logger.error("httpRequest occur error.", e);
		} finally {
			try {
				httpUrlConn.disconnect();
			} catch (Exception e) {
				logger.error("httpRequest occur error.", e);
			}
		}
		return res;
	}

}
