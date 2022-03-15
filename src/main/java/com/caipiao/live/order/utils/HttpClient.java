package com.caipiao.live.order.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.Map;

public class HttpClient {

    private static Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private static void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * 发送Http POST请求
     *
     * @param url
     * @param param
     * @param header
     * @return
     */
    public static String sendPost(String url, String param, Map<String, String> header) {
        OutputStreamWriter out = null;
        BufferedReader in = null;
        String result = "";
        HttpURLConnection conn;

        try {
            trustAllHosts();
            URL realUrl = new URL(url);
            // 通过请求地址判断请求类型(http或者是https)
            if ("https".equals(realUrl.getProtocol().toLowerCase())) {
                HttpsURLConnection https = (HttpsURLConnection) realUrl.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) realUrl.openConnection();
            }
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
            // 发送POST请求必须设置如下两行

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");    // POST方法
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            if (header != null) {
                for (Map.Entry entry : header.entrySet()) {
                    conn.setRequestProperty(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            // 设置通用的请求属性
            conn.connect();

            // 获取URLConnection对象对应的输出流
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            // 发送请求参数
            out.write(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            logger.error("发送 POST 请求出现异常！===" + url + "===params===" + param + "===Error:" + e);
        }

        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static String getRequestBodyFromMap(Map parametersMap, boolean isUrlEncoding) {
        StringBuffer sbuffer = new StringBuffer();
        for (Object obj : parametersMap.keySet()) {
            String value = (String) parametersMap.get(obj);
            if (isUrlEncoding) {
                try {
                    value = URLEncoder.encode(value, "UTF-8");
                    if (value != null && value.isEmpty() == false) {
                        parametersMap.put(obj, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sbuffer.append(obj).append("=").append(value).append("&");
        }
        return sbuffer.toString().replaceAll("&$", "");
    }
}
