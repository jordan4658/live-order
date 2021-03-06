package com.caipiao.live.order.service.lottery.impl;


import com.caipiao.live.common.constant.Constants;
import com.caipiao.live.common.constant.LotteryInformationType;
import com.caipiao.live.common.constant.RedisKeys;
import com.caipiao.live.common.enums.AppMianParamEnum;
import com.caipiao.live.common.enums.CalculationEnum;
import com.caipiao.live.common.enums.StatusCode;
import com.caipiao.live.common.enums.lottery.CaipiaoRedisTimeEnum;
import com.caipiao.live.common.enums.lottery.CaipiaoTypeEnum;
import com.caipiao.live.common.model.common.ResultInfo;
import com.caipiao.live.common.model.dto.lottery.LotterySgModel;
import com.caipiao.live.common.model.dto.result.LhcSgDTO;
import com.caipiao.live.common.model.vo.LhcWsdxVO;
import com.caipiao.live.common.model.vo.MapListVO;
import com.caipiao.live.common.model.vo.MapStringVO;
import com.caipiao.live.common.model.vo.MapVO;
import com.caipiao.live.common.model.vo.lottery.LhcCountVO;
import com.caipiao.live.common.model.vo.lottery.LhcLotterySgVO;
import com.caipiao.live.common.model.vo.lottery.LhcLskjVO;
import com.caipiao.live.common.model.vo.lottery.LhcPhotoCategoryVO;
import com.caipiao.live.common.mybatis.entity.*;
import com.caipiao.live.common.mybatis.mapper.LhcHandicapMapper;
import com.caipiao.live.common.mybatis.mapper.LhcLotterySgMapper;
import com.caipiao.live.common.mybatis.mapper.LotteryPlaySettingMapper;
import com.caipiao.live.common.mybatis.mapperbean.LhcBeanMapper;
import com.caipiao.live.common.util.DateUtils;
import com.caipiao.live.common.util.DefaultResultUtil;
import com.caipiao.live.common.util.TimeHelper;
import com.caipiao.live.common.util.lottery.LhcSgUtils;
import com.caipiao.live.common.util.lottery.LhcUtils;
import com.caipiao.live.common.util.redis.BasicRedisClient;
import com.caipiao.live.common.util.stat.LHCStatUtil;
import com.caipiao.live.order.service.lottery.LhcLotterySgService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author lzy
 * @create 2018-07-20 14:46
 **/
@Service
public class LhcLotterySgServiceImpl implements LhcLotterySgService {

    private static final Logger logger = LoggerFactory.getLogger(LhcLotterySgServiceImpl.class);

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LhcBeanMapper lhcBeanMapper;

    @Autowired
    private LhcLotterySgMapper lhcLotterySgMapper;
    @Autowired
    private LhcHandicapMapper lhcHandicapMapper;
    @Autowired
    private LotteryPlaySettingMapper lotteryPlaySettingMapper;
    @Autowired
    private LhcLotterySgService lhcLotterySgRest;


    @Autowired
    private BasicRedisClient basicRedisClient;

