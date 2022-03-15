package com.caipiao.live.order.service.lottery.impl;


import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.AppMianParamEnum;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.enums.lottery.LotteryOpenStatusEnum;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.*;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.lottery.PceggUtil;
import com.caipiao.live.common.util.lottery.XyftUtils;
import com.caipiao.live.common.util.redis.RedisBusinessUtil;
import com.caipiao.live.order.service.lottery.LotterySgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lzy
 * @create 2018-08-27 14:18
 **/
@Service
public class LotterySgServiceImpl implements LotterySgService {

    private final static Logger logger = LoggerFactory.getLogger(LotterySgServiceImpl.class);

    @Resource
    private TensscLotterySgMapper tensscLotterySgMapper;
    @Resource
    private FivesscLotterySgMapper fivesscLotterySgMapper;
    @Resource
    private JssscLotterySgMapper jssscLotterySgMapper;
    @Resource
    private TenbjpksLotterySgMapper tenbjpksLotterySgMapper;
    @Resource
    private FivebjpksLotterySgMapper fivebjpksLotterySgMapper;
    @Resource
    private JsbjpksLotterySgMapper jsbjpksLotterySgMapper;
    @Resource
    private XyftLotterySgMapper xyftLotterySgMapper;
    @Resource
    private DzxyftLotterySgMapper dzxyftLotterySgMapper;
    @Resource
    private DzpceggLotterySgMapper dzpceggLotterySgMapper;


    @Override
    public Map<String, Object> lishiSg(Integer pageNo, Integer pageSize, Integer id) {
        Map<String, Object> resultMap = new HashMap<>();
        CaipiaoTypeEnum caipiaoTypeEnum = CaipiaoTypeEnum.valueOfTagType(String.valueOf(id));
        if (null == caipiaoTypeEnum) {
            return resultMap;
        }

      //  TENSSC("tenssc", "10分时时彩", "1104"),
              //  FIVESSC("fivessc", "5分时时彩", "1105"),
              //  JSSSC("jsssc", "德州时时彩", "1106"),
              //  TENPKS("tenpks", "10分PK10", "1302"),
              //  FIVEPKS("fivepks", "5分PK10", "1303"),
               // JSPKS("jspks", "德州PK10", "1304"),
               // XYFEIT("xyft", "幸运飞艇", "1401"),
              //  DZXYFEIT("dzxyft", "德州幸运飞艇", "1402"),
              //  DZPCDAND("dzpcegg", "德州PC蛋蛋", "1502"),


        switch (caipiaoTypeEnum){
            case TENSSC:
                resultMap = this.lishitensscSg(pageNo, pageSize);
                break;
            case FIVESSC:
                resultMap = this.lishiFivesscSg(pageNo, pageSize);
                break;
            case JSSSC:
                resultMap = this.lishijssscSg(pageNo, pageSize);
                break;
            case TENPKS:
                resultMap = this.lishitenpksSg(pageNo, pageSize);
                break;
            case FIVEPKS:
                resultMap = this.lishifivepksSg(pageNo, pageSize);
                break;
            case JSPKS:
                resultMap = this.lishijspksSg(pageNo, pageSize);
                break;
            case XYFEIT:
                resultMap = this.lishixyfeitSg(pageNo, pageSize);
                break;
            case DZXYFEIT:
                resultMap = this.lishiDzxyfeitSg(pageNo, pageSize);
                break;
            case DZPCDAND:
                resultMap = this.lishiDzpcdandSg(pageNo, pageSize);
                break;
        }


        return resultMap;
    }


