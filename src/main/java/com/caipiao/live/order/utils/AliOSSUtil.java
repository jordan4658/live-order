package com.caipiao.live.order.utils;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.caipiao.live.order.config.AliOSSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云文件上传工具类
 */
public class AliOSSUtil {

    private static Logger logger = LoggerFactory.getLogger(AliOSSUtil.class);

    public static OSSClient buildOSSClient() {
        return new OSSClient(AliOSSProperties.ENDPOINT, AliOSSProperties.ACCESS_KEY_ID, AliOSSProperties.ACCESS_KEY_SECRET);
    }

    /**
     * 上传单个文件
     *
     * @param
     * @return
     */
    public static String upload(InputStream is, String prefix, String type, String folder) {
        long start = System.currentTimeMillis();
        logger.info("OSS文件上传开始,源文件名：{}", is.toString());

        // 获取年-月-日
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = format.format(new Date());

        // 创建OSSClient实例
        OSSClient ossClient = null;
        String url = "";
        try {
            ossClient = buildOSSClient();

            // 容器不存在，就创建
            if (!ossClient.doesBucketExist(AliOSSProperties.BUCKET_NAME)) {
                ossClient.createBucket(AliOSSProperties.BUCKET_NAME);
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(AliOSSProperties.BUCKET_NAME);
                createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
                ossClient.createBucket(createBucketRequest);
            }

            // 创建文件路径
            String filePath = type + "/" + folder + "/" + (dateStr + "/" + UUID.randomUUID().toString() + prefix);
            // 上传文件
            PutObjectResult result = ossClient.putObject(new PutObjectRequest(AliOSSProperties.BUCKET_NAME, filePath, is));
            // 设置权限 这里是公开读
            ossClient.setBucketAcl(AliOSSProperties.BUCKET_NAME, CannedAccessControlList.PublicRead);
            // 上传文件结果
            if (null != result) {
                url = AliOSSProperties.FILE_HOST + filePath;
            }
        } catch (Exception oe) {
            logger.error(oe.getMessage());
        } finally {
            logger.info("OSS文件上传成功,图片地址：{}.times:{} ms.", url, (System.currentTimeMillis() - start));
            // 关闭 OSS 服务
            shutdown(ossClient);
        }
        return url;
    }

    /**
     * 删除单个文件，删除多个文件调用 deleteMultiFiles 方法批量删除
     *
     * @param key
     */
    public static void deleteFile(String key) {
        key = getFileName(key);
        if (null == key) {
            return;
        }

        OSSClient ossClient = null;
        try {
            ossClient = buildOSSClient();
            ossClient.deleteObject(AliOSSProperties.BUCKET_NAME, key);
            logger.info("deleteFile for:{} succeed.", key);
        } catch (Exception e) {
            logger.error("deleteFile for:{} occur error:{}", key, e);
        } finally {
            shutdown(ossClient);
        }
    }

    /**
     * 批量删除文件
     *
     * @param keys
     */
    public static void deleteMultiFiles(List<String> keys) {
        keys = getFileName(keys);
        if (null == keys) {
            return;
        }

        OSSClient ossClient = null;
        try {
            ossClient = buildOSSClient();
            DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(new DeleteObjectsRequest(AliOSSProperties.BUCKET_NAME).withKeys(keys));
            List<String> deletedObjects = deleteObjectsResult.getDeletedObjects();
            logger.info("deleteMultiFiles:{} succeed.", JSONObject.toJSONString(deletedObjects));
        } catch (Exception e) {
            logger.error("deleteMultiFiles:{} occur error:{}", JSONObject.toJSONString(keys), e);
        } finally {
            shutdown(ossClient);
        }
    }

    /**
     * 获取名称
     *
     * @param name
     * @return
     */
    public static String getFileName(String name) {
        if (null == name || "".equals(name.trim())) {
            return null;
        }
        if (name.startsWith(AliOSSProperties.FILE_HOST)) {
            name = name.substring(AliOSSProperties.FILE_HOST.length());
        }
        return name;
    }

    /**
     * 批量获取名称
     *
     * @param names
     * @return
     */
    public static List<String> getFileName(List<String> names) {
        if (null == names || names.size() == 0) {
            return null;
        }
        List<String> list = new ArrayList<>(names.size());
        for (String name : names) {
            String curr = getFileName(name);
            if (null != curr) {
                list.add(curr);
            }
        }
        return list.size() == 0 ? null : list;
    }

    // 关闭OSSClient
    private static void shutdown(OSSClient ossClient) {
        if (null != ossClient) {
            ossClient.shutdown();
        }
    }

}