    @Override
    public ResultInfo<List<MapVO>> lhcInformation(String type, Integer issue) {
        if (issue == null) {
            issue = 100;
        }
        // ?????????????????????1000???
        if (issue > 1000) {
            issue = 1000;
        }
        List<String> sg = this.lhcBeanMapper.getSg(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_BSTM_RT.equals(type)) {
            List<MapVO> list = LhcUtils.boseTemaRe(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_BSTM_LT.equals(type)) {
            List<MapVO> list = LhcUtils.boseTemaLen(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_BSZM_RT.equals(type)) {
            List<MapVO> list = LhcUtils.boseZhengmaRe(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_BSZM_LT.equals(type)) {
            List<MapVO> list = LhcUtils.boseZhengmaLen(sg);
            return ResultInfo.ok(list);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_TMLM.equals(type)) {
            List<MapVO> list = LhcUtils.temaLiangMian(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_TMWS_RT.equals(type)) {
            List<MapVO> list = LhcUtils.temaWeiRe(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_TMWS_LT.equals(type)) {
            List<MapVO> list = LhcUtils.temaWeiLen(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_ZMWS_RT.equals(type)) {
            List<MapVO> list = LhcUtils.zhengmaWeiRe(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_ZMWS_LT.equals(type)) {
            List<MapVO> list = LhcUtils.zhengmaWeiLen(sg);
            return ResultInfo.ok(list);
        }

        // ??????????????????????????????
        if (LotteryInformationType.LHC_ZMZF.equals(type)) {
            List<MapVO> list = LhcUtils.zhengmaZongFen(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_HMBD_ZM.equals(type)) {
            List<MapVO> list = LhcUtils.zhengmaBoDuan(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_HMBD_TM.equals(type)) {
            List<MapVO> list = LhcUtils.temaBoDuan(sg);
            return ResultInfo.ok(list);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<List<MapVO>>> lhcInfo(String type, Integer issue) {
        if (issue == null) {
            issue = 100;
        }
        // ?????????????????????1000???
        if (issue > 1000) {
            issue = 1000;
        }
        List<String> sg = this.lhcBeanMapper.getSg(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_TMLS.equals(type)) {
            List<List<MapVO>> lists = LhcUtils.temaLenReTu(sg);
            return ResultInfo.ok(lists);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_ZMLS.equals(type)) {
            List<List<MapVO>> lists = LhcUtils.zhengmaLenReTu(sg);
            return ResultInfo.ok(lists);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<List<String>>> lhcInfoC(String type, String year) {
        if (StringUtils.isBlank(year)) {
            year = TimeHelper.date("yyyy");
        }
        List<LotterySgModel> sg = this.lhcBeanMapper.getSgByYear(year);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }
        // ?????????????????????
        if (LotteryInformationType.LHC_WSDX.equals(type)) {
            List<List<String>> lhcVOs = LhcUtils.weishuDaXiao(sg);
            return ResultInfo.ok(lhcVOs);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_LMZS.equals(type)) {
            List<List<String>> lhcVOs = LhcUtils.lianMaZouShi(sg);
            return ResultInfo.ok(lhcVOs);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_LXZS.equals(type)) {
            List<List<String>> lhcVOs = LhcUtils.lianXiaoZouShi(sg);
            return ResultInfo.ok(lhcVOs);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_JQYS.equals(type)) {
            List<List<String>> lhcVOs = LhcUtils.jiaQinYeShou(sg);
            return ResultInfo.ok(lhcVOs);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_SXCX.equals(type)) {
            List<List<String>> lhcVOs = LhcUtils.shengxiaoChaXun(sg);
            return ResultInfo.ok(lhcVOs);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<MapVO>> lhcInfoD(String type, Integer issue) {
        if (issue == null) {
            issue = 100;
        }
        // ?????????????????????1000???
        if (issue > 1000) {
            issue = 1000;
        }

        List<LotterySgModel> sg = this.lhcBeanMapper.getSg2(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_SXTM_RT.equals(type)) {
            List<MapVO> list = LhcUtils.shengXiaoTemaRe(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_SXTM_LT.equals(type)) {
            List<MapVO> list = LhcUtils.shengXiaoTemaLeng(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_SXZM_RT.equals(type)) {
            List<MapVO> list = LhcUtils.shengXiaoZhengmaRe(sg);
            return ResultInfo.ok(list);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_SXZM_LT.equals(type)) {
            List<MapVO> list = LhcUtils.shengXiaoZhengmaLeng(sg);
            return ResultInfo.ok(list);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<MapListVO>> lhcInfoE(String type, Integer issue) {
        if (issue == null) {
            issue = Constants.DEFAULT_STAT_ISSUES;
        }
        // ?????????????????????1000???
        if (issue > Constants.MAX_STAT_ISSUES) {
            issue = Constants.MAX_STAT_ISSUES;
        }

        List<LotterySgModel> sg = this.lhcBeanMapper.getSg2(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_ZXTJ.equals(type)) {
//            List<MapListVO> lists = LhcUtils.ziXunTongJi(sg);
            List<MapListVO> lists = LHCStatUtil.statInfomations(sg);

            return ResultInfo.ok(lists);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<List<MapStringVO>>> lhcInfoF(String type, String year, Integer sort, Integer pageNo,
                                                        Integer pageSize) {
        // ????????????
        if (!LotteryInformationType.LHC_LSKJ.equals(type)) {
            return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
        }

        // ????????????????????????
        year = StringUtils.isBlank(year) ? TimeHelper.date("yyyy") : year;

        // ??????????????????
        pageNo = pageNo == null ? 1 : pageNo;
        pageSize = pageSize == null ? 10 : pageSize;

        LhcLotterySgExample example = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andYearEqualTo(year);
        // ??????????????????
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        // ????????????
        sort = sort == null ? 0 : sort;
        if (sort.equals(0)) {
            example.setOrderByClause("issue desc");
        } else {
            example.setOrderByClause("issue asc");
        }
        List<LhcLotterySg> lhcLotterySgs = lhcLotterySgMapper.selectByExample(example);

        // ?????????????????????
        List<List<MapStringVO>> voList = LhcUtils.lishiKaiJiang(lhcLotterySgs);

        return ResultInfo.ok(voList);
    }

    @Override
    public ResultInfo<List<Integer>> getAiNum(String date, String dateb) {
        List<Integer> list = LhcUtils.aiNum(date, dateb);
        return ResultInfo.ok(list);
    }

    @Override
    public ResultInfo<Map<String, Object>> lishiSg(Integer pageNo, Integer pageSize) {
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        LhcLotterySgExample example = new LhcLotterySgExample();
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        example.setOrderByClause("year DESC,issue DESC");
        List<LhcLotterySg> lhcLotterySgs = this.lhcLotterySgMapper.selectByExample(example);
        List<Map<String, Object>> maps = LhcUtils.lishiSg(lhcLotterySgs);
        Map<String, Object> result = new HashMap<>();
        result.put("list", maps);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);

        return ResultInfo.ok(result);
    }

    @Override
    public ResultInfo<Map<String, Object>> sgDetails(String date) {
        if (StringUtils.isBlank(date)) {
            return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
        }
        LhcLotterySgExample example = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andTimeEqualTo(date);
        LhcLotterySg lhcLotterySg = this.lhcLotterySgMapper.selectOneByExample(example);
        Map<String, Object> map = LhcUtils.sgDetails(lhcLotterySg);
        return ResultInfo.ok(map);
    }

    @Override
    public ResultInfo<Map<String, Object>> getNewestSgInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            // ????????????
            String redisKey = RedisKeys.LHC_RESULT_VALUE;
            Long redisTime = CaipiaoRedisTimeEnum.LHC.getRedisTime();
            result = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
            if (result == null) {
                result = new HashMap<>();
            }
            if (!CollectionUtils.isEmpty(result)) {
                // ???????????????????????????
                return ResultInfo.ok(result);
            }
            // ????????????????????????
            LhcLotterySgExample example = new LhcLotterySgExample();
            example.setOrderByClause("year DESC,issue DESC");
            LhcLotterySg lhcLotterySg = lhcLotterySgMapper.selectOneByExample(example);

            if (lhcLotterySg != null) {
                String issue = lhcLotterySg.getIssue();
                String number = lhcLotterySg.getNumber();
                result.put(AppMianParamEnum.TIME.getParamEnName(), lhcLotterySg.getTime());
                result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcLotterySg.getYear() + issue);
                result.put(AppMianParamEnum.NUMBER.getParamEnName(), LhcUtils.getNumberString(number));
                result.put(AppMianParamEnum.SHENGXIAO.getParamEnName(),
                        LhcUtils.getNumberZodiac(number, lhcLotterySg.getTime()));
            } else {
                result.put(AppMianParamEnum.ISSUE.getParamEnName(), Constants.DEFAULT_NULL);
                result.put(AppMianParamEnum.NUMBER.getParamEnName(), Constants.DEFAULT_NULL);
                result.put(AppMianParamEnum.SHENGXIAO.getParamEnName(), Constants.DEFAULT_NULL);
                result.put(AppMianParamEnum.TIME.getParamEnName(), Constants.DEFAULT_NULL);
            }
            LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
            LhcHandicapExample.Criteria criteria = lhcHandicapExample.createCriteria();
            String date = TimeHelper.date("yyyy-MM-dd HH:mm:ss");
            criteria.andStartTimeLessThanOrEqualTo(date);
            criteria.andStartlottoTimeGreaterThanOrEqualTo(date);
            criteria.andIsDeleteEqualTo(false);
            lhcHandicapExample.setOrderByClause("startlotto_time ASC");
            LhcHandicap lhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);

            if (lhcHandicap != null) {
                // ??????????????????
                result.put(AppMianParamEnum.NEXTISSUE.getParamEnName(), lhcHandicap.getIssue());
                // ???????????????????????????
                String startlottoTime = lhcHandicap.getStartlottoTime();
                result.put(AppMianParamEnum.NEXTOPENTIME.getParamEnName(), TimeHelper.str2time(startlottoTime, false));
                // ?????????????????????????????????
                String endTime = lhcHandicap.getEndTime();
                result.put(AppMianParamEnum.NEXTTIME.getParamEnName(), TimeHelper.str2time(endTime, false));
            } else {
                result.put(AppMianParamEnum.NEXTISSUE.getParamEnName(), Constants.DEFAULT_NULL);
                result.put(AppMianParamEnum.NEXTOPENTIME.getParamEnName(), Constants.DEFAULT_NULL);
                result.put(AppMianParamEnum.NEXTTIME.getParamEnName(), Constants.DEFAULT_NULL);
            }
            // ????????????
            redisTemplate.opsForValue().set(redisKey, result, redisTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("getNewestSgInfobyids:" + CaipiaoTypeEnum.LHC.getTagType() + " ????????? ", e);
            result = DefaultResultUtil.getNullResult();
        }
        return ResultInfo.ok(result);
    }

    @Override
    public ResultInfo<Map<String, Object>> getNewestSgInfoWeb() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "?????????");
        // ????????????????????????
        LhcLotterySgExample example = new LhcLotterySgExample();
        example.setOrderByClause("time DESC");
        LhcLotterySg lhcLotterySg = this.lhcLotterySgMapper.selectOneByExample(example);
        if (lhcLotterySg != null) {
            String issue = lhcLotterySg.getIssue();
            String number = lhcLotterySg.getNumber();
            String[] balls = number.split(",");
            ArrayList<String> numList = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                numList.add(balls[i]);
            }
            result.put("issue", lhcLotterySg.getYear() + issue);
            result.put("number", numList);
            result.put("shengxiao", LhcUtils.getNumberShengXiao(number, lhcLotterySg.getTime()));
            result.put("wuxing", LhcUtils.getNumberWuXing(number, lhcLotterySg.getTime()));
            result.put("time", lhcLotterySg.getTime());

        } else {
            result.put("issue", null);
            result.put("number", null);
            result.put("shengxiao", null);
            result.put("wuxing", null);
            result.put("time", null);
        }
        return ResultInfo.ok(result);
    }

    @Override
    public LhcLotterySgVO getNewestSg() {
        LhcLotterySgVO lhcLotterySgVO = new LhcLotterySgVO();

        // ????????????????????????
        LhcLotterySgExample example = new LhcLotterySgExample();
        example.setOrderByClause("time DESC,year DESC,issue DESC");
        LhcLotterySg lhcLotterySg = this.lhcLotterySgMapper.selectOneByExample(example);
        if (lhcLotterySg != null) {
            String issue = lhcLotterySg.getIssue();
            String number = lhcLotterySg.getNumber();
            lhcLotterySgVO.setIssue(lhcLotterySg.getYear() + issue);
            List<String> numberShengXiao = LhcUtils.getNumberShengXiao(number, lhcLotterySg.getTime());
            StringBuilder numSb = new StringBuilder(number);
            numSb.append("/");
            for (int i = 0; i < 7; i++) {
                numSb.append(numberShengXiao.get(i)).append(",");
            }
            lhcLotterySgVO.setNumber(numSb.substring(0, numSb.length() - 1));
        }
        return lhcLotterySgVO;
    }


    @Override
    public ResultInfo<Map<String, Object>> startlottoDate(String date) {
        if (StringUtils.isBlank(date)) {
            date = TimeHelper.date("yyyy-MM");
        }
        LhcHandicapExample example = new LhcHandicapExample();
        LhcHandicapExample.Criteria criteria = example.createCriteria();
        criteria.andStartlottoTimeLike(date + "%");
        example.setOrderByClause("startlotto_time ASC");
        List<LhcHandicap> lhcHandicaps = this.lhcHandicapMapper.selectByExample(example);
        List<String> dates = new ArrayList<>();
        if (lhcHandicaps != null && lhcHandicaps.size() > 0) {
            for (int i = 0; i < lhcHandicaps.size(); i++) {
                LhcHandicap lhcHandicap = lhcHandicaps.get(i);
                String startlottoTime = lhcHandicap.getStartlottoTime();
                dates.add(startlottoTime.substring(0, 10));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("lhcDate", dates);
        return ResultInfo.ok(result);
    }

    @Override
    public ResultInfo<Map<String, Object>> getNowIssueAndTime() {
        Map<String, Object> result = new HashMap<>();
        LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
        LhcHandicapExample.Criteria criteria = lhcHandicapExample.createCriteria();
        String date = TimeHelper.date("yyyy-MM-dd HH:mm:ss");
        criteria.andStartTimeLessThanOrEqualTo(date);
        criteria.andStartlottoTimeGreaterThanOrEqualTo(date);
        lhcHandicapExample.setOrderByClause("startlotto_time ASC");
        LhcHandicap lhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
        if (lhcHandicap != null) {
            // ???????????????
            result.put("issue", lhcHandicap.getIssue());
            // ????????????????????????
            String startlottoTime = lhcHandicap.getStartlottoTime();
            result.put("openTime", TimeHelper.str2time(startlottoTime, false));
            // ??????????????????????????????
            String endTime = lhcHandicap.getEndTime();
            result.put("betTime", TimeHelper.str2time(endTime, false));
        } else {
            result.put("issue", "");
            result.put("openTime", null);
            result.put("betTime", null);
        }

        return ResultInfo.ok(result);
    }

    @Override
    public ResultInfo<List<LhcLskjVO>> lhcWebLskj(String year) {
        if (StringUtils.isBlank(year)) {
            year = TimeHelper.date("yyyy");
        }
        LhcLotterySgExample example = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andYearEqualTo(year);

        example.setOrderByClause("issue DESC");
        List<LhcLotterySg> lhcLotterySgs = this.lhcLotterySgMapper.selectByExample(example);
        List<LhcLskjVO> voList = LhcUtils.lishiKaiJiangWeb(lhcLotterySgs);
        return ResultInfo.ok(voList);
    }

    @Override
    public ResultInfo<Map<String, List<MapVO>>> lhcWebInfoB(String type, Integer issue) {
        if (issue == null) {
            issue = 30;
        }
        // ?????????????????????1000???
        if (issue > 500) {
            issue = 500;
        }
        List<String> sg = this.lhcBeanMapper.getSg(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_TMLS.equals(type)) {
            Map<String, List<MapVO>> result = LhcUtils.temaLenReTuWeb(sg);
            return ResultInfo.ok(result);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_ZMLS.equals(type)) {
            Map<String, List<MapVO>> result = LhcUtils.zhengmaLenReTuWeb(sg);
            return ResultInfo.ok(result);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<Map<String, List<MapVO>>> lhcWebInformation(String type, Integer issue) {

        if (issue == null) {
            issue = 30;
        }
        // ?????????????????????500???
        if (issue > 500) {
            issue = 500;
        }
        List<String> sg = this.lhcBeanMapper.getSg(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_BSTM_RT.equals(type)) {
            List<MapVO> re = LhcUtils.boseTemaRe(sg);
            List<MapVO> len = LhcUtils.boseTemaLen(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("retu", re);
            map.put("lentu", len);
            return ResultInfo.ok(map);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_BSZM_RT.equals(type)) {
            List<MapVO> re = LhcUtils.boseZhengmaRe(sg);
            List<MapVO> len = LhcUtils.boseZhengmaLen(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("retu", re);
            map.put("lentu", len);
            return ResultInfo.ok(map);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_HMBD_TM.equals(type)) {
            List<MapVO> temaBoDuan = LhcUtils.temaBoDuan(sg);
            List<MapVO> zhengmaBoDuan = LhcUtils.zhengmaBoDuan(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("tema", temaBoDuan);
            map.put("zhengma", zhengmaBoDuan);
            return ResultInfo.ok(map);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_TMWS_RT.equals(type)) {
            List<MapVO> re = LhcUtils.temaWeiRe(sg);
            List<MapVO> len = LhcUtils.temaWeiLen(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("retu", re);
            map.put("lentu", len);
            return ResultInfo.ok(map);
        }

        // ???????????????????????????
        if (LotteryInformationType.LHC_ZMWS_RT.equals(type)) {
            List<MapVO> re = LhcUtils.zhengmaWeiRe(sg);
            List<MapVO> len = LhcUtils.zhengmaWeiLen(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("retu", re);
            map.put("lentu", len);
            return ResultInfo.ok(map);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<LhcCountVO>> lhcWebInfoH(String type, Integer issue) {
        if (issue == null) {
            issue = 30;
        }
        // ?????????????????????500???
        if (issue > 500) {
            issue = 500;
        }
        List<String> sg = this.lhcBeanMapper.getSg(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_TMLM.equals(type)) {
            List<LhcCountVO> result = LhcUtils.temaLiangMianWeb(sg);
            return ResultInfo.ok(result);
        }

        // ??????????????????????????????
        if (LotteryInformationType.LHC_ZMZF.equals(type)) {
            List<LhcCountVO> result = LhcUtils.zhengmaZongFenWeb(sg);
            return ResultInfo.ok(result);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<Map<String, List<MapVO>>> lhcWebInfoD(String type, Integer issue) {
        if (issue == null) {
            issue = 30;
        }
        // ?????????????????????500???
        if (issue > 500) {
            issue = 500;
        }

        List<LotterySgModel> sg = this.lhcBeanMapper.getSg2(issue);
        if (sg == null) {
            sg = new ArrayList<>(0);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_SXTM_RT.equals(type)) {
            List<MapVO> re = LhcUtils.shengXiaoTemaRe(sg);
            List<MapVO> len = LhcUtils.shengXiaoTemaLeng(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("retu", re);
            map.put("lentu", len);
            return ResultInfo.ok(map);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_SXZM_RT.equals(type)) {
            List<MapVO> re = LhcUtils.shengXiaoZhengmaRe(sg);
            List<MapVO> len = LhcUtils.shengXiaoZhengmaLeng(sg);
            Map<String, List<MapVO>> map = new HashMap<>();
            map.put("retu", re);
            map.put("lentu", len);
            return ResultInfo.ok(map);
        }

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public ResultInfo<List<LhcWsdxVO>> lhcWebInfoC(String type, String year) {
        if (StringUtils.isBlank(year)) {
            year = TimeHelper.date("yyyy");
        }
        LhcLotterySgExample example = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria criteria = example.createCriteria();
        criteria.andYearEqualTo(year);
        List<LhcLotterySg> lhcLotterySgs = lhcLotterySgMapper.selectByExample(example);
        if (lhcLotterySgs == null) {
            lhcLotterySgs = new ArrayList<>(0);
        }
        // ?????????????????????
        if (LotteryInformationType.LHC_WSDX.equals(type)) {
            List<LhcWsdxVO> result = LhcUtils.weishuDaXiaoWeb(lhcLotterySgs);
            return ResultInfo.ok(result);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_LMZS.equals(type)) {
            List<LhcWsdxVO> result = LhcUtils.lianMaZouShiWeb(lhcLotterySgs);
            return ResultInfo.ok(result);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_LXZS.equals(type)) {
            List<LhcWsdxVO> result = LhcUtils.lianXiaoZouShiWeb(lhcLotterySgs);
            return ResultInfo.ok(result);
        }

        // ?????????????????????
        if (LotteryInformationType.LHC_JQYS.equals(type)) {
            List<LhcWsdxVO> result = LhcUtils.jiaQinYeShouWeb(lhcLotterySgs);
            return ResultInfo.ok(result);
        }

        /*
         * //????????????????????? if (LotteryInformationType.LHC_SXCX.equals(type)) {
         * List<List<String>> lhcVOs = LhcUtils.shengxiaoChaXun(sg); return
         * ResultInfo.ok(lhcVOs); }
         */

        return ResultInfo.getInstance(null, StatusCode.PARAM_ERROR);
    }

    @Override
    public List<LhcSgDTO> queryHelper(String year, Integer sort, Integer pageNo, Integer pageSize) {
        List<LhcSgDTO> list = new ArrayList<>();
        LhcLotterySgExample example = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isBlank(year)) {
            year = DateUtils.formatDate(new Date(), "yyyy");
        }
        criteria.andYearEqualTo(year);
        if (sort > 0) {
            example.setOrderByClause("issue asc");
        } else {
            example.setOrderByClause("issue desc");
        }
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        List<LhcLotterySg> lhcLotterySgs = lhcLotterySgMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(lhcLotterySgs)) {
            return list;
        }
        LhcSgDTO dto;
        for (LhcLotterySg sg : lhcLotterySgs) {
            dto = new LhcSgDTO();
            dto.setIssue(sg.getYear() + sg.getIssue());
            dto.setYear(sg.getYear());
            dto.setDate(sg.getTime());
            String number = sg.getNumber();
            dto.setNumber(number);
            String[] num = number.split(",");
            int tema = Integer.parseInt(num[6]);
            dto.setSize(tema == 49 ? "???" : tema > 24 ? "???" : "???");
            dto.setSingle(tema == 49 ? "???" : tema % 2 == 0 ? "???" : "???");
            dto.setSum(Integer.valueOf(num[0]) + Integer.valueOf(num[1]) + Integer.valueOf(num[2])
                    + Integer.valueOf(num[3]) + Integer.valueOf(num[4]) + Integer.valueOf(num[5]) + tema);
            list.add(dto);
        }
        return list;
    }

    /**
     * web??????????????????????????????????????????????????????
     *
     * @return
     */
    @Override
    public ResultInfo<Map<String, Object>> getTeMaAndShenXiaoDistory(Integer issue) {
        Map<String, Object> result = new HashMap<>();
        // ???????????????????????????????????????
        ResultInfo<Map<String, List<MapVO>>> mapResultInfo1 = lhcLotterySgRest
                .lhcWebInfoB(LotteryInformationType.LHC_TMLS, issue);
        Map<String, List<MapVO>> data1 = mapResultInfo1.getData();
        for (Map.Entry<String, List<MapVO>> temaKeyValue : data1.entrySet()) {
            if (temaKeyValue.getKey() == "retu") {
                result.put("tema", temaKeyValue.getValue());
            }
        }

        // ???????????????????????????????????????
        ResultInfo<Map<String, List<MapVO>>> mapResultInfo = lhcLotterySgRest
                .lhcWebInfoD(LotteryInformationType.LHC_SXTM_RT, issue);
        Map<String, List<MapVO>> data = mapResultInfo.getData();
        for (Map.Entry<String, List<MapVO>> shengXiaoKeyValue : data.entrySet()) {
            if (shengXiaoKeyValue.getKey() == "retu") {
                result.put("shengxiaoTema", shengXiaoKeyValue.getValue());
            }
        }

        return ResultInfo.ok(result);
    }

    /**
     * ???????????????????????????
     *
     * @param pageNo
     * @param pageSize
     * @param year
     * @return
     */
    @Override
    public ResultInfo<Map<String, Object>> getTodayAndHistoryNews(Integer pageNo, Integer pageSize, String year) {
        if (StringUtils.isBlank(year)) {
            year = TimeHelper.date("yyyy");
        }
        Map<String, Object> result = new HashMap<>();

        LhcLotterySgExample example = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria criteria = example.createCriteria();
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        criteria.andYearEqualTo(year);
        example.setOrderByClause("issue ASC");
        List<LhcLotterySg> lhcLotterySgs = this.lhcLotterySgMapper.selectByExample(example);

        List<Object> list = new ArrayList<>();
        Map<String, Object> data = lhcLotterySgRest.getNowIssueAndTime().getData();
        for (Map.Entry<String, Object> map : data.entrySet()) {
            list.add(map.getValue());
        }
        String issue = list.get(0).toString();

        // ???????????????????????????
        LhcHandicapExample example1 = new LhcHandicapExample();
        LhcHandicapExample.Criteria criteria1 = example1.createCriteria();
        criteria1.andIssueGreaterThanOrEqualTo(issue);
        example.setLimit(1);
        example.setOrderByClause("`issue` ASC");
        LhcHandicap lhcHandicap = lhcHandicapMapper.selectOneByExample(example1);
        int number;
        try {
            number = DateUtils.dayForWeek(lhcHandicap.getStartlottoTime());
        } catch (Exception e) {
            throw new RuntimeException("???????????????????????????");
        }
        List<LhcLskjVO> voLists = LhcUtils.lishiKaiJiangWeb(lhcLotterySgs);
        result.put("LhcLskjVO", voLists);
        result.put("nextStartTime", lhcHandicap.getStartlottoTime());
        result.put("week", number);
        return ResultInfo.ok(result);
    }

    @Override
    public LhcHandicap selectNextHandicap(String issue) {
        LhcHandicapExample example = new LhcHandicapExample();
        LhcHandicapExample.Criteria criteria = example.createCriteria();
        criteria.andIssueGreaterThan(issue);
        example.setOrderByClause("`issue` ASC");
        return lhcHandicapMapper.selectOneByExample(example);
    }

    @Override
    public LotteryPlaySetting selectSetting(Integer palyId) {
        LotteryPlaySettingExample example = new LotteryPlaySettingExample();
        LotteryPlaySettingExample.Criteria criteria = example.createCriteria();
        criteria.andPlayIdEqualTo(palyId);
        return lotteryPlaySettingMapper.selectOneByExample(example);
    }

    @Override
    public List<LhcLotterySgVO> queryHistories(Integer pageNo, Integer pageSize) {
        List<LhcLotterySgVO> list = new ArrayList<>();

        // ????????????????????????
        LhcLotterySgExample example = new LhcLotterySgExample();
        example.setOrderByClause("year DESC,issue DESC");
        example.setOffset((pageNo - 1) * pageSize);
        example.setLimit(pageSize);
        List<LhcLotterySg> sgList = lhcLotterySgMapper.selectByExample(example);

        // ??????
        if (CollectionUtils.isEmpty(sgList)) {
            return list;
        }

        // ????????????
        LhcLotterySgVO vo;
        for (LhcLotterySg sg : sgList) {
            vo = new LhcLotterySgVO();
            String issue = sg.getIssue();
            String number = sg.getNumber();
            vo.setIssue(sg.getYear() + issue);
            vo.setDate(sg.getTime());
            vo.setNumber(number);
            // ????????????
            List<String> numberShengXiao = LhcUtils.getNumberShengXiao(number, sg.getTime());
            StringBuilder zodiac = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                zodiac.append(numberShengXiao.get(i));
                if (i < 6) {
                    zodiac.append(",");
                }
            }
            vo.setZodiac(zodiac.toString());
            list.add(vo);
        }
        return list;
    }



    @Override
    public Map<String, Object> getLhcKaijiangMessage(String message) {
        Map<String, Object> result = new HashMap<>();

        Object value = basicRedisClient.hGet(Constants.LHC_KAIJIANG_STATUS, "NEW");
        logger.info("message?????????" + value);
        if (value != null) {
            message = String.valueOf(value);
            logger.info("message??????1???" + message);
        }

        if (message == null) { // ?????????????????????????????????????????????????????????????????????
            LhcLotterySgExample example = new LhcLotterySgExample();
            LhcLotterySgExample.Criteria criteria = example.createCriteria();
            example.setOrderByClause("time DESC");
            LhcLotterySg lhcLotterySg = lhcLotterySgMapper.selectOneByExample(example);
            result.put("numbers", lhcLotterySg.getNumber());
            result.put("issueStatus", "0");
            result.put("issue", lhcLotterySg.getYear() + lhcLotterySg.getIssue());

            LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
            LhcHandicapExample.Criteria criteriaHand = lhcHandicapExample.createCriteria();
            criteriaHand.andIssueEqualTo(lhcLotterySg.getYear() + lhcLotterySg.getIssue());
            LhcHandicap lhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
            result.put("lotteryTime", lhcHandicap.getStartlottoTime());

            // ???????????????????????????
            lhcHandicapExample = new LhcHandicapExample();
            criteriaHand = lhcHandicapExample.createCriteria();
            criteriaHand.andIssueGreaterThan(lhcHandicap.getIssue());
            LhcHandicap nextLhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
            if (nextLhcHandicap != null) {
                result.put("nextLotteryTime", nextLhcHandicap.getStartlottoTime().replace("-", "/"));
            }
        } else {
            String array[] = message.split(",");
            if ("0".equals(array[1])) {
                LhcLotterySgExample example = new LhcLotterySgExample();
                LhcLotterySgExample.Criteria criteria = example.createCriteria();
                example.setOrderByClause("time DESC");
                LhcLotterySg lhcLotterySg = lhcLotterySgMapper.selectOneByExample(example);
                result.put("numbers", lhcLotterySg.getNumber());
                result.put("issueStatus", "0");
                result.put("issue", array[0]);

                LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
                LhcHandicapExample.Criteria criteriaHand = lhcHandicapExample.createCriteria();
                criteriaHand.andIssueEqualTo(lhcLotterySg.getYear() + lhcLotterySg.getIssue());
                LhcHandicap lhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
                result.put("lotteryTime", lhcHandicap.getStartlottoTime());

                // ???????????????????????????
                lhcHandicapExample = new LhcHandicapExample();
                criteriaHand = lhcHandicapExample.createCriteria();
                criteriaHand.andIssueGreaterThan(lhcHandicap.getIssue());
                LhcHandicap nextLhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
                if (nextLhcHandicap != null) {
                    result.put("nextLotteryTime", nextLhcHandicap.getStartlottoTime().replace("-", "/"));
                }
            } else {
                String numbers = String.valueOf(basicRedisClient.hGet(Constants.LHC_KAIJIANG_STATUS, "NEW_NUMBER"));
                result.put("numbers", numbers);
                result.put("issueStatus", "1");
                result.put("issue", array[0]);

                LhcHandicapExample lhcHandicapExample = new LhcHandicapExample();
                LhcHandicapExample.Criteria criteriaHand = lhcHandicapExample.createCriteria();
                criteriaHand.andIssueEqualTo(array[0]);
                LhcHandicap lhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
                result.put("lotteryTime", lhcHandicap.getStartlottoTime());

                // ???????????????????????????
                lhcHandicapExample = new LhcHandicapExample();
                criteriaHand = lhcHandicapExample.createCriteria();
                criteriaHand.andIssueGreaterThan(lhcHandicap.getIssue());
                LhcHandicap nextLhcHandicap = this.lhcHandicapMapper.selectOneByExample(lhcHandicapExample);
                if (nextLhcHandicap != null) {
                    result.put("nextLotteryTime", nextLhcHandicap.getStartlottoTime().replace("-", "/"));
                }
            }
        }

        return result;
    }

    /**
     * ????????????????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> specialPoultryOrBeast() {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();

        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ????????????
        Integer poultryCount = 0;
        // ?????????
        Integer beastCount = 0;
        // ????????????
        Integer heCount = 0;
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();
                    String number = LhcSgUtils.getPoultryOrBeast(lhcSg.getNumber(), CalculationEnum.LHCSIX.getCode());
                    // ???????????????
                    if (number.equals(Constants.BIGORSMALL_POULTRY)) {
                        poultryCount++;
                    } else if (number.equals(Constants.BIGORSMALL_BEAST)) {
                        beastCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SAME)) {
                        heCount++;
                    }

                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????????????????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);

                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("specialPoultryOrBeast error", e);
        }
        resultMap.put("poultry", poultryCount);
        resultMap.put("beast", beastCount);
        resultMap.put("and", heCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ????????? ??????1-6 ??????????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcSingleOrDouble(Integer type) {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();
        // ?????????
        Integer singleCount = 0;
        // ?????????
        Integer doubleCount = 0;
        // ?????????
        Integer heCount = 0;
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();
                    String number = LhcSgUtils.singleOrDouble(lhcSg.getNumber(), type);
                    // ???????????????
                    if (number.equals(Constants.BIGORSMALL_ODD_NUMBER)) {
                        singleCount++;
                    } else if (number.equals(Constants.BIGORSMALL_EVEN_NUMBER)) {
                        doubleCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SAME)) {
                        heCount++;
                    }
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);

                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcSingleOrDouble error", e);
        }

        resultMap.put("single", singleCount);
        resultMap.put("double", doubleCount);
        resultMap.put("and", heCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ???????????????????????? ??????1-6 ???????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcJoinSingleOrDouble(Integer type) {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();

        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ????????????
        Integer singleCount = 0;
        // ????????????
        Integer doubleCount = 0;
        // ?????????
        Integer heCount = 0;
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();
                    String number = LhcSgUtils.joinSingleOrDouble(lhcSg.getNumber(), type);
                    // ??????????????????
                    if (number.equals(Constants.BIGORSMALL_ODD_NUMBER)) {
                        singleCount++;
                    } else if (number.equals(Constants.BIGORSMALL_EVEN_NUMBER)) {
                        doubleCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SAME)) {
                        heCount++;
                    }
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ?????????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);

                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcJoinSingleOrDouble error", e);
        }
        resultMap.put("single", singleCount);
        resultMap.put("double", doubleCount);
        resultMap.put("and", heCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ??????????????????????????? ??????1-6 ???????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcTailBigOrSmall(Integer type) {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ????????????
        Integer tailBigCount = 0;
        // ???????????????
        Integer tailSmallCount = 0;
        // ?????????
        Integer heCount = 0;

        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();

                    String number = LhcSgUtils.tailBigOrSmall(lhcSg.getNumber(), type);
                    // ??????????????????
                    if (number.equals(Constants.BIGORSMALL_BIG)) {
                        tailBigCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SMALL)) {
                        tailSmallCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SAME)) {
                        heCount++;
                    }
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ?????????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);
                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcTailBigOrSmall error", e);
        }
        resultMap.put("big", tailBigCount);
        resultMap.put("small", tailSmallCount);
        resultMap.put("and", heCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ???????????????????????? ??????1-6 ????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcBigOrSmall(Integer type) {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ?????????
        Integer bigCount = 0;
        // ????????????
        Integer smallCount = 0;
        // ?????????
        Integer heCount = 0;
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();

                    String number = LhcSgUtils.getBigOrSmall(lhcSg.getNumber(), type);
                    // ???????????????
                    if (number.equals(Constants.BIGORSMALL_BIG)) {
                        bigCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SMALL)) {
                        smallCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SAME)) {
                        heCount++;
                    }
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);
                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcBigOrSmall error", e);
        }

        resultMap.put("big", bigCount);
        resultMap.put("small", smallCount);
        resultMap.put("and", heCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ??????????????? ??????1-6????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcWaveColor(Integer type) {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ?????????
        Integer redCount = 0;
        // ???????????????
        Integer greenCount = 0;
        // ?????????
        Integer blueCount = 0;
        // ?????????
        Integer heCount = 0;
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();

                    String number = LhcSgUtils.getNumBose(lhcSg.getNumber(), type);
                    // ???????????????
                    if (number.equals(Constants.BIGORSMALL_RED)) {
                        redCount++;
                    } else if (number.equals(Constants.BIGORSMALL_BLUE)) {
                        blueCount++;
                    } else if (number.equals(Constants.BIGORSMALL_GREEN)) {
                        greenCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SAME)) {
                        heCount++;
                    }
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);
                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcBigOrSmall error", e);
        }

        resultMap.put("red", redCount);
        resultMap.put("blue", greenCount);
        resultMap.put("green", blueCount);
        resultMap.put("and", heCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ???????????????????????? ??????1-6????????????
     */
    @Override
    public ResultInfo<List<Map<String, Object>>> lhcAttribute(Integer type) {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();

        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();
            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), LhcSgUtils.attribute(lhcSg.getNumber(), type));
                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcDragonOrTiger error", e);
        }
        return ResultInfo.ok(resultMaps);
    }

    /**
     * ??????????????????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcFiveElements() {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ?????????
        Integer goldCount = 0;
        // ?????????
        Integer woodCount = 0;
        // ?????????
        Integer waterCount = 0;
        // ?????????
        Integer fireCount = 0;
        // ?????????
        Integer soilCount = 0;
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();

                    String number = LhcSgUtils.getNumWuXing(lhcSg.getNumber(), CalculationEnum.LHCSIX.getCode());
                    // ???????????????
                    if (number.equals(Constants.BIGORSMALL_GOLD_TYPE)) {
                        goldCount++;
                    } else if (number.equals(Constants.BIGORSMALL_WOOD_TYPE)) {
                        woodCount++;
                    } else if (number.equals(Constants.BIGORSMALL_WATER_TYPE)) {
                        waterCount++;
                    } else if (number.equals(Constants.BIGORSMALL_FIRE_TYPE)) {
                        fireCount++;
                    } else if (number.equals(Constants.BIGORSMALL_SOIL_TYPE)) {
                        soilCount++;
                    }
                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);
                    resultMaps.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("lhcFiveElements error", e);
        }

        resultMap.put("gold", goldCount);
        resultMap.put("wood", woodCount);
        resultMap.put("water", waterCount);
        resultMap.put("fire", fireCount);
        resultMap.put("soil", soilCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public List<LhcLotterySg> getLhcHistoryData() {
        LhcLotterySgExample lhcExample = new LhcLotterySgExample();
        LhcLotterySgExample.Criteria lhcCriteria = lhcExample.createCriteria();
        Calendar date = Calendar.getInstance();
        String year = String.valueOf(date.get(Calendar.YEAR));
        lhcCriteria.andYearEqualTo(year);
        List<LhcLotterySg> ausactLotterySgList = lhcLotterySgMapper.selectByExample(lhcExample);
        return ausactLotterySgList;
    }

    /**
     * ???????????? ??????
     */
    @Override
    public ResultInfo<Map<String, Object>> lhcDragonOrTiger() {
        List<Map<String, Object>> resultMaps = new ArrayList<Map<String, Object>>();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        // ?????????
        Integer dragonCount = 0;
        // ?????????
        Integer tigerCount = 0;

        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();
            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                for (LhcLotterySg lhcSg : lhcLotterySgList) {
                    Map<String, Object> result = new HashMap<>();
                    String number = LhcSgUtils.dragonOrTiger(lhcSg.getNumber());

                    if (number.equals(Constants.PLAYRESULT_DRAGON)) {
                        dragonCount++;
                    } else if (number.equals(Constants.PLAYRESULT_TIGER)) {
                        tigerCount++;
                    }

                    result.put(AppMianParamEnum.ISSUE.getParamEnName(), lhcSg.getIssue());
                    // ??????
                    result.put(AppMianParamEnum.NUMBER.getParamEnName(), number);
                    resultMaps.add(result);
                }
            } else {
                Map<String, Object> result = new HashMap<>();
                result = DefaultResultUtil.getNullAlgorithmResult();
                resultMaps.add(result);
            }
        } catch (Exception e) {
            logger.error("lhcDragonOrTiger error", e);
        }
        resultMap.put("dragon", dragonCount);
        resultMap.put("tiger", tigerCount);
        resultMap.put("list", resultMaps);
        return ResultInfo.ok(resultMap);
    }

    /**
     * ???????????????1-6 ??????????????????
     */
    @Override
    public ResultInfo<List<Map<String, Integer>>> lhcOmitCensus(Integer type) {

        List<Map<String, Integer>> listMap = new ArrayList<Map<String, Integer>>();
        try {
            List<LhcLotterySg> lhcLotterySgList = this.getLhcHistoryData();

            Map<String, Integer> resultList = new HashMap<String, Integer>();
            if (!CollectionUtils.isEmpty(lhcLotterySgList)) {
                // ??????
                Map<String, Integer> bigList = commonBigOrSmall(lhcLotterySgList, type);
                if (!bigList.isEmpty()) {
                    resultList.putAll(resultList);
                }
                // ??????
                Map<String, Integer> singletList = commonSingleOrDouble(lhcLotterySgList, type);
                if (!singletList.isEmpty()) {
                    resultList.putAll(singletList);
                }
                // ?????????
                Map<String, Integer> joinList = commonJoinSingleOrDouble(lhcLotterySgList, type);
                if (!joinList.isEmpty()) {
                    resultList.putAll(joinList);
                }
                // ?????????
                Map<String, Integer> tailList = commonTailBigOrSmall(lhcLotterySgList, type);
                if (!tailList.isEmpty()) {
                    resultList.putAll(tailList);
                }
                // ??????
                Map<String, Integer> numberList = commonNumber(lhcLotterySgList, type);
                if (!numberList.isEmpty()) {
                    resultList.putAll(numberList);
                }

                Map<String, Integer> colourList = commonColour(lhcLotterySgList, type);
                if (!numberList.isEmpty()) {
                    resultList.putAll(colourList);
                }

                // ?????? ??????
                if (CalculationEnum.LHCSIX.getCode() == type) {
                    Map<String, Integer> poultryList = commonPoultryOrBeast(lhcLotterySgList, type);
                    if (!poultryList.isEmpty()) {
                        resultList.putAll(poultryList);
                    }
                    Map<String, Integer> fiveElementsList = commonFiveElements(lhcLotterySgList, type);
                    if (!fiveElementsList.isEmpty()) {
                        resultList.putAll(fiveElementsList);
                    }

                }

            }
            if (!resultList.isEmpty()) {
                // ????????????
                List<Map.Entry<String, Integer>> list = new ArrayList<>(resultList.entrySet());
                Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                });
                Map<String, Integer> querylist;
                for (Map.Entry s : list) {
                    querylist = new HashMap<String, Integer>();
                    querylist.put(s.getKey().toString(), Integer.parseInt(s.getValue().toString()));
                    listMap.add(querylist);
                }
            }
        } catch (NumberFormatException e) {
            logger.error("lhcOmitCensus error", e);
        }
        return ResultInfo.ok(listMap);

    }

    /**
     * ?????? ??????1-6 ???????????? ??????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonBigOrSmall(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        // ???
        Integer bigCount = 0;
        // ???
        Integer smallCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {

            String number = LhcSgUtils.getBigOrSmall(lhcSg.getNumber(), type);
            if (number.equals(Constants.BIGORSMALL_BIG)) {
                bigCount = 0;
                smallCount++;
            } else {
                smallCount = 0;
                bigCount++;
            }
        }

        result.put(Constants.BIGORSMALL_BIG, bigCount);
        result.put(Constants.BIGORSMALL_SMALL, smallCount);
        return result;
    }

    /**
     * ?????? ??????1-6 ??????????????????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonFiveElements(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        // ?????????
        Integer goldCount = 0;
        // ?????????
        Integer woodCount = 0;
        // ?????????
        Integer waterCount = 0;
        // ?????????
        Integer fireCount = 0;
        // ?????????
        Integer soilCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {

            String number = LhcSgUtils.getNumWuXing(lhcSg.getNumber(), CalculationEnum.LHCSIX.getCode());
            // ???????????????
            if (number.equals(Constants.BIGORSMALL_GOLD_TYPE)) {
                goldCount = 0;
                woodCount++;
                waterCount++;
                fireCount++;
                soilCount++;
            } else if (number.equals(Constants.BIGORSMALL_WOOD_TYPE)) {
                goldCount++;
                woodCount = 0;
                waterCount++;
                fireCount++;
                soilCount++;
            } else if (number.equals(Constants.BIGORSMALL_WATER_TYPE)) {
                goldCount++;
                woodCount++;
                waterCount = 0;
                fireCount++;
                soilCount++;
            } else if (number.equals(Constants.BIGORSMALL_FIRE_TYPE)) {
                goldCount++;
                woodCount++;
                waterCount++;
                fireCount = 0;
                soilCount++;
            } else if (number.equals(Constants.BIGORSMALL_SOIL_TYPE)) {
                goldCount++;
                woodCount++;
                waterCount++;
                fireCount++;
                soilCount = 0;
            }
        }

        result.put(Constants.BIGORSMALL_GOLD_TYPE, goldCount);
        result.put(Constants.BIGORSMALL_WOOD_TYPE, woodCount);
        result.put(Constants.BIGORSMALL_WATER_TYPE, waterCount);
        result.put(Constants.BIGORSMALL_FIRE_TYPE, fireCount);
        result.put(Constants.BIGORSMALL_SOIL_TYPE, soilCount);
        return result;
    }

    private Map<String, Integer> lhcAttribute(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        //
        Integer ratCount = 0;
        //
        Integer cattleCount = 0;
        //
        Integer tigerCount = 0;
        //
        Integer rabbitCount = 0;
        //
        Integer dragonCount = 0;
        //
        Integer snakeCount = 0;
        //
        Integer horseCount = 0;
        //
        Integer sheepCount = 0;
        //
        Integer monkeyCount = 0;
        //
        Integer chickenCount = 0;
        //
        Integer dogCount = 0;
        //
        Integer pigCount = 0;

        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.attribute(lhcSg.getNumber(), type);
            if (number.equals(Constants.LHC_ATTRIBUTE_RAT)) {
                ratCount = 0;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_CATTLE)) {
                ratCount++;
                cattleCount = 0;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_TIGER)) {
                ratCount++;
                cattleCount++;
                tigerCount = 0;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_RABBIT)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount = 0;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_DRAGON)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount = 0;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_SNAKE)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount = 0;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_HORSE)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount = 0;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_SHEEP)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount = 0;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_MONKEY)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount = 0;
                chickenCount++;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_CHICKEN)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount = 0;
                dogCount++;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_DOG)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount = 0;
                pigCount++;
            }
            if (number.equals(Constants.LHC_ATTRIBUTE_PIG)) {
                ratCount++;
                cattleCount++;
                tigerCount++;
                rabbitCount++;
                dragonCount++;
                snakeCount++;
                horseCount++;
                sheepCount++;
                monkeyCount++;
                chickenCount++;
                dogCount++;
                pigCount = 0;
            }
        }

        result.put(Constants.LHC_ATTRIBUTE_RAT, ratCount);
        result.put(Constants.LHC_ATTRIBUTE_CATTLE, cattleCount);
        result.put(Constants.LHC_ATTRIBUTE_TIGER, tigerCount);
        result.put(Constants.LHC_ATTRIBUTE_RABBIT, rabbitCount);
        result.put(Constants.LHC_ATTRIBUTE_DRAGON, dragonCount);
        result.put(Constants.LHC_ATTRIBUTE_SNAKE, snakeCount);
        result.put(Constants.LHC_ATTRIBUTE_HORSE, horseCount);
        result.put(Constants.LHC_ATTRIBUTE_SHEEP, horseCount);
        result.put(Constants.LHC_ATTRIBUTE_MONKEY, monkeyCount);
        result.put(Constants.LHC_ATTRIBUTE_CHICKEN, chickenCount);
        result.put(Constants.LHC_ATTRIBUTE_DOG, dogCount);
        result.put(Constants.LHC_ATTRIBUTE_PIG, pigCount);
        return result;
    }

    /**
     * ???????????? ??????1-6 ?????? ??????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonSingleOrDouble(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> resultMap = new HashMap<>();
        // ???
        Integer singleCount = 0;
        // ???
        Integer doubleCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.singleOrDouble(lhcSg.getNumber(), type);
            if (number.equals(Constants.BIGORSMALL_ODD_NUMBER)) {
                doubleCount++;
                singleCount = 0;
            } else {
                singleCount++;
                doubleCount = 0;
            }
        }
        resultMap.put(Constants.BIGORSMALL_ODD_NUMBER, singleCount);
        resultMap.put(Constants.BIGORSMALL_EVEN_NUMBER, doubleCount);
        return resultMap;
    }

    /**
     * ???????????? ??????1-6 ??? ?????? ??????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonJoinSingleOrDouble(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> resultMap = new HashMap<>();
        // ???
        Integer joinSingleCount = 0;
        // ???
        Integer joinDoubleCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.joinSingleOrDouble(lhcSg.getNumber(), type);
            if (number.equals(Constants.BIGORSMALL_ODD_NUMBER)) {
                joinDoubleCount++;
                joinSingleCount = 0;
            } else {
                joinSingleCount++;
                joinDoubleCount = 0;
            }
        }
        resultMap.put(Constants.JOIN_BIGORSMALL_ODD_NUMBER, joinSingleCount);
        resultMap.put(Constants.JOIN_BIGORSMALL_EVEN_NUMBER, joinDoubleCount);
        return resultMap;
    }

    /**
     * ??????????????????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonPoultryOrBeast(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> resultMap = new HashMap<>();
        // ??????
        Integer poultryCount = 0;
        // ??????
        Integer beastCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.getPoultryOrBeast(lhcSg.getNumber(), type);
            if (number.equals(Constants.BIGORSMALL_POULTRY)) {
                beastCount++;
                poultryCount = 0;
            } else {
                poultryCount++;
                beastCount = 0;
            }
        }
        resultMap.put(Constants.BIGORSMALL_POULTRY, poultryCount);
        resultMap.put(Constants.BIGORSMALL_BEAST, beastCount);
        return resultMap;
    }

    /**
     * ???????????? ??? 1-6 ????????? ??????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonTailBigOrSmall(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> resultMap = new HashMap<>();
        // ???
        Integer tailBigCount = 0;
        // ???
        Integer tailSmallCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.tailBigOrSmall(lhcSg.getNumber(), type);
            if (number.equals(Constants.BIGORSMALL_BIG)) {
                tailSmallCount++;
                tailBigCount = 0;
            } else {
                tailBigCount++;
                tailSmallCount = 0;
            }
        }
        resultMap.put(Constants.TAIL_BIGORSMALL_BIG, tailBigCount);
        resultMap.put(Constants.TAIL_BIGORSMALL_SMALL, tailSmallCount);
        return resultMap;
    }

    /**
     * ???????????? ??? 1-6 ?????? ??????
     *
     * @param lhcLotterySgList
     * @param type
     * @return
     */
    private Map<String, Integer> commonColour(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> resultMap = new HashMap<>();
        // ???
        Integer redCount = 0;
        // ???
        Integer greenCount = 0;
        // ???
        Integer blueCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.tailBigOrSmall(lhcSg.getNumber(), type);
            if (number.equals(Constants.BIGORSMALL_RED)) {
                greenCount++;
                blueCount++;
                redCount = 0;
            }
            if (number.equals(Constants.BIGORSMALL_GREEN)) {
                greenCount = 0;
                blueCount++;
                redCount++;
            } else {
                greenCount++;
                blueCount = 0;
                redCount++;
            }
        }
        resultMap.put(Constants.BIGORSMALL_RED, redCount);
        resultMap.put(Constants.BIGORSMALL_GREEN, greenCount);
        resultMap.put(Constants.BIGORSMALL_BLUE, blueCount);
        return resultMap;
    }

    private Map<String, Integer> commonNumber(List<LhcLotterySg> lhcLotterySgList, Integer type) {
        Map<String, Integer> resultMap = new HashMap<>();
        int one = 0;
        int two = 0;
        int three = 0;
        int four = 0;
        int five = 0;
        int six = 0;
        int seven = 0;
        int eight = 0;
        int nineCount = 0;
        int tenCount = 0;
        int elevenCount = 0;
        int twelveCount = 0;
        int thirteenCount = 0;
        int fourteenCount = 0;
        int fifteenCount = 0;
        int sixteenCount = 0;
        int seventeenCount = 0;
        int eighteenCount = 0;
        int nineteenCount = 0;
        int twentyCount = 0;
        int twentyOneCount = 0;
        int twentyTwoCount = 0;
        int twentyThreeCount = 0;
        int twentyFourCount = 0;
        int twentyFiveCount = 0;
        int twentySixCount = 0;
        int twentySevenCount = 0;
        int twentyEightCount = 0;
        int twentyNineCount = 0;
        int thirtyCount = 0;
        int thirtyOneCount = 0;
        int thirtyTwoCount = 0;
        int thirtyThreeCount = 0;
        int thirtyFourCount = 0;
        int thirtyFiveCount = 0;
        int thirtySixCount = 0;
        int thirtySevenCount = 0;
        int thirtyEightCount = 0;
        int thirtyNineCount = 0;
        int fortyCount = 0;
        int fortyOneCount = 0;
        int fortyTwoCount = 0;
        int fortyThreeCount = 0;
        int fortyFourCount = 0;
        int fortyFiveCount = 0;
        int fortySixCount = 0;
        int fortySevenCount = 0;
        int fortyEightCount = 0;
        int fortyNineCount = 0;
        for (LhcLotterySg lhcSg : lhcLotterySgList) {
            String number = LhcSgUtils.commonNumber(lhcSg.getNumber(), type);
            switch (number) {
                case Constants.BIGORSMALL_ONE:
                    one = 0;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWO:
                    one++;
                    two = 0;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THREE:
                    one++;
                    two++;
                    three = 0;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FOUR:
                    one++;
                    two++;
                    three++;
                    four = 0;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FIVE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five = 0;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_SIX:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six = 0;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_SEVEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven = 0;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_EIGHT:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight = 0;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_NINE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount = 0;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount = 0;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_ELEVEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount = 0;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWELVE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount = 0;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount = 0;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FOURTEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount = 0;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FIFTEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount = 0;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_SIXTEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount = 0;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_SEVENTEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount = 0;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_EIGHTEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount = 0;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_NINETEEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount = 0;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount = 0;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_ONE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount = 0;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_TWO:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount = 0;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_THREE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount = 0;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_FOUR:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount = 0;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_FIVE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount = 0;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_SIX:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount = 0;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_SEVEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount = 0;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_EIGHT:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount = 0;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_TWENTY_NINE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount = 0;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount = 0;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_ONE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount = 0;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_TWO:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount = 0;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_THREE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount = 0;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_FOUR:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount = 0;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_FIVE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount = 0;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_SIX:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount = 0;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_SEVEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount = 0;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_EIGHT:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount = 0;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_THIRTY_NINE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount = 0;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount = 0;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_ONE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount = 0;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_TWO:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount = 0;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_THREE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount = 0;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_FOUR:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount = 0;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_FIVE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount = 0;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_SIX:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount = 0;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_SEVEN:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount = 0;
                    fortyEightCount++;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_EIGHT:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount = 0;
                    fortyNineCount++;
                    break;
                case Constants.BIGORSMALL_FORTY_NINE:
                    one++;
                    two++;
                    three++;
                    four++;
                    five++;
                    six++;
                    seven++;
                    eight++;
                    nineCount++;
                    tenCount++;
                    elevenCount++;
                    twelveCount++;
                    thirteenCount++;
                    fourteenCount++;
                    fifteenCount++;
                    sixteenCount++;
                    seventeenCount++;
                    eighteenCount++;
                    nineteenCount++;
                    twentyCount++;
                    twentyOneCount++;
                    twentyTwoCount++;
                    twentyThreeCount++;
                    twentyFourCount++;
                    twentyFiveCount++;
                    twentySixCount++;
                    twentySevenCount++;
                    twentyEightCount++;
                    twentyNineCount++;
                    thirtyCount++;
                    thirtyOneCount++;
                    thirtyTwoCount++;
                    thirtyThreeCount++;
                    thirtyFourCount++;
                    thirtyFiveCount++;
                    thirtySixCount++;
                    thirtySevenCount++;
                    thirtyEightCount++;
                    thirtyNineCount++;
                    fortyCount++;
                    fortyOneCount++;
                    fortyTwoCount++;
                    fortyThreeCount++;
                    fortyFourCount++;
                    fortyFiveCount++;
                    fortySixCount++;
                    fortySevenCount++;
                    fortyEightCount++;
                    fortyNineCount = 0;
                    break;
                default:
                    break;
            }

        }
        resultMap.put(Constants.BIGORSMALL_ONE, one);
        resultMap.put(Constants.BIGORSMALL_TWO, two);
        resultMap.put(Constants.BIGORSMALL_THREE, three);
        resultMap.put(Constants.BIGORSMALL_FOUR, four);
        resultMap.put(Constants.BIGORSMALL_FIVE, five);
        resultMap.put(Constants.BIGORSMALL_SIX, six);
        resultMap.put(Constants.BIGORSMALL_SEVEN, seven);
        resultMap.put(Constants.BIGORSMALL_EIGHT, eight);
        resultMap.put(Constants.BIGORSMALL_NINE, nineCount);
        resultMap.put(Constants.BIGORSMALL_TEN, tenCount);
        resultMap.put(Constants.BIGORSMALL_ELEVEN, elevenCount);
        resultMap.put(Constants.BIGORSMALL_TWELVE, twelveCount);
        resultMap.put(Constants.BIGORSMALL_THIRTEEN, thirteenCount);
        resultMap.put(Constants.BIGORSMALL_FOURTEEN, fourteenCount);
        resultMap.put(Constants.BIGORSMALL_FIFTEEN, fifteenCount);
        resultMap.put(Constants.BIGORSMALL_SIXTEEN, sixteenCount);
        resultMap.put(Constants.BIGORSMALL_SEVENTEEN, seventeenCount);
        resultMap.put(Constants.BIGORSMALL_EIGHTEEN, eighteenCount);
        resultMap.put(Constants.BIGORSMALL_NINETEEN, nineteenCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY, twentyCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_ONE, twentyOneCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_TWO, twentyTwoCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_THREE, twentyThreeCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_FOUR, twentyFourCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_FIVE, twentyFiveCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_SIX, twentySixCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_SEVEN, twentySevenCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_EIGHT, twentyEightCount);
        resultMap.put(Constants.BIGORSMALL_TWENTY_NINE, twentyNineCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY, thirtyCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_ONE, thirtyOneCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_TWO, thirtyTwoCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_THREE, thirtyThreeCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_FOUR, thirtyFourCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_FIVE, thirtyFiveCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_SIX, thirtySixCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_SEVEN, thirtySevenCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_EIGHT, thirtyEightCount);
        resultMap.put(Constants.BIGORSMALL_THIRTY_NINE, thirtyNineCount);
        resultMap.put(Constants.BIGORSMALL_FORTY, fortyCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_ONE, fortyOneCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_TWO, fortyTwoCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_THREE, fortyThreeCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_FOUR, fortyFourCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_FIVE, fortyFiveCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_SIX, fortySixCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_SEVEN, fortySevenCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_EIGHT, fortyEightCount);
        resultMap.put(Constants.BIGORSMALL_FORTY_NINE, fortyNineCount);
        return resultMap;
    }

}