    @Override
    public Map<String, Object> lishitensscSg(Integer pageNo, Integer pageSize) {
        com.caipiao.live.common.mybatis.entity.TensscLotterySgExample example = new TensscLotterySgExample();
        TensscLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusNotEqualTo(LotteryOpenStatusEnum.WAIT.name());
        criteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<TensscLotterySg> tensscLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询

        if (!RedisBusinessUtil.exists(RedisKeys.TENSSC_SG_HS_LIST)) {
            TensscLotterySgExample exampleOne = new TensscLotterySgExample();
            TensscLotterySgExample.Criteria tensscCriteriaOne = exampleOne.createCriteria();
            tensscCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<TensscLotterySg> tensscLotterySgsOne = tensscLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addTensscLotterySgList(tensscLotterySgsOne);
        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            tensscLotterySgs =   RedisBusinessUtil.getTensscLotterySgList((pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            tensscLotterySgs = tensscLotterySgMapper.selectByExample(example);
        }

//        List<TensscLotterySg> lotterySgs = tensscLotterySgMapper.selectByExample(example);
        //logger.info("lishitensscSg lotterySgs:{}", JSONObject.toJSONString(lotterySgs));

        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (TensscLotterySg sg : tensscLotterySgs) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("issue", sg.getIssue());
            map.put("time", sg.getTime());
            map.put("number", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
                    .append(sg.getShi()).append(sg.getGe()).toString());
            map.put("numberstr", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
                    .append(sg.getShi()).append(sg.getGe()).toString());
            map.put("he", sg.getWan() + sg.getQian() + sg.getBai() + sg.getShi() + sg.getGe());

            maps.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }



    @Override
    public Map<String, Object> lishiFivesscSg(Integer pageNo, Integer pageSize) {
        FivesscLotterySgExample example = new FivesscLotterySgExample();
        FivesscLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusNotEqualTo(LotteryOpenStatusEnum.WAIT.name());
//        criteria.andWanIsNotNull();
        criteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);
//        criteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<FivesscLotterySg> fivesscLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.FIVESSC_SG_HS_LIST)) {
            FivesscLotterySgExample exampleOne = new FivesscLotterySgExample();
            FivesscLotterySgExample.Criteria fivesscCriteriaOne = exampleOne.createCriteria();
            fivesscCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<FivesscLotterySg> fivesscLotterySgsOne = fivesscLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addFivesscLotterySgList(fivesscLotterySgsOne);

        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            fivesscLotterySgs =  RedisBusinessUtil.getFivesscLotterySgList((pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            fivesscLotterySgs = fivesscLotterySgMapper.selectByExample(example);
        }
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (FivesscLotterySg sg : fivesscLotterySgs) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("issue", sg.getIssue());
            map.put("time", sg.getTime());

            map.put("number", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
                    .append(sg.getShi()).append(sg.getGe()).toString());
            map.put("numberstr", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
                    .append(sg.getShi()).append(sg.getGe()).toString());
            // map.put("numberstr", CaipiaoNumberFormatUtils.NumberFormat(sg.getWan(),
            // sg.getQian(), sg.getBai(), sg.getShi(), sg.getGe()));
            map.put("he", sg.getWan() + sg.getQian() + sg.getBai() + sg.getShi() + sg.getGe());

            maps.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }



    @Override
    public Map<String, Object> lishijssscSg(Integer pageNo, Integer pageSize) {
        JssscLotterySgExample example = new JssscLotterySgExample();
        JssscLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andOpenStatusNotEqualTo(LotteryOpenStatusEnum.WAIT.name());
        //        criteria.andWanIsNotNull();
        criteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);
//        criteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<JssscLotterySg> jssscLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.JSSSC_SG_HS_LIST)) {
            JssscLotterySgExample exampleOne = new JssscLotterySgExample();
            JssscLotterySgExample.Criteria jssscCriteriaOne = exampleOne.createCriteria();
            jssscCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<JssscLotterySg> jssscLotterySgsOne = jssscLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addJssscLotterySgList(jssscLotterySgsOne);

        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            jssscLotterySgs =  RedisBusinessUtil.getJssscLotterySgList( (pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            jssscLotterySgs = jssscLotterySgMapper.selectByExample(example);
        }

//        List<JssscLotterySg> lotterySgs = jssscLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (JssscLotterySg sg : jssscLotterySgs) {
            Map<String, Object> map = new HashMap<String, Object>();

            map.put("issue", sg.getIssue());
            map.put("time", sg.getTime());

//            if(StringUtils.isEmpty(sg.getTime())){
//                map.put("time", sg.getIdealTime());
//            }else{
//                map.put("time", sg.getTime());
//            }

//            if(sg.getWan() == null){
//                map.put(Constants.SGSIGN, 0);
//            }else{
//                map.put(Constants.SGSIGN, 1);
//                map.put("number", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
//                        .append(sg.getShi()).append(sg.getGe()).toString());
//                map.put("numberstr", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
//                        .append(sg.getShi()).append(sg.getGe()).toString());
//                // map.put("numberstr", CaipiaoNumberFormatUtils.NumberFormat(sg.getWan(),
//                // sg.getQian(), sg.getBai(), sg.getShi(), sg.getGe()));
//                map.put("he", sg.getWan() + sg.getQian() + sg.getBai() + sg.getShi() + sg.getGe());
//            }

            map.put("number", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
                    .append(sg.getShi()).append(sg.getGe()).toString());
            map.put("numberstr", new StringBuffer().append(sg.getWan()).append(sg.getQian()).append(sg.getBai())
                    .append(sg.getShi()).append(sg.getGe()).toString());
            // map.put("numberstr", CaipiaoNumberFormatUtils.NumberFormat(sg.getWan(),
            // sg.getQian(), sg.getBai(), sg.getShi(), sg.getGe()));
            map.put("he", sg.getWan() + sg.getQian() + sg.getBai() + sg.getShi() + sg.getGe());
            maps.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }


    @Override
    public Map<String, Object> lishitenpksSg(Integer pageNo, Integer pageSize) {
        TenbjpksLotterySgExample example = new TenbjpksLotterySgExample();
//        example.createCriteria()
//                .andOpenStatusNotEqualTo(LotteryOpenStatusEnum.WAIT.name())
//                .andNumberIsNotNull();
        example.createCriteria().andOpenStatusEqualTo(Constants.STATUS_AUTO);

//        example.createCriteria().andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<TenbjpksLotterySg> tenbjpksLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.TENPKS_SG_HS_LIST)) {
            TenbjpksLotterySgExample exampleOne = new TenbjpksLotterySgExample();
            TenbjpksLotterySgExample.Criteria tenpksCriteriaOne = exampleOne.createCriteria();
            tenpksCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<TenbjpksLotterySg> tenbjpksLotterySgsOne = tenbjpksLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addTenpksLotterySgList(tenbjpksLotterySgsOne);
        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            tenbjpksLotterySgs = RedisBusinessUtil.getTenpksLotterySgList((pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            tenbjpksLotterySgs = tenbjpksLotterySgMapper.selectByExample(example);
        }

//        List<TenbjpksLotterySg> bjpksLotterySgs = tenbjpksLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (TenbjpksLotterySg sg : tenbjpksLotterySgs) {
            Map<String, Object> map = new HashMap<>();
            map.put("issue", sg.getIssue());
            map.put("time", sg.getTime());

//            if(StringUtils.isEmpty(sg.getTime())){
//                map.put("time", sg.getIdealTime());
//            }else{
//                map.put("time", sg.getTime());
//            }

//            if(StringUtils.isEmpty(sg.getNumber())){
//                map.put(Constants.SGSIGN, 0);
//            }else{
//                map.put(Constants.SGSIGN, 1);
//                map.put("number", sg.getNumber());
//                map.put("numberstr", sg.getNumber());
//            }

            map.put("number", sg.getNumber());
            map.put("numberstr", sg.getNumber());
            maps.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }


    @Override
    public Map<String, Object> lishifivepksSg(Integer pageNo, Integer pageSize) {
        FivebjpksLotterySgExample example = new FivebjpksLotterySgExample();
        FivebjpksLotterySgExample.Criteria bjpksCriteria = example.createCriteria();
//        bjpksCriteria.andOpenStatusNotEqualTo(LotteryOpenStatusEnum.WAIT.name());
//        bjpksCriteria.andNumberIsNotNull();
        bjpksCriteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);

//        bjpksCriteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<FivebjpksLotterySg> fivebjpksLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.FIVEPKS_SG_HS_LIST)) {
            FivebjpksLotterySgExample exampleOne = new FivebjpksLotterySgExample();
            FivebjpksLotterySgExample.Criteria fivebjpksCriteriaOne = exampleOne.createCriteria();
            fivebjpksCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<FivebjpksLotterySg> fivebjpksLotterySgsOne = fivebjpksLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addFivepksLotterySgList(fivebjpksLotterySgsOne);

        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            fivebjpksLotterySgs =   RedisBusinessUtil.getFivepksLotterySgList((pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            fivebjpksLotterySgs = fivebjpksLotterySgMapper.selectByExample(example);
        }

//        List<FivebjpksLotterySg> bjpksLotterySgs = fivebjpksLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (FivebjpksLotterySg sg : fivebjpksLotterySgs) {
            Map<String, Object> map = new HashMap<>();
            map.put("issue", sg.getIssue());
            map.put("time", sg.getTime());

//            if(StringUtils.isEmpty(sg.getTime())){
//                map.put("time", sg.getIdealTime());
//            }else{
//                map.put("time", sg.getTime());
//            }

//            if(StringUtils.isEmpty(sg.getNumber())){
//                map.put(Constants.SGSIGN, 0);
//            }else{
//                map.put(Constants.SGSIGN, 1);
//                map.put("number", sg.getNumber());
//                map.put("numberstr", sg.getNumber());
//            }

            map.put("number", sg.getNumber());
            map.put("numberstr", sg.getNumber());

            maps.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }


    @Override
    public Map<String, Object> lishijspksSg(Integer pageNo, Integer pageSize) {
        JsbjpksLotterySgExample example = new JsbjpksLotterySgExample();
        JsbjpksLotterySgExample.Criteria bjpksCriteria = example.createCriteria();
//        bjpksCriteria.andOpenStatusNotEqualTo(LotteryOpenStatusEnum.WAIT.name());
//        bjpksCriteria.andNumberIsNotNull();
        bjpksCriteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);

//        bjpksCriteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<JsbjpksLotterySg> jsbjpksLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.JSPKS_SG_HS_LIST)) {
            JsbjpksLotterySgExample exampleOne = new JsbjpksLotterySgExample();
            JsbjpksLotterySgExample.Criteria jsbjpksCriteriaOne = exampleOne.createCriteria();
            jsbjpksCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<JsbjpksLotterySg> jsbjpksLotterySgsOne = jsbjpksLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addJspksLotterySgList(jsbjpksLotterySgsOne);
        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            jsbjpksLotterySgs = RedisBusinessUtil.getJspksLotterySgList( (pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            jsbjpksLotterySgs = jsbjpksLotterySgMapper.selectByExample(example);
        }

//        List<JsbjpksLotterySg> bjpksLotterySgs = jsbjpksLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (JsbjpksLotterySg sg : jsbjpksLotterySgs) {
            Map<String, Object> map = new HashMap<>();
            map.put("issue", sg.getIssue());
            map.put("time", sg.getTime());

//            if(StringUtils.isEmpty(sg.getTime())){
//                map.put("time", sg.getIdealTime());
//            }else{
//                map.put("time", sg.getTime());
//            }

//            if(StringUtils.isEmpty(sg.getNumber())){
//                map.put(Constants.SGSIGN, 0);
//            }else{
//                map.put(Constants.SGSIGN, 1);
//                map.put("number", sg.getNumber());
//                map.put("numberstr", sg.getNumber());
//            }

            map.put("number", sg.getNumber());
            map.put("numberstr", sg.getNumber());
            maps.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }


    @Override
    public Map<String, Object> lishixyfeitSg(Integer pageNo, Integer pageSize) {
        XyftLotterySgExample example = new XyftLotterySgExample();
        XyftLotterySgExample.Criteria sgCriteria = example.createCriteria();
//        sgCriteria.andNumberIsNotNull();
//        criteria.andWanIsNotNull();
        sgCriteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);

//        sgCriteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("`ideal_time` DESC");

        List<XyftLotterySg> xyftLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.XYFT_SG_HS_LIST)) {
            XyftLotterySgExample exampleOne = new XyftLotterySgExample();
            XyftLotterySgExample.Criteria cqsscCriteriaOne = exampleOne.createCriteria();
            cqsscCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<XyftLotterySg> xyftLotterySgsOne = xyftLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addXyftLotterySgList(xyftLotterySgsOne);
        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            xyftLotterySgs =  RedisBusinessUtil.getXyftLotterySgList((pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            xyftLotterySgs = xyftLotterySgMapper.selectByExample(example);
        }

//        List<XyftLotterySg> xyftLotterySgs = xyftLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = XyftUtils.lishiSg(xyftLotterySgs);
        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }


    @Override
    public Map<String, Object> lishiDzxyfeitSg(Integer pageNo, Integer pageSize) {
        DzxyftLotterySgExample example = new DzxyftLotterySgExample();
        DzxyftLotterySgExample.Criteria dzxyftCriteria = example.createCriteria();
        dzxyftCriteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);
//        bjpksCriteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<DzxyftLotterySg> dzxyftLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.DZXYFT_SG_HS_LIST)) {
            DzxyftLotterySgExample exampleOne = new DzxyftLotterySgExample();
            DzxyftLotterySgExample.Criteria cqsscCriteriaOne = exampleOne.createCriteria();
            cqsscCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<DzxyftLotterySg> dzxyftLotterySgsOne = dzxyftLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addDzxyftLotterySgList(dzxyftLotterySgsOne);
        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            dzxyftLotterySgs =   RedisBusinessUtil.getDzxyftLotterySgList( (pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            dzxyftLotterySgs = dzxyftLotterySgMapper.selectByExample(example);
        }

//        List<DzxyftLotterySg> dzxyftLotterySgs = dzxyftLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = this.lishiDzxyfeitSg(dzxyftLotterySgs);
        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }


    @Override
    public Map<String, Object> lishiDzpcdandSg(Integer pageNo, Integer pageSize) {
        DzpceggLotterySgExample example = new DzpceggLotterySgExample();
        DzpceggLotterySgExample.Criteria dzpceggCriteria = example.createCriteria();
        dzpceggCriteria.andOpenStatusEqualTo(Constants.STATUS_AUTO);
//        criteria.andWanIsNotNull();

//        bjpksCriteria.andIdealTimeLessThan(DateUtils.formatDate(new Date(),DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("ideal_time DESC");

        List<DzpceggLotterySg> dzpceggLotterySgs = null;
        //存储100条 最新历史数据到缓存里，供页面查询
        if (!RedisBusinessUtil.exists(RedisKeys.DZPCEGG_SG_HS_LIST)) {
            DzpceggLotterySgExample exampleOne = new DzpceggLotterySgExample();
            DzpceggLotterySgExample.Criteria dzpceggCriteriaOne = exampleOne.createCriteria();
            dzpceggCriteriaOne.andOpenStatusEqualTo(Constants.STATUS_AUTO);
            exampleOne.setOffset(0);
            exampleOne.setLimit(100);
            exampleOne.setOrderByClause("ideal_time DESC");
            List<DzpceggLotterySg> dzpceggLotterySgsOne = dzpceggLotterySgMapper.selectByExample(exampleOne);
            RedisBusinessUtil.addDzpceggLotterySgList(dzpceggLotterySgsOne);
        }
        if ((pageNo - 1) * pageSize + pageSize <= 100) {     //从缓存中取
            dzpceggLotterySgs = RedisBusinessUtil.getDzpceggLotterySgList((pageNo - 1) * pageSize, pageNo * pageSize - 1);
        } else {  //从数据库中取
            dzpceggLotterySgs = dzpceggLotterySgMapper.selectByExample(example);
        }

//        List<DzpceggLotterySg> dzpceggLotterySgs = dzpceggLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = this.lishiDzpcdandSg(dzpceggLotterySgs);
        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return result;
    }

    private List<Map<String, Object>> lishiDzpcdandSg(List<DzpceggLotterySg> dzpceggLotterySgs) {
        if (dzpceggLotterySgs == null) {
            return null;
        }
        int totalIssue = dzpceggLotterySgs.size();
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < totalIssue; i++) {
            DzpceggLotterySg sg = dzpceggLotterySgs.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("issue", sg.getIssue());
            map.put(AppMianParamEnum.TIME.getParamEnName(), DateUtils.formatDate(sg.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

//            if(StringUtils.isEmpty(sg.getTime())){
//                map.put(AppMianParamEnum.TIME.getParamEnName(), sg.getIdealTime());
//            }else{
//                map.put(AppMianParamEnum.TIME.getParamEnName(), sg.getTime());
//            }

//            if(StringUtils.isEmpty(sg.getNumber())){
//                map.put(Constants.SGSIGN, 0);
//            }else{
//                map.put(Constants.SGSIGN, 1);
//                map.put("number", sg.getNumber());
//                map.put("numberstr", removeCommand(sg.getNumber()));
//            }

            Integer sum = PceggUtil.sumNumber(sg.getNumber());
            map.put("sum", sum);
            map.put("bigOrSmall", PceggUtil.checkBigOrSmall(sum));
            map.put("singleOrDouble", PceggUtil.checkSingleOrDouble(sum));
            map.put("number", sg.getNumber());
            map.put("numberstr", sg.getNumber());

            result.add(map);
        }
        return result;
    }



    private List<Map<String, Object>> lishiDzxyfeitSg(List<DzxyftLotterySg> dzxtftLotterySgs) {
        if (dzxtftLotterySgs == null) {
            return null;
        }
        int totalIssue = dzxtftLotterySgs.size();
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < totalIssue; i++) {
            DzxyftLotterySg sg = dzxtftLotterySgs.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("issue", sg.getIssue());
            map.put(AppMianParamEnum.TIME.getParamEnName(), DateUtils.formatDate(sg.getTime(), DateUtils.FORMAT_YYYY_MM_DD_HHMMSS));

//            if(StringUtils.isEmpty(sg.getTime())){
//                map.put(AppMianParamEnum.TIME.getParamEnName(), sg.getIdealTime());
//            }else{
//                map.put(AppMianParamEnum.TIME.getParamEnName(), sg.getTime());
//            }

//            if(StringUtils.isEmpty(sg.getNumber())){
//                map.put(Constants.SGSIGN, 0);
//            }else{
//                map.put(Constants.SGSIGN, 1);
//                map.put("number", sg.getNumber());
//                map.put("numberstr", removeCommand(sg.getNumber()));
//            }

            map.put("number", sg.getNumber());
            map.put("numberstr", sg.getNumber());

            result.add(map);
        }
        return result;
    }



}
