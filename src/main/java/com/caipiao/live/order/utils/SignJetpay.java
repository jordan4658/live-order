package com.caipiao.live.order.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;

public class SignJetpay {
    private static final Logger logger = LoggerFactory.getLogger(SignJetpay.class);

    public static String signMD5(String data) {
        String returnsign = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            returnsign = bytes2HexString(md.digest(data.getBytes("UTF-8")));
        } catch (Exception e) {
            logger.error("signMD5 occur error.", e);
        }
        return returnsign;

    }

    public static String lowerCaseMD5(String data, String byteType) {
        String result = "";
        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((data).getBytes(byteType));
            byte b[] = md.digest();

            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }

            result = buf.toString();
        } catch (Exception e) {
            logger.error("lowerCaseMD5 occur error.", e);
        }
        logger.info("result = " + result);
        return result;

    }

    public static String signMD5(String data, String byteType) {
        String returnsign = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            returnsign = bytes2HexString(md.digest(data.getBytes(byteType)));
        } catch (Exception e) {
            logger.error("signMD5 occur error.", e);
        }
        return returnsign;

    }

    public static String signRxMD5(String data) {
        String returnsign = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            returnsign = md.digest(data.getBytes("UTF-8")).toString();
        } catch (Exception e) {
            logger.error("signRxMD5 occur error.", e);
        }
        return returnsign;

    }

    public static String bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

}
