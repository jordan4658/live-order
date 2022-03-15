package com.caipiao.live.order.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.model.dto.lottery.LotterySgModel;
import com.caipiao.live.common.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.Proxy.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 获取赛果接口数据工具类
 *
 * @author lzy
 * @create 2018-07-26 23:07
 */
@Configuration
@ConfigurationProperties(prefix = "sg")
public class GetHttpsgUtil {

    private static final Logger logger = LoggerFactory.getLogger(GetHttpsgUtil.class);

    private static String kcwtoken;

    private static String cpktoken;

    private static String cpkuid;


    public static String getKcwtoken() {
        return kcwtoken;
    }

    public static void setKcwtoken(String kcwtoken) {
        GetHttpsgUtil.kcwtoken = kcwtoken;
    }

    public static String getCpktoken() {
        return cpktoken;
    }

    public static void setCpktoken(String cpktoken) {
        GetHttpsgUtil.cpktoken = cpktoken;
    }

    public static String getCpkuid() {
        return cpkuid;
    }

    public static void setCpkuid(String cpkuid) {
        GetHttpsgUtil.cpkuid = cpkuid;
    }


    /**
     * 获取【彩票控】赛果记录
     *
     * @param code 彩种编号
     * @param num  记录条数
     * @return
     */
    public static List<LotterySgModel> getCpkSg(String code, int num) {
        ArrayList<LotterySgModel> sgModels = new ArrayList<>();
        //     String uid = "1039254"; // 用户ID
        //    String token = "e9f25742da0f490e24243acd2bf90c9c9dfa56cb"; // token
        String charset = "UTF-8";

        // 拼装URL
        String url = "http://api.caipiaokong.com/lottery/?"
                + "name=" + code
                + "&format=json" // 数据格式，此文件仅支持json
                + "&uid=" + cpkuid
                + "&token=" + cpktoken
                + "&num=" + num; // 默认只获取一条赛果记录

        String jsonStr = get(url, charset); // 得到JSON字符串
        if (StringUtils.isBlank(jsonStr)) {
            return sgModels;
        }
        JSONObject object = JSONObject.parseObject(jsonStr); // 转化为JSON类
        try {
            Set<Map.Entry<String, Object>> entries = object.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                String entryKey = entry.getKey();
                Map<String, String> value = (Map<String, String>) entry.getValue();
                // 判断是否有开奖号码
                if (StringUtils.isBlank(value.get("number"))) {
                    return sgModels;
                }
                LotterySgModel sgModel = new LotterySgModel();
                sgModel.setIssue(entryKey);
                sgModel.setSg(value.get("number"));
                sgModel.setDate(value.get("dateline"));
                sgModels.add(sgModel);
            }
        } catch (JSONException e) {
            logger.error("获取【彩票控】赛果记录出错", e);
        }
        return sgModels;
    }

    /**
     * 获取【开彩网】赛果记录
     *
     * @param code 彩种编号
     * @param num  记录条数
     * @return
     */
    public static List<LotterySgModel> getKcwSg(String code, int num) {
        List<LotterySgModel> results = new ArrayList<>();
        //     String token = "t0248909c3620ff1bk";
        String charset = "UTF-8";
        String url = "http://wd.apiplus.net/newly.do?"
                + "token=" + kcwtoken
                + "&code=" + code
                + "&rows=" + num
                + "&format=json";

        String jsonStr = get(url, charset);
        if (StringUtils.isBlank(jsonStr)) {
            return results;
        }
        // 解析返回的json字符串
        JSONObject object = JSONObject.parseObject(jsonStr); // 转化为JSON类
        // 获取开奖期数信息
        JSONArray data = object.getJSONArray("data");
        LotterySgModel model;
        // 遍历获取出的赛果
        for (int i = 0; i < data.size(); i++) {
            model = new LotterySgModel();
            JSONObject jsonObject = data.getJSONObject(i);
            model.setIssue(jsonObject.getString("expect"));
            model.setDate(jsonObject.getString("opentime"));
            model.setSg(jsonObject.getString("opencode"));
            results.add(model);
        }
        return results;
    }

    /**
     * 请求接口获取json格式数据
     *
     * @param url     彩种
     * @param charset 字符编码
     * @return 返回json结果
     */
    public static String get(String url, String charset) {
        String result = "";
        try {
            BufferedReader reader;
            StringBuilder sbf = new StringBuilder();
            String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.66 Safari/537.36";

            URL httpUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(30000);
            connection.setConnectTimeout(30000);
            connection.setRequestProperty("User-agent", userAgent);
            connection.connect();
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, charset));
            String strRead;
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            result = sbf.toString();
        } catch (Exception e) {
            logger.error("获取{}记录出错", url, e);
        }
        return result;
    }

    public static List<LotterySgModel> getAct() {
        List<LotterySgModel> list = new ArrayList<>();
        SocketAddress address = new InetSocketAddress("47.74.70.72", 8888);
        Proxy proxy = new Proxy(Type.HTTP, address);
        try {
            URL url = new URL("https://api-info-act.keno.com.au/v2/info/history?jurisdiction=ACT");
            URLConnection connection = url.openConnection(proxy);
            connection.setConnectTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; NT 5.1; GTB5; .NET CLR 2.0.50727; CIBA)");
            connection.getContent();
            URL url2 = new URL("https://api-info-act.keno.com.au/v2/games/kds?jurisdiction=ACT");
            URLConnection connection2 = url2.openConnection(proxy);
            connection2.setConnectTimeout(5000);
            connection2.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; NT 5.1; GTB5; .NET CLR 2.0.50727; CIBA)");
            connection2.getContent();
            String json = ParseStream(connection.getInputStream());
            String jsonnew = ParseStream(connection2.getInputStream());
            JSONObject current = null;
            JSONObject selling = null;
            if (StringUtils.isNotBlank(jsonnew)) {
                JSONObject actobj = JSONObject.parseObject(jsonnew);
                current = actobj.getJSONObject("current");
                selling = actobj.getJSONObject("selling");
            }

            if (StringUtils.isNotBlank(json)) {
                JSONObject actobj = JSONObject.parseObject(json);
                JSONArray actarry = actobj.getJSONArray("items");
                if (current != null) {
                    actarry.add(current);
                }
                for (Object fl : actarry) {
                    int issue = ((JSONObject) fl).getInteger("game-number");
                    JSONArray draw = ((JSONObject) fl).getJSONArray("draw");
                    String sg = "";
                    for (Object num : draw) {
                        sg += num + ",";
                    }
                    String date = ((JSONObject) fl).getString("closed");
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    //设置时区UTC
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    //格式化，转当地时区时间
                    Date after = df.parse(date);


                    df.applyPattern(DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
                    //默认时区
                    df.setTimeZone(TimeZone.getDefault());
                    String dateact = df.format(after);
                    df.applyPattern("yyyyMMdd");
                    LotterySgModel model = new LotterySgModel();
                    model.setIssue(df.format(after) + String.format("%03d", issue));
                    model.setSg(sg.substring(0, sg.length() - 1));
                    model.setDate(dateact);
                    model.setStatus(Constants.AUTO);
                    model.setOpenTime(dateact);
                    list.add(model);
                }

                //添加一个 预期数据
                int issue = selling.getInteger("game-number");
                String date = selling.getString("closing");

                logger.info(current.getInteger("game-number") + "," + issue);

                LotterySgModel model = new LotterySgModel();

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

                //设置时区UTC
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                //格式化，转当地时区时间
                Date dDate = df.parse(date);

                df.applyPattern(DateUtils.FORMAT_YYYY_MM_DD_HHMMSS);
                //默认时区
                df.setTimeZone(TimeZone.getDefault());
                String dateShiqu = df.format(dDate);

                df.applyPattern("yyyyMMdd");
                model.setDate(dateShiqu);
                model.setIssue(df.format(dDate) + String.format("%03d", issue));
                list.add(model);

            }
        } catch (Exception e) {
            logger.error("获取【彩票控】赛果记录出错", e);
        }

        return list;
    }

    public static String ParseStream(InputStream stream) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String strtmp = reader.readLine();
            while (null != strtmp) {
                builder.append(strtmp);
                builder.append("\n");
                strtmp = reader.readLine();
            }
        } catch (Exception e) {
            logger.error("读取解析流出错", e);
        }
        return builder.toString();
    }
}
