package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.NotifyConstants;
import com.zbkj.common.constants.UserConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.combination.StorePink;
import com.zbkj.common.model.express.Express;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.sms.SmsTemplate;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.system.SystemNotification;
import com.zbkj.common.model.system.SystemStore;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.user.UserBrokerageRecord;
import com.zbkj.common.model.user.UserToken;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.*;
import com.zbkj.common.response.*;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.ValidateFormUtil;
import com.zbkj.common.vo.*;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.delete.OrderUtils;
import com.zbkj.service.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StoreOrderServiceImpl ????????????
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB???????????????????????????????????? ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2020 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB????????????????????????????????????????????????CRMEB????????????
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Service
public class StoreOrderServiceImpl extends ServiceImpl<StoreOrderDao, StoreOrder> implements StoreOrderService {

    @Resource
    private StoreOrderDao dao;

    @Autowired
    private SystemStoreService systemStoreService;

    @Autowired
    private StoreOrderInfoService StoreOrderInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserBillService userBillService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private StoreOrderRefundService storeOrderRefundService;

    @Autowired
    private ExpressService expressService;

    @Autowired
    private TemplateMessageService templateMessageService;

    @Autowired
    private LogisticService logisticService;

    @Autowired
    private OrderUtils orderUtils;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private StorePinkService storePinkService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OnePassService onePassService;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private UserBrokerageRecordService userBrokerageRecordService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private SmsTemplateService smsTemplateService;

    /**
    * ??????
    * @param request ????????????
    * @param pageParamRequest ???????????????
    * @return CommonPage<StoreOrderDetailResponse>
    */
    @Override
    public CommonPage<StoreOrderDetailResponse> getAdminList(StoreOrderSearchRequest request, PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "order_id", "uid", "real_name", "pay_price", "pay_type", "create_time", "status", "refund_status"
                , "refund_reason_wap_img", "refund_reason_wap_explain", "refund_reason_wap", "refund_reason", "refund_reason_time"
                , "is_del", "combination_id", "pink_id", "seckill_id", "bargain_id", "verify_code", "remark", "paid", "is_system_del", "shipping_type", "type", "is_alter_price");
        if (StrUtil.isNotBlank(request.getOrderNo())) {
            queryWrapper.eq("order_id", request.getOrderNo());
        }
        getRequestTimeWhere(queryWrapper, request);
        getStatusWhere(queryWrapper, request.getStatus());
        if (!request.getType().equals(2)) {
            queryWrapper.eq("type", request.getType());
        }
        queryWrapper.orderByDesc("id");
        List<StoreOrder> orderList = dao.selectList(queryWrapper);
        List<StoreOrderDetailResponse> detailResponseList = new ArrayList<>();
        if (CollUtil.isNotEmpty(orderList)) {
            detailResponseList = formatOrder1(orderList);
        }
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, detailResponseList));
    }


    /**
     * H5????????????
     * @param uid ??????uid
     * @param status ????????????|0=?????????,1=?????????,2=?????????,3=?????????,4=?????????,-3=??????/??????
     * @param pageParamRequest ????????????
     * @return ??????????????????
     */
    @Override
    public List<StoreOrder> getUserOrderList(Integer uid, Integer status, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        orderUtils.statusApiByWhere(lqw, status);
        lqw.eq(StoreOrder::getUid, uid);
        lqw.orderByDesc(StoreOrder::getId);
        return dao.selectList(lqw);
    }

    /**
     * ????????????
     * @param storeOrder ????????????
     * @return ????????????
     */
    @Override
    public boolean create(StoreOrder storeOrder) {
        return dao.insert(storeOrder) > 0;
    }

    /**
     * ????????????????????????
     * @param storeOrder ??????
     * @return ????????????
     */
    @Override
    public StoreOrder getByEntityOne(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(storeOrder);
        return dao.selectOne(lqw);
    }

    /**
     * ????????????
     * @param request ????????????
     * @param pageParamRequest ???????????????
     * @return List<StoreOrder>
     */
    @Override
    public SystemWriteOffOrderResponse getWriteOffList(SystemWriteOffOrderSearchRequest request, PageParamRequest pageParamRequest) {
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        String where = " is_del = 0 and shipping_type = 2";
        //??????
        if (!StringUtils.isBlank(request.getDateLimit())) {
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            where += " and (create_time between '" + dateLimit.getStartTime() + "' and '" + dateLimit.getEndTime() + "' )";
        }

        if (!StringUtils.isBlank(request.getKeywords())) {
            where += " and (real_name like '%"+ request.getKeywords() +"%' or user_phone = '"+ request.getKeywords() +"' or order_id = '" + request.getKeywords() + "' or id = '" + request.getKeywords() + "' )";
        }

        if (request.getStoreId() != null && request.getStoreId() > 0) {
            where += " and store_id = " + request.getStoreId();
        }

        SystemWriteOffOrderResponse systemWriteOffOrderResponse = new SystemWriteOffOrderResponse();
        BigDecimal totalPrice = dao.getTotalPrice(where);
        if (ObjectUtil.isNull(totalPrice)) {
            totalPrice = BigDecimal.ZERO;
        }
        systemWriteOffOrderResponse.setOrderTotalPrice(totalPrice);   //???????????????

        BigDecimal refundPrice = dao.getRefundPrice(where);
        if (ObjectUtil.isNull(refundPrice)) {
            refundPrice = BigDecimal.ZERO;
        }
        systemWriteOffOrderResponse.setRefundTotalPrice(refundPrice); //???????????????
        systemWriteOffOrderResponse.setRefundTotal(dao.getRefundTotal(where));  //???????????????

        Page<StoreOrder> storeOrderPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        lambdaQueryWrapper.apply(where);
        lambdaQueryWrapper.orderByDesc(StoreOrder::getId);
        List<StoreOrder> storeOrderList = dao.selectList(lambdaQueryWrapper);

        if (storeOrderList.size() < 1) {
            systemWriteOffOrderResponse.setList(CommonPage.restPage(new PageInfo<>()));
            return systemWriteOffOrderResponse;
        }

        List<StoreOrderItemResponse> storeOrderItemResponseArrayList = formatOrder(storeOrderList);

        systemWriteOffOrderResponse.setTotal(storeOrderPage.getTotal()); //?????????
        systemWriteOffOrderResponse.setList(CommonPage.restPage(CommonPage.copyPageInfo(storeOrderPage, storeOrderItemResponseArrayList)));

        return systemWriteOffOrderResponse;
    }

    /**
     * ??????????????????????????????????????????
     * @param orderList List<StoreOrder> ????????????
     * @return List<StoreOrderItemResponse>
     */
    private List<StoreOrderDetailResponse> formatOrder1(List<StoreOrder> orderList) {
        List<StoreOrderDetailResponse> detailResponseList  = new ArrayList<>();
        if (CollUtil.isEmpty(orderList)) {
            return detailResponseList;
        }

        //??????id??????
        List<Integer> orderIdList = orderList.stream().map(StoreOrder::getId).distinct().collect(Collectors.toList());

        //??????????????????map
        HashMap<Integer, List<StoreOrderInfoOldVo>> orderInfoList = StoreOrderInfoService.getMapInId(orderIdList);
//
//        //????????????????????????
//        List<Integer> userIdList = orderList.stream().map(StoreOrder::getUid).distinct().collect(Collectors.toList());
//        //??????????????????
//        HashMap<Integer, User> userList = userService.getMapListInUid(userIdList);

        for (StoreOrder storeOrder : orderList) {
            StoreOrderDetailResponse storeOrderItemResponse = new StoreOrderDetailResponse();
            BeanUtils.copyProperties(storeOrder, storeOrderItemResponse);

            storeOrderItemResponse.setProductList(orderInfoList.get(storeOrder.getId()));

            //????????????
            storeOrderItemResponse.setStatusStr(getStatus(storeOrder));
            storeOrderItemResponse.setStatus(storeOrder.getStatus());
            //????????????
            storeOrderItemResponse.setPayTypeStr(getPayType(storeOrder.getPayType()));

            // ????????????????????????
            storeOrderItemResponse.setOrderType(getOrderTypeStr(storeOrder));
            detailResponseList.add(storeOrderItemResponse);
        }
        return detailResponseList;
    }

    /**
     * ????????????????????????????????????
     * @param storeOrder ??????
     * @return String
     */
    private String getOrderTypeStr(StoreOrder storeOrder) {
        String orderTypeFormat = "[{}??????]{}";
        String orderType = StrUtil.format(orderTypeFormat, "??????", "");
        // ??????
        if (StrUtil.isNotBlank(storeOrder.getVerifyCode())) {
            orderType = StrUtil.format(orderTypeFormat, "??????", "");
        }
        // ??????
        if (ObjectUtil.isNotNull(storeOrder.getSeckillId()) && storeOrder.getSeckillId() > 0) {
            orderType = StrUtil.format(orderTypeFormat, "??????", "");
        }
        // ??????
        if (ObjectUtil.isNotNull(storeOrder.getBargainId()) && storeOrder.getBargainId() > 0) {
            orderType = StrUtil.format(orderTypeFormat, "??????", "");
        }
        // ??????
        if (ObjectUtil.isNotNull(storeOrder.getCombinationId()) && storeOrder.getCombinationId() > 0) {
            StorePink storePink = storePinkService.getById(storeOrder.getPinkId());
            if (ObjectUtil.isNotNull(storePink)) {
                String pinkstatus = "";
                if (storePink.getStatus() == 2) {
                    pinkstatus = "?????????";
                } else if (storePink.getStatus() == 3) {
                    pinkstatus = "?????????";
                } else {
                    pinkstatus = "???????????????";
                }
                orderType = StrUtil.format(orderTypeFormat, "??????", pinkstatus);
            }
        }
        if (storeOrder.getType().equals(1)) {// ????????????
            orderType = StrUtil.format(orderTypeFormat, "?????????", "");
        }
        return orderType;
    }

    /**
     * ??????????????????????????????????????????
     * @param storeOrderList List<StoreOrder> ????????????
     * @author Mr.Zhang
     * @since 2020-05-28
     * @return List<StoreOrderItemResponse>
     */
    private List<StoreOrderItemResponse> formatOrder(List<StoreOrder> storeOrderList) {
        List<StoreOrderItemResponse> storeOrderItemResponseArrayList  = new ArrayList<>();
        if (null == storeOrderList || storeOrderList.size() < 1) {
            return storeOrderItemResponseArrayList;
        }
        //??????id
        List<Integer> storeIdList = storeOrderList.stream().map(StoreOrder::getStoreId).distinct().collect(Collectors.toList());
        //??????id / ?????????id
        List<Integer> clerkIdList = storeOrderList.stream().map(StoreOrder::getClerkId).distinct().collect(Collectors.toList());

        //??????id??????
        List<Integer> orderIdList = storeOrderList.stream().map(StoreOrder::getId).distinct().collect(Collectors.toList());

        //????????????map
        HashMap<Integer, SystemStore> systemStoreList = systemStoreService.getMapInId(storeIdList);
        //????????????map
//        HashMap<Integer, SystemStoreStaff> systemStoreStaffList = systemStoreStaffService.getMapInId(clerkIdList);
        HashMap<Integer, SystemAdmin> systemStoreStaffList = systemAdminService.getMapInId(clerkIdList);
        //??????????????????map
        HashMap<Integer, List<StoreOrderInfoOldVo>> orderInfoList = StoreOrderInfoService.getMapInId(orderIdList);

        //????????????????????????
        List<Integer> userIdList = storeOrderList.stream().map(StoreOrder::getUid).distinct().collect(Collectors.toList());
        //??????????????????
        HashMap<Integer, User> userList = userService.getMapListInUid(userIdList);

        //???????????????id??????
        List<Integer> spreadPeopleUidList = new ArrayList<>();
        for(Map.Entry<Integer, User> entry : userList.entrySet()) {
            spreadPeopleUidList.add(entry.getValue().getSpreadUid());
        }

        //????????????
        HashMap<Integer, User> mapListInUid = new HashMap<>();
        if (userIdList.size() > 0 && spreadPeopleUidList.size() > 0) {
            //???????????????
            mapListInUid = userService.getMapListInUid(spreadPeopleUidList);
        }

        for (StoreOrder storeOrder : storeOrderList) {
            StoreOrderItemResponse storeOrderItemResponse = new StoreOrderItemResponse();
            BeanUtils.copyProperties(storeOrder, storeOrderItemResponse);
            String storeName = "";
            if (systemStoreList.containsKey(storeOrder.getStoreId())) {
                storeName = systemStoreList.get(storeOrder.getStoreId()).getName();
            }
            storeOrderItemResponse.setStoreName(storeName);

            // ?????????????????????
            String clerkName = "";
            if (systemStoreStaffList.containsKey(storeOrder.getClerkId())) {
                clerkName = systemStoreStaffList.get(storeOrder.getClerkId()).getRealName();
            }
            storeOrderItemResponse.setProductList(orderInfoList.get(storeOrder.getId()));
            storeOrderItemResponse.setTotalNum(storeOrder.getTotalNum());

            //????????????
            storeOrderItemResponse.setStatusStr(getStatus(storeOrder));
            storeOrderItemResponse.setStatus(storeOrder.getStatus());
            //????????????
            storeOrderItemResponse.setPayTypeStr(getPayType(storeOrder.getPayType()));

            //???????????????
            if (!userList.isEmpty()  && null != userList.get(storeOrder.getUid()) && mapListInUid.containsKey(userList.get(storeOrder.getUid()).getSpreadUid())) {
                storeOrderItemResponse.getSpreadInfo().setId(mapListInUid.get(userList.get(storeOrder.getUid()).getSpreadUid()).getUid());
                storeOrderItemResponse.getSpreadInfo().setName(mapListInUid.get(userList.get(storeOrder.getUid()).getSpreadUid()).getNickname());
            }
            storeOrderItemResponse.setRefundStatus(storeOrder.getRefundStatus());

            storeOrderItemResponse.setClerkName(clerkName);

            // ????????????????????????
            String orderTypeFormat = "[{}??????]{}";
            String orderType = "";
            // ??????
            if (StrUtil.isNotBlank(storeOrder.getVerifyCode())) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getSeckillId()) && storeOrder.getSeckillId() > 0) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getBargainId()) && storeOrder.getBargainId() > 0) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getPinkId()) && storeOrder.getPinkId() > 0) {
                StorePink storePink = storePinkService.getById(storeOrder.getPinkId());
                if (ObjectUtil.isNotNull(storePink)) {
                    String pinkstatus = "";
                    if (storePink.getStatus() == 2) {
                        pinkstatus = "?????????";
                    } else if (storePink.getStatus() == 3) {
                        pinkstatus = "?????????";
                    } else {
                        pinkstatus = "???????????????";
                    }
                    orderType = StrUtil.format(orderTypeFormat, "??????", pinkstatus);
                }
            }
            if (StrUtil.isBlank(orderType)) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            storeOrderItemResponse.setOrderType(orderType);
            storeOrderItemResponseArrayList.add(storeOrderItemResponse);
        }
        return storeOrderItemResponseArrayList;
    }

    /**
     * ????????????
     * @param userId Integer ??????id
     * @author Mr.Zhang
     * @since 2020-06-10
     * @return UserBalanceResponse
     */
    @Override
    public BigDecimal getSumBigDecimal(Integer userId, String date) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price").
                eq("paid", 1).
                eq("is_del", 0);
        if (null != userId) {
            queryWrapper.eq("uid", userId);
        }
        if (null != date) {
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        StoreOrder storeOrder = dao.selectOne(queryWrapper);
        if (null == storeOrder || null == storeOrder.getPayPrice()) {
            return BigDecimal.ZERO;
        }
        return storeOrder.getPayPrice();
    }

    /**
     * ?????????????????????????????????
     * @param date String ????????????
     * @param lefTime int ????????????????????????
     * @author Mr.Zhang
     * @since 2020-05-16
     * @return HashMap<String, Object>
     */
    public List<StoreOrder> getOrderGroupByDate(String date, int lefTime) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price", "left(create_time, "+lefTime+") as orderId", "count(id) as id");
        if (StringUtils.isNotBlank(date)) {
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        queryWrapper.groupBy("orderId").orderByAsc("orderId");
        return dao.selectList(queryWrapper);
    }

    /** ??????
     * @param request StoreOrderRefundRequest ????????????
     * @return boolean
     * ???????????????????????????
     * ?????????????????????????????????????????????
     * ??????????????????redis?????????
     */
    @Override
    public boolean refund(StoreOrderRefundRequest request) {
        StoreOrder storeOrder = getInfoException(request.getOrderNo());
        if (!storeOrder.getPaid()) {
            throw new CrmebException("?????????????????????");
        }
        if (storeOrder.getRefundPrice().add(request.getAmount()).compareTo(storeOrder.getPayPrice()) > 0) {
            throw new CrmebException("??????????????????????????????????????????????????????");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            if (storeOrder.getPayPrice().compareTo(BigDecimal.ZERO) != 0) {
                throw new CrmebException("?????????????????????0????????????????????????");
            }
        }
        request.setOrderId(storeOrder.getId());
        //??????
        User user = userService.getById(storeOrder.getUid());

        //??????
        if (storeOrder.getPayType().equals(Constants.PAY_TYPE_WE_CHAT) && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                storeOrderRefundService.refund(request, storeOrder);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CrmebException("???????????????????????????");
            }
        }

        //????????????????????????
        storeOrder.setRefundStatus(3);
        storeOrder.setRefundPrice(request.getAmount());

        Boolean execute = transactionTemplate.execute(e -> {
            updateById(storeOrder);
            if (storeOrder.getPayType().equals(Constants.PAY_TYPE_YUE)) {
                //????????????
                request.setOrderId(storeOrder.getId());
                userBillService.saveRefundBill(request, user);

                // ??????????????????
                userService.operationNowMoney(user.getUid(), request.getAmount(), user.getNowMoney(), "add");

                // ??????task
                redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, storeOrder.getId());
            }
            if (storeOrder.getPayType().equals(Constants.PAY_TYPE_WE_CHAT) && request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                //????????????
                userBillService.saveRefundBill(request, user);

                // ??????task
                redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, storeOrder.getId());
            }
            return Boolean.TRUE;
        });
        if (!execute) {
            storeOrderStatusService.saveRefund(storeOrder.getId(), request.getAmount(), "??????");
            throw new CrmebException("??????????????????");
        }

        // ??????????????????
        return execute;
    }

    /**
     * ???????????????PC???
     * @param orderNo ????????????
     * @return StoreOrderInfoResponse
     */
    @Override
    public StoreOrderInfoResponse info(String orderNo) {
        StoreOrder storeOrder = getInfoException(orderNo);
        if (storeOrder.getIsSystemDel()) {
            throw new CrmebException("???????????????????????????");
        }
        StoreOrderInfoResponse storeOrderInfoResponse = new StoreOrderInfoResponse();
        BeanUtils.copyProperties(storeOrder, storeOrderInfoResponse);
        List<StoreOrderInfoOldVo> orderInfos = StoreOrderInfoService.getOrderListByOrderId(storeOrder.getId());
        storeOrderInfoResponse.setOrderInfo(orderInfos);
        storeOrderInfoResponse.setPayTypeStr(getPayType(storeOrder.getPayType()));
        storeOrderInfoResponse.setStatusStr(getStatus(storeOrder));
        if (ObjectUtil.isNotNull(storeOrder.getStoreId()) && storeOrder.getStoreId() > 0) {
            SystemStore systemStorePram = new SystemStore();
            systemStorePram.setId(storeOrder.getStoreId());
            storeOrderInfoResponse.setSystemStore(systemStoreService.getByCondition(systemStorePram));
        }

        //????????????
        User user = userService.getById(storeOrder.getUid());
        storeOrderInfoResponse.setNikeName(user.getNickname());
        storeOrderInfoResponse.setPhone(user.getPhone());

        UserBrokerageRecord brokerageRecord = userBrokerageRecordService.getByLinkIdAndLinkType(orderNo, "order");
        if (ObjectUtil.isNotNull(brokerageRecord)) {
            User spread = userService.getById(brokerageRecord.getUid());
            storeOrderInfoResponse.setSpreadName(spread.getNickname());
        }

        storeOrderInfoResponse.setProTotalPrice(storeOrder.getTotalPrice().subtract(storeOrder.getTotalPostage()));
        return storeOrderInfoResponse;
    }

    /** ????????????
     * @param request StoreOrderSendRequest ????????????
     * @author Mr.Zhang
     * @since 2020-06-10
     * @return boolean
     */
    @Override
    public Boolean send(StoreOrderSendRequest request) {
        //????????????
        StoreOrder storeOrder = getInfoException(request.getOrderNo());
        if (storeOrder.getIsDel()) throw new CrmebException("???????????????,????????????!");
        if (storeOrder.getStatus() > 0) throw new CrmebException("?????????????????????????????????!");
        request.setId(storeOrder.getId());
        switch (request.getType()) {
            case "1":// ??????
                express(request, storeOrder);
                break;
            case "2":// ??????
                delivery(request, storeOrder);
                break;
            case "3":// ??????
                virtual(request, storeOrder);
                break;
            default:
                throw new CrmebException("????????????");
        }
        return true;
    }

    /**
     * ????????????
     * @param orderNo ????????????
     * @param mark ??????
     * @return Boolean
     */
    @Override
    public Boolean mark(String orderNo, String mark) {
        StoreOrder storeOrder = getInfoException(orderNo);
        storeOrder.setRemark(mark);
        return updateById(storeOrder);
    }

    /**
     * ????????????
     * @param orderNo ????????????
     * @param reason String ??????
     * @return Boolean
     */
    @Override
    public Boolean refundRefuse(String orderNo, String reason) {
        if (StrUtil.isBlank(reason)) {
            throw new CrmebException("???????????????????????????");
        }
        StoreOrder storeOrder = getInfoException(orderNo);
        storeOrder.setRefundReason(reason);
        storeOrder.setRefundStatus(0);

        User user = userService.getById(storeOrder.getUid());

        Boolean execute = transactionTemplate.execute(e -> {
            updateById(storeOrder);
            storeOrderStatusService.createLog(storeOrder.getId(), Constants.ORDER_LOG_REFUND_REFUSE, Constants.ORDER_LOG_MESSAGE_REFUND_REFUSE.replace("{reason}", reason));
            return Boolean.TRUE;
        });
        if (execute) {
            // ????????????????????????????????????????????????
            if (ObjectUtil.isNotNull(storeOrder) && storeOrder.getPinkId() > 0) {
                StorePink storePink = storePinkService.getById(storeOrder.getPinkId());
                if (storePink.getStatus().equals(3)) {
                    storePink.setStatus(1);
                    storePinkService.updateById(storePink);
                }
            }
        }
        return execute;
    }

    /**
     * ????????????
     * @param storeOrder StoreOrder ????????????
     * @author Mr.Zhang
     * @since 2020-05-28
     * @return StoreOrder
     */
    @Override
    public StoreOrder getInfoByEntity(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.setEntity(storeOrder);
        return dao.selectOne(lambdaQueryWrapper);
    }

    /**
     * ????????????????????????
     * @param orderNo ????????????
     * @return LogisticsResultVo
     */
    @Override
    public LogisticsResultVo getLogisticsInfo(String orderNo) {
        StoreOrder info = getInfoException(orderNo);
        if (info.getType().equals(1)) {// ???????????????
            Express express = expressService.getByName(info.getDeliveryName());
            if (ObjectUtil.isNotNull(express)) {
                info.setDeliveryCode(express.getCode());
            } else {
                info.setDeliveryCode("");
            }
        }
        return logisticService.info(info.getDeliveryId(), null, Optional.ofNullable(info.getDeliveryCode()).orElse(""), info.getUserPhone());
    }

    /**
     * ?????? top ????????????
     * @param status ????????????
     * @return ??????????????????
     */
    @Override
    public Integer getTopDataUtil(Integer status, Integer userId) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        orderUtils.statusApiByWhere(lqw, status);
        lqw.eq(StoreOrder::getUid,userId);
        return dao.selectCount(lqw);
    }

    /**
     * ??????
     * @param orderNo ????????????
     * @param price ??????????????????
     * @param oldPrice ???????????????
     */
    private Boolean orderEditPrice(String orderNo, BigDecimal price, BigDecimal oldPrice) {
        LambdaUpdateWrapper<StoreOrder> luw = new LambdaUpdateWrapper<>();
        luw.set(StoreOrder::getPayPrice, price);
        luw.set(StoreOrder::getBeforePayPrice, oldPrice);
        luw.set(StoreOrder::getIsAlterPrice, 1);
        luw.eq(StoreOrder::getOrderId, orderNo);
        luw.eq(StoreOrder::getPaid, false);
        return update(luw);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param dateLimit ????????????
     * @param type ??????
     * @return ??????????????????
     */
    @Override
    public StoreOrderStatisticsResponse orderStatisticsByTime(String dateLimit, Integer type) {
        StoreOrderStatisticsResponse response = new StoreOrderStatisticsResponse();
        // ???????????????????????????????????????????????? ?????????????????????????????????????????? ?????????????????????????????????????????? ?????????????????????
        dateLimitUtilVo dateRange = DateUtil.getDateLimit(dateLimit);
        String dateStartD = dateRange.getStartTime();
        String dateEndD = dateRange.getEndTime();
        int days = DateUtil.daysBetween(
                DateUtil.strToDate(dateStartD,Constants.DATE_FORMAT_DATE),
                DateUtil.strToDate(dateEndD,Constants.DATE_FORMAT_DATE)
        );
        // ???????????????????????????????????????
        String perDateStart = DateUtil.addDay(
                DateUtil.strToDate(dateStartD,Constants.DATE_FORMAT_DATE), -days, Constants.DATE_FORMAT_START);
        // ??????????????????
        String dateStart = DateUtil.addDay(
                DateUtil.strToDate(dateStartD,Constants.DATE_FORMAT_DATE),0,Constants.DATE_FORMAT_START);
        String dateEnd = DateUtil.addDay(
                DateUtil.strToDate(dateEndD,Constants.DATE_FORMAT_DATE),0,Constants.DATE_FORMAT_END);

        // ????????????????????????
        List<StoreOrder> orderPerList = getOrderPayedByDateLimit(perDateStart,dateStart);

        // ???????????????
        List<StoreOrder> orderCurrentList = getOrderPayedByDateLimit(dateStart, dateEnd);
        double increasePrice = 0;
        if (type == 1) {
            double perSumPrice = orderPerList.stream().mapToDouble(e -> e.getPayPrice().doubleValue()).sum();
            double currentSumPrice = orderCurrentList.stream().mapToDouble(e -> e.getPayPrice().doubleValue()).sum();

            response.setChart(dao.getOrderStatisticsPriceDetail(new StoreDateRangeSqlPram(dateStart,dateEnd)));
            response.setTime(BigDecimal.valueOf(currentSumPrice).setScale(2,BigDecimal.ROUND_HALF_UP));
            // ??????????????????????????????????????????????????????
            increasePrice = currentSumPrice - perSumPrice;
            if (increasePrice <= 0) response.setGrowthRate(0);
            else if (perSumPrice == 0) response.setGrowthRate((int) increasePrice * 100);
            else response.setGrowthRate((int)((increasePrice * perSumPrice) * 100));
        }else if (type ==2) {
            response.setChart(dao.getOrderStatisticsOrderCountDetail(new StoreDateRangeSqlPram(dateStart,dateEnd)));
            response.setTime(BigDecimal.valueOf(orderCurrentList.size()));
            increasePrice = orderCurrentList.size() - orderPerList.size();
            if (increasePrice <= 0) response.setGrowthRate(0);
            else if (orderPerList.size() == 0) response.setGrowthRate((int) increasePrice);
            else response.setGrowthRate((int)((increasePrice / orderPerList.size()) * 100));
        }
        response.setIncreaseTime(increasePrice+"");
        response.setIncreaseTimeStatus(increasePrice >= 0 ? 1:2);
        return response;
    }

    /**
     * ?????????????????????????????????
     *
     * @param uid ??????uid
     * @param seckillId ????????????id
     * @return ???????????????????????????????????????
     */
    @Override
    public List<StoreOrder> getUserCurrentDaySecKillOrders(Integer uid, Integer seckillId) {
        String dayStart = DateUtil.nowDateTime(Constants.DATE_FORMAT_START);
        String dayEnd = DateUtil.nowDateTime(Constants.DATE_FORMAT_END);
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getUid, uid);
        lqw.eq(StoreOrder::getSeckillId, seckillId);
        lqw.between(StoreOrder::getCreateTime, dayStart, dayEnd);
        lqw.eq(StoreOrder::getIsDel, false);
        return dao.selectList(lqw);
    }

    /**
     * ?????????????????????????????????
     * @param uid    ??????uid
     * @return  ???????????????????????????
     */
    @Override
    public List<StoreOrder> getUserCurrentBargainOrders(Integer uid, Integer bargainId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getUid, uid);
        lqw.eq(StoreOrder::getBargainId, bargainId);
        lqw.eq(StoreOrder::getIsDel, false);
        return dao.selectList(lqw);
    }

    /**
     * ?????????????????????????????????
     * @param uid    ??????uid
     * @return  ???????????????????????????
     */
    @Override
    public List<StoreOrder> getUserCurrentCombinationOrders(Integer uid, Integer combinationId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getUid, uid);
        lqw.eq(StoreOrder::getCombinationId, combinationId);
        lqw.eq(StoreOrder::getIsDel, false);
        return dao.selectList(lqw);
    }

    @Override
    public StoreOrder getByOderId(String orderId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, orderId);
        return dao.selectOne(lqw);
    }

    /**
     * ??????????????????????????????
     * @return ExpressSheetVo
     */
    @Override
    public ExpressSheetVo getDeliveryInfo() {
        return systemConfigService.getDeliveryInfo();
    }

    /**
     * ??????????????????
     * @param orderNo ????????????
     * @return Boolean
     */
    @Override
    public Boolean updatePaid(String orderNo) {
        LambdaUpdateWrapper<StoreOrder> lqw = new LambdaUpdateWrapper<>();
        lqw.set(StoreOrder::getPaid, true);
        lqw.set(StoreOrder::getPayTime, DateUtil.nowDateTime());
        lqw.eq(StoreOrder::getOrderId, orderNo);
        lqw.eq(StoreOrder::getPaid,false);
        return update(lqw);
    }

    /**
     * ???????????????????????????????????????Map
     * @param orderNoList ???????????????
     * @return Map
     */
    @Override
    public Map<String, StoreOrder> getMapInOrderNo(List<String> orderNoList) {
        Map<String, StoreOrder> map = CollUtil.newHashMap();
        LambdaUpdateWrapper<StoreOrder> lqw = new LambdaUpdateWrapper<>();
        lqw.in(StoreOrder::getOrderId, orderNoList);
        List<StoreOrder> orderList = dao.selectList(lqw);
        orderList.forEach(order -> {
            map.put(order.getOrderId(), order);
        });
        return map;
    }

    /**
     * ???????????????????????????
     * @param orderNoList ??????????????????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getSpreadOrderTotalPriceByOrderList(List<String> orderNoList) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.select(StoreOrder::getPayPrice);
        lqw.in(StoreOrder::getOrderId, orderNoList);
        List<StoreOrder> orderList = dao.selectList(lqw);
        return orderList.stream().map(StoreOrder::getPayPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ????????????????????????id??????
     * @return List<StoreOrder>
     */
    @Override
    public List<StoreOrder> findIdAndUidListByReceipt() {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.select(StoreOrder::getId, StoreOrder::getUid);
        lqw.eq(StoreOrder::getStatus, 2);
        lqw.eq(StoreOrder::getRefundStatus, 0);
        lqw.eq(StoreOrder::getIsDel, false);
        List<StoreOrder> orderList = dao.selectList(lqw);
        if (CollUtil.isEmpty(orderList)) {
            return CollUtil.newArrayList();
        }
        return orderList;
    }

    /**
     *
     * @param userId ??????uid
     * @param pageParamRequest ????????????
     * @return List
     */
    @Override
    public List<StoreOrder> findPaidListByUid(Integer userId, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreOrder::getUid, userId);
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        lqw.orderByDesc(StoreOrder::getId);
        return dao.selectList(lqw);
    }

    /**
     * ????????????
     * @param request ??????????????????
     * @return ????????????
     */
    @Override
    public Boolean updatePrice(StoreOrderUpdatePriceRequest request) {
        StoreOrder existOrder = getInfoException(request.getOrderNo());
        // ???????????????
        if (existOrder.getPaid()) {
            throw new CrmebException(StrUtil.format("???????????? {} ??????????????????", existOrder.getOrderId()));
        }
        if (existOrder.getIsAlterPrice()) {
            throw new CrmebException("???????????????????????????");
        }
        // ?????????????????????????????????
        if (existOrder.getPayPrice().compareTo(request.getPayPrice()) ==0) {
            throw new CrmebException(StrUtil.format("?????????????????????????????????????????? ?????? {} ????????? {}", existOrder.getPayPrice(), request.getPayPrice()));
        }
        String oldPrice = existOrder.getPayPrice()+"";

        Boolean execute = transactionTemplate.execute(e -> {
            // ??????????????????
            orderEditPrice(existOrder.getOrderId(), request.getPayPrice(), existOrder.getPayPrice());
            // ????????????????????????
            storeOrderStatusService.createLog(existOrder.getId(), Constants.ORDER_LOG_EDIT,
                    Constants.RESULT_ORDER_EDIT_PRICE_LOGS.replace("${orderPrice}", oldPrice)
                            .replace("${price}", request.getPayPrice() + ""));
            return Boolean.TRUE;
        });
        if (!execute) {
            throw new CrmebException(Constants.RESULT_ORDER_EDIT_PRICE_SUCCESS
                    .replace("${orderNo}", existOrder.getOrderId()).replace("${price}", request.getPayPrice()+""));
        }
        // ????????????????????????
        SystemNotification notification = systemNotificationService.getByMark(NotifyConstants.MODIFY_ORDER_PRICE_MARK);
        if (notification.getIsSms().equals(1)) {
            User user = userService.getById(existOrder.getUid());
            if (StrUtil.isNotBlank(user.getPhone())) {
                SmsTemplate smsTemplate = smsTemplateService.getDetail(notification.getSmsId());
                // ????????????????????????
                smsService.sendOrderEditPriceNotice(user.getPhone(), existOrder.getOrderId(), request.getPayPrice(), Integer.valueOf(smsTemplate.getTempId()));
            }
        }

        return execute;
    }

    /**
     * ?????????????????????
     * @param uid ??????uid
     * @return Integer
     */
    @Override
    public Integer getOrderCountByUid(Integer uid) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid, uid);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        return dao.selectCount(lqw);
    }

    /**
     * ???????????????????????????
     * @param userId ??????uid
     * @return BigDecimal
     */
    @Override
    public BigDecimal getSumPayPriceByUid(Integer userId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.select(StoreOrder::getPayPrice);
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid, userId);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        List<StoreOrder> orderList = dao.selectList(lqw);
        return orderList.stream().map(StoreOrder::getPayPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ??????????????????(??????)
     * @param uid ??????uid
     * @return Integer
     */
    @Override
    public Integer getOrderCountByUidAndDate(Integer uid, String date) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid, uid);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        if (StrUtil.isNotBlank(date)) {
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            lqw.between(StoreOrder::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        return dao.selectCount(lqw);
    }

    /**
     * ????????????????????????(??????)
     * @param userId ??????uid
     * @return BigDecimal
     */
    @Override
    public BigDecimal getSumPayPriceByUidAndDate(Integer userId, String date) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.select(StoreOrder::getPayPrice);
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid, userId);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        if (StrUtil.isNotBlank(date)) {
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            lqw.between(StoreOrder::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        List<StoreOrder> orderList = dao.selectList(lqw);
        return orderList.stream().map(StoreOrder::getPayPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ??????????????????
     * @param bargainId ????????????id
     * @param bargainUserId ??????????????????id
     * @return StoreOrder
     */
    @Override
    public StoreOrder getByBargainOrder(Integer bargainId, Integer bargainUserId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getBargainId, bargainId);
        lqw.eq(StoreOrder::getBargainUserId, bargainUserId);
        lqw.orderByDesc(StoreOrder::getId);
        lqw.last(" limit 1");
        return dao.selectOne(lqw);
    }

    /**
     * ????????????????????????
     * @return StoreOrderCountItemResponse
     */
    @Override
    public StoreOrderCountItemResponse getOrderStatusNum(String dateLimit, Integer type) {
        StoreOrderCountItemResponse response = new StoreOrderCountItemResponse();
        if (type.equals(2)) {
            type = null;
        }
        // ????????????
        response.setAll(getCount(dateLimit, Constants.ORDER_STATUS_ALL, type));
        // ???????????????
        response.setUnPaid(getCount(dateLimit, Constants.ORDER_STATUS_UNPAID, type));
        // ???????????????
        response.setNotShipped(getCount(dateLimit, Constants.ORDER_STATUS_NOT_SHIPPED, type));
        // ???????????????
        response.setSpike(getCount(dateLimit, Constants.ORDER_STATUS_SPIKE, type));
        // ???????????????
        response.setBargain(getCount(dateLimit, Constants.ORDER_STATUS_BARGAIN, type));
        // ??????????????????
        response.setComplete(getCount(dateLimit, Constants.ORDER_STATUS_COMPLETE, type));
        // ???????????????
        response.setToBeWrittenOff(getCount(dateLimit, Constants.ORDER_STATUS_TOBE_WRITTEN_OFF, type));
        // ???????????????
        response.setRefunding(getCount(dateLimit, Constants.ORDER_STATUS_REFUNDING, type));
        // ???????????????
        response.setRefunded(getCount(dateLimit, Constants.ORDER_STATUS_REFUNDED, type));
        // ???????????????
        response.setDeleted(getCount(dateLimit, Constants.ORDER_STATUS_DELETED, type));
        return response;
    }

    /**
     * ????????????????????????
     * @param dateLimit ?????????
     * @return StoreOrderTopItemResponse
     */
    @Override
    public StoreOrderTopItemResponse getOrderData(String dateLimit) {
        StoreOrderTopItemResponse itemResponse = new StoreOrderTopItemResponse();
        // ????????????
        itemResponse.setCount(getCount(dateLimit, Constants.ORDER_STATUS_ALL));
        // ????????????
        itemResponse.setAmount(getAmount(dateLimit, ""));
        // ??????????????????
        itemResponse.setWeChatAmount(getAmount(dateLimit, Constants.PAY_TYPE_WE_CHAT));
        // ??????????????????
        itemResponse.setYueAmount(getAmount(dateLimit, Constants.PAY_TYPE_YUE));
        return itemResponse;
    }

    /**
     * ????????????
     * @param orderNo ????????????
     * @return Boolean
     */
    @Override
    public Boolean delete(String orderNo) {
        StoreOrder storeOrder = getInfoException(orderNo);
        if (!storeOrder.getIsDel()) {
            throw new CrmebException("?????????????????????????????????????????????????????????????????????????????????????????????");
        }
        if (storeOrder.getIsSystemDel()) {
            throw new CrmebException("???????????????????????????!");
        }
        storeOrder.setIsSystemDel(true);
        return updateById(storeOrder);
    }

    /**
     * ????????????????????????????????????
     * @param date ?????????yyyy-MM-dd??????
     * @return Integer
     */
    @Override
    public Integer getOrderProductNumByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(total_num), 0) as total_num");
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        StoreOrder storeOrder = dao.selectOne(wrapper);
        return storeOrder.getTotalNum();
    }

    /**
     * ??????????????????????????????????????????
     * @param date ?????????yyyy-MM-dd??????
     * @return Integer
     */
    @Override
    public Integer getOrderSuccessProductNumByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(total_num), 0) as total_num");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        StoreOrder storeOrder = dao.selectOne(wrapper);
        return storeOrder.getTotalNum();
    }

    /**
     * ??????????????????????????????
     * @param date ?????????yyyy-MM-dd??????
     * @return Integer
     */
    @Override
    public Integer getOrderNumByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        return dao.selectCount(wrapper);
    }

    /**
     * ????????????????????????????????????
     * @param date ?????????yyyy-MM-dd??????
     * @return Integer
     */
    @Override
    public Integer getPayOrderNumByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        return dao.selectCount(wrapper);
    }

    /**
     * ????????????????????????????????????
     * @param date ?????????yyyy-MM-dd??????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getPayOrderAmountByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(pay_price), 0) as pay_price");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        StoreOrder storeOrder = dao.selectOne(wrapper);
        return storeOrder.getPayPrice();
    }

    /**
     * ????????????????????????????????????
     * @param startDate ??????
     * @param endDate ??????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getPayOrderAmountByPeriod(String startDate, String endDate) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(pay_price), 0) as pay_price");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') between {0} and {1}", startDate, endDate);
        StoreOrder storeOrder = dao.selectOne(wrapper);
        return storeOrder.getPayPrice();
    }

    /**
     * ????????????????????????????????????????????????
     * @param date ?????????yyyy-MM-dd??????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getYuePayOrderAmountByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(pay_price), 0) as pay_price");
        wrapper.eq("paid", 1);
        wrapper.eq("pay_type", "yue");
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        StoreOrder storeOrder = dao.selectOne(wrapper);
        return storeOrder.getPayPrice();
    }

    /**
     * ????????????????????????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getTotalPrice() {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(pay_price), 0) as pay_price");
        wrapper.eq("paid", 1);
        StoreOrder storeOrder = dao.selectOne(wrapper);
        return storeOrder.getPayPrice();
    }

    /**
     * ????????????????????????????????????
     * @param date ??????
     * @return Integer
     */
    @Override
    public Integer getOrderUserNumByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        wrapper.groupBy("uid");
        List<StoreOrder> orderList = dao.selectList(wrapper);
        if (CollUtil.isEmpty(orderList)) {
            return 0;
        }
        return orderList.size();
    }

    /**
     * ????????????????????????????????????
     * @param startDate ??????
     * @param endDate ??????
     * @return Integer
     */
    @Override
    public Integer getOrderUserNumByPeriod(String startDate, String endDate) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.apply("date_format(create_time, '%Y-%m-%d') between {0} and {1}", startDate, endDate);
        wrapper.groupBy("uid");
        List<StoreOrder> orderList = dao.selectList(wrapper);
        if (CollUtil.isEmpty(orderList)) {
            return 0;
        }
        return orderList.size();
    }

    /**
     * ????????????????????????????????????
     * @param date ??????
     * @return Integer
     */
    @Override
    public Integer getOrderPayUserNumByDate(String date) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        wrapper.groupBy("uid");
        List<StoreOrder> orderList = dao.selectList(wrapper);
        if (CollUtil.isEmpty(orderList)) {
            return 0;
        }
        return orderList.size();
    }

    /**
     * ????????????????????????????????????
     * @param startDate ??????
     * @param endDate ??????
     * @return Integer
     */
    @Override
    public Integer getOrderPayUserNumByPeriod(String startDate, String endDate) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.eq("paid", 1);
        wrapper.apply("date_format(create_time, '%Y-%m-%d') between {0} and {1}", startDate, endDate);
        wrapper.groupBy("uid");
        List<StoreOrder> orderList = dao.selectList(wrapper);
        if (CollUtil.isEmpty(orderList)) {
            return 0;
        }
        return orderList.size();
    }

    /**
     * ????????????uid??????????????????????????????
     * @param uidList ????????????
     * @return Integer
     */
    @Override
    public Integer getOrderPayUserNumByUidList(List<Integer> uidList) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        wrapper.eq("paid", 1);
        wrapper.in("uid", uidList);
        wrapper.groupBy("uid");
        List<StoreOrder> orderList = dao.selectList(wrapper);
        if (CollUtil.isEmpty(orderList)) {
            return 0;
        }
        return orderList.size();
    }

    /**
     * ????????????uid????????????????????????
     * @param uidList ????????????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getPayOrderAmountByUidList(List<Integer> uidList) {
        QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(sum(pay_price), 0.00) as pay_price");
//        wrapper.select("ifnull(if(sum(pay_price) = 0.00, 0, sum(pay_price)), 0) as pay_price");
        wrapper.eq("paid", 1);
        wrapper.in("uid", uidList);
        List<StoreOrder> orderList = dao.selectList(wrapper);
        if (CollUtil.isEmpty(orderList)) {
            return BigDecimal.ZERO;
        }
        return orderList.stream().map(StoreOrder::getPayPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ???????????????????????????
     * @return Integer
     */
    @Override
    public Integer getNotShippingNum() {
        return getCount("", Constants.ORDER_STATUS_NOT_SHIPPED);
    }

    /**
     * ???????????????????????????
     */
    @Override
    public Integer getRefundingNum() {
        return getCount("", Constants.ORDER_STATUS_REFUNDING);
    }

    /**
     * ???????????????????????????
     */
    @Override
    public Integer getNotWriteOffNum() {
        return getCount("", Constants.ORDER_STATUS_TOBE_WRITTEN_OFF);
    }

    /**
     * ????????????????????????
     * @param uid ??????uid
     * @param spreadId ?????????uid
     */
    @Override
    public OrderBrokerageData getBrokerageData(Integer uid, Integer spreadId) {
        return dao.getBrokerageData(uid, spreadId);
    }

///////////////////////////////////////////////////////////////////////////////////////////////////// ????????????????????????

    /**
     * ????????????????????????????????????
     * @return ??????????????????
     */
    private List<StoreOrder> getOrderPayedByDateLimit(String startTime, String endTime) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getIsDel, false).eq(StoreOrder::getPaid, true).eq(StoreOrder::getRefundStatus,0)
                .between(StoreOrder::getCreateTime, startTime, endTime);
     return dao.selectList(lqw);
    }

    private StoreOrder getInfoException(String orderNo) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, orderNo);
        StoreOrder storeOrder = dao.selectOne(lqw);
        if (ObjectUtil.isNull(storeOrder)) {
            throw new CrmebException("????????????????????????");
        }
        return storeOrder;
    }

    /** ??????
     * @param request StoreOrderSendRequest ????????????
     * @param storeOrder StoreOrder ????????????
     */
    private void express(StoreOrderSendRequest request, StoreOrder storeOrder) {
        // ????????????????????????
        validateExpressSend(request);
        //??????????????????
        Express express = expressService.getByCode(request.getExpressCode());
        if (request.getExpressRecordType().equals("1")) { // ????????????
            deliverGoods(request, storeOrder);
        }
        if (request.getExpressRecordType().equals("2")) { // ????????????
            request.setExpressName(express.getName());
            expressDump(request, storeOrder, express);
        }

        storeOrder.setDeliveryCode(express.getCode());
        storeOrder.setDeliveryName(express.getName());
        storeOrder.setStatus(1);
        storeOrder.setDeliveryType("express");

        String message = Constants.ORDER_LOG_MESSAGE_EXPRESS.replace("{deliveryName}", express.getName()).replace("{deliveryCode}", storeOrder.getDeliveryId());

        Boolean execute = transactionTemplate.execute(i -> {
            updateById(storeOrder);
            //??????????????????
            storeOrderStatusService.createLog(request.getId(), Constants.ORDER_LOG_EXPRESS, message);
            return Boolean.TRUE;
        });

        if (!execute) throw new CrmebException("?????????????????????");

        sendGoodsNotify(storeOrder);
    }


    /**
     * ????????????
     * @param storeOrder ??????
     */
    private void sendGoodsNotify(StoreOrder storeOrder) {
        User user = userService.getById(storeOrder.getUid());
        SystemNotification notification = systemNotificationService.getByMark(NotifyConstants.DELIVER_GOODS_MARK);
        if (notification.getIsSms().equals(1)) {
            // ??????????????????
            if (StrUtil.isNotBlank(user.getPhone())) {
                SmsTemplate smsTemplate = smsTemplateService.getDetail(notification.getSmsId());
                String proName = "";
                List<StoreOrderInfoOldVo> voList = storeOrderInfoService.getOrderListByOrderId(storeOrder.getId());
                proName = voList.get(0).getInfo().getProductName();
                if (voList.size() > 1) {
                    proName = proName.concat("???");
                }
                smsService.sendOrderDeliverNotice(user.getPhone(), user.getNickname(), proName, storeOrder.getOrderId(), Integer.valueOf(smsTemplate.getTempId()));
            }
        }

        // ??????????????????
        pushMessageOrder(storeOrder, user, notification);
    }

    /**
     * ??????????????????
     * ????????????????????????
     * ?????????????????????
     * ?????????????????????
     */
    private void pushMessageOrder(StoreOrder storeOrder, User user, SystemNotification notification) {
        if (storeOrder.getIsChannel().equals(2)) {
            return;
        }
        if (!storeOrder.getPayType().equals(Constants.PAY_TYPE_WE_CHAT)) {
            return;
        }
        UserToken userToken;
        HashMap<String, String> temMap = new HashMap<>();

        // ?????????
        if (storeOrder.getIsChannel().equals(Constants.ORDER_PAY_CHANNEL_PUBLIC) && notification.getIsWechat().equals(1)) {
            userToken = userTokenService.getTokenByUserId(user.getUid(), UserConstants.USER_TOKEN_TYPE_WECHAT);
            if (ObjectUtil.isNull(userToken)) {
                return ;
            }
            // ????????????????????????
            temMap.put(Constants.WE_CHAT_TEMP_KEY_FIRST, "??????????????????");
            temMap.put("keyword1", storeOrder.getOrderId());
            temMap.put("keyword2", cn.hutool.core.date.DateUtil.now());
            temMap.put("keyword3", storeOrder.getDeliveryName());
            temMap.put("keyword4", storeOrder.getDeliveryId());
            temMap.put(Constants.WE_CHAT_TEMP_KEY_END, "?????????????????????");
            templateMessageService.pushTemplateMessage(notification.getWechatId(), temMap, userToken.getToken());
            return;
        } else if (notification.getIsRoutine().equals(1)) {
            // ???????????????????????????
            userToken = userTokenService.getTokenByUserId(user.getUid(), UserConstants.USER_TOKEN_TYPE_ROUTINE);
            if (ObjectUtil.isNull(userToken)) {
                return ;
            }
            // ????????????
            // ?????????????????????????????????
//        temMap.put("character_string1", storeOrder.getOrderId());
//        temMap.put("name3", storeOrder.getDeliveryName());
//        temMap.put("character_string4", storeOrder.getDeliveryId());
//        temMap.put("thing7", "?????????????????????");
            // ????????????????????????????????????
            temMap.put("character_string1", storeOrder.getOrderId());
            temMap.put("name6", storeOrder.getDeliveryName());
            temMap.put("character_string7", storeOrder.getDeliveryId());
            temMap.put("thing11", "?????????????????????");
            templateMessageService.pushMiniTemplateMessage(notification.getRoutineId(), temMap, userToken.getToken());
        }
    }

    /**
     * ????????????
     * @param request
     * @param storeOrder
     * @param express
     */
    private void expressDump(StoreOrderSendRequest request, StoreOrder storeOrder, Express express) {
        String configExportOpen = systemConfigService.getValueByKeyException("config_export_open");
        if (!configExportOpen.equals("1")) {// ?????????????????????
            throw new CrmebException("????????????????????????");
        }
        MyRecord record = new MyRecord();
        record.set("com", express.getCode());// ??????????????????
        record.set("to_name", storeOrder.getRealName());// ?????????
        record.set("to_tel", storeOrder.getUserPhone());// ???????????????
        record.set("to_addr", storeOrder.getUserAddress());// ?????????????????????
        record.set("from_name", request.getToName());// ?????????
        record.set("from_tel", request.getToTel());// ???????????????
        record.set("from_addr", request.getToAddr());// ?????????????????????
        record.set("temp_id", request.getExpressTempId());// ??????????????????ID
        String siid = systemConfigService.getValueByKeyException("config_export_siid");
        record.set("siid", siid);// ??????????????????
        record.set("count", storeOrder.getTotalNum());// ????????????

        //????????????????????????
        List<Integer> orderIdList = new ArrayList<>();
        orderIdList.add(storeOrder.getId());
        HashMap<Integer, List<StoreOrderInfoOldVo>> orderInfoMap = StoreOrderInfoService.getMapInId(orderIdList);
        if (orderInfoMap.isEmpty() || !orderInfoMap.containsKey(storeOrder.getId())) {
            throw new CrmebException("?????????????????????????????????");
        }
        List<String> productNameList = new ArrayList<>();
        for (StoreOrderInfoOldVo storeOrderInfoVo : orderInfoMap.get(storeOrder.getId())) {
            productNameList.add(storeOrderInfoVo.getInfo().getProductName());
        }

        record.set("cargo", String.join(",", productNameList));// ????????????
        if (express.getPartnerId()) {
            record.set("partner_id", express.getAccount());// ????????????????????????(????????????????????????)
        }
        if (express.getPartnerKey()) {
            record.set("partner_key", express.getPassword());// ??????????????????(????????????????????????)
        }
        if (express.getNet()) {
            record.set("net", express.getNetName());// ??????????????????(????????????????????????)
        }

        MyRecord myRecord = onePassService.expressDump(record);
        storeOrder.setDeliveryId(myRecord.getStr("kuaidinum"));
    }

    /**
     * ????????????
     */
    private void deliverGoods(StoreOrderSendRequest request, StoreOrder storeOrder) {
        storeOrder.setDeliveryId(request.getExpressNumber());
    }

    /**
     * ????????????????????????
     */
    private void validateExpressSend(StoreOrderSendRequest request) {
        if (request.getExpressRecordType().equals("1")) {
            if (StrUtil.isBlank(request.getExpressNumber())) throw new CrmebException("?????????????????????");
            return;
        }
        if (StrUtil.isBlank(request.getExpressCode())) throw new CrmebException("?????????????????????");
        if (StrUtil.isBlank(request.getExpressRecordType())) throw new CrmebException("???????????????????????????");
        if (StrUtil.isBlank(request.getExpressTempId())) throw new CrmebException("?????????????????????");
        if (StrUtil.isBlank(request.getToName())) throw new CrmebException("????????????????????????");
        if (StrUtil.isBlank(request.getToTel())) throw new CrmebException("????????????????????????");
        if (StrUtil.isBlank(request.getToAddr())) throw new CrmebException("????????????????????????");
    }

    /** ????????????
     * @param request StoreOrderSendRequest ????????????
     * @param storeOrder StoreOrder ????????????
     * @author Mr.Zhang
     * @since 2020-06-10
     */
    private void delivery(StoreOrderSendRequest request, StoreOrder storeOrder) {
        if (StrUtil.isBlank(request.getDeliveryName())) throw new CrmebException("????????????????????????");
        if (StrUtil.isBlank(request.getDeliveryTel())) throw new CrmebException("??????????????????????????????");
        ValidateFormUtil.isPhone(request.getDeliveryTel(), "?????????????????????");

        //????????????
        storeOrder.setDeliveryName(request.getDeliveryName());
        storeOrder.setDeliveryId(request.getDeliveryTel());
        storeOrder.setStatus(1);
        storeOrder.setDeliveryType("send");

        //????????????????????????
        List<Integer> orderIdList = new ArrayList<>();
        orderIdList.add(storeOrder.getId());
        HashMap<Integer, List<StoreOrderInfoOldVo>> orderInfoMap = StoreOrderInfoService.getMapInId(orderIdList);
        if (orderInfoMap.isEmpty() || !orderInfoMap.containsKey(storeOrder.getId())) {
            throw new CrmebException("?????????????????????????????????");
        }
        List<String> productNameList = new ArrayList<>();
        for (StoreOrderInfoOldVo storeOrderInfoVo : orderInfoMap.get(storeOrder.getId())) {
            productNameList.add(storeOrderInfoVo.getInfo().getProductName());
        }

        String message = Constants.ORDER_LOG_MESSAGE_DELIVERY.replace("{deliveryName}", request.getDeliveryName()).replace("{deliveryCode}", request.getDeliveryTel());

        Boolean execute = transactionTemplate.execute(i -> {
            // ????????????
            updateById(storeOrder);
            // ??????????????????
            storeOrderStatusService.createLog(request.getId(), Constants.ORDER_LOG_DELIVERY, message);
            return Boolean.TRUE;
        });
        if (!execute) throw new CrmebException("????????????????????????");

        User user = userService.getById(storeOrder.getUid());
        // ??????????????????
        pushMessageDeliveryOrder(storeOrder, user, request, productNameList);
    }

    /**
     * ??????????????????
     * ????????????????????????
     * ?????????????????????
     * ?????????????????????
     */
    private void pushMessageDeliveryOrder(StoreOrder storeOrder, User user, StoreOrderSendRequest request, List<String> productNameList) {
        if (storeOrder.getIsChannel().equals(2)) {
            return;
        }
        if (!storeOrder.getPayType().equals(Constants.PAY_TYPE_WE_CHAT)) {
            return;
        }
        SystemNotification notification = systemNotificationService.getByMark(NotifyConstants.FULFILLMENT_ORDER_MARK);
        UserToken userToken;
        HashMap<String, String> map = new HashMap<>();
        String proName = "";
        if (CollUtil.isNotEmpty(productNameList)) {
            proName = StringUtils.join(productNameList, "|");
        }
        // ?????????
        if (storeOrder.getIsChannel().equals(Constants.ORDER_PAY_CHANNEL_PUBLIC) && notification.getIsWechat().equals(1)) {
            userToken = userTokenService.getTokenByUserId(user.getUid(), UserConstants.USER_TOKEN_TYPE_WECHAT);
            if (ObjectUtil.isNull(userToken)) {
                return ;
            }
            map.put(Constants.WE_CHAT_TEMP_KEY_FIRST, "??????????????????");
            map.put("keyword1", storeOrder.getOrderId());
            map.put("keyword2", DateUtil.dateToStr(storeOrder.getCreateTime(), Constants.DATE_FORMAT));
            map.put("keyword3", storeOrder.getUserAddress());
            map.put("keyword4", request.getDeliveryName());
            map.put("keyword5", request.getDeliveryTel());
            map.put(Constants.WE_CHAT_TEMP_KEY_END, "?????????????????????");
            // ????????????????????????
            templateMessageService.pushTemplateMessage(notification.getWechatId(), map, userToken.getToken());
        } else if (notification.getIsRoutine().equals(1)) {
            // ???????????????????????????
            userToken = userTokenService.getTokenByUserId(user.getUid(), UserConstants.USER_TOKEN_TYPE_ROUTINE);
            if (ObjectUtil.isNull(userToken)) {
                return ;
            }

            if (proName.length() > 20) {
                proName = proName.substring(0, 15) + "***";
            }
//        map.put("thing8", proName);
//        map.put("character_string1", storeOrder.getOrderId());
//        map.put("name4", request.getDeliveryName());
//        map.put("phone_number10", request.getDeliveryTel());
            map.put("thing8", proName);
            map.put("character_string1", storeOrder.getOrderId());
            map.put("name4", request.getDeliveryName());
            map.put("phone_number10", request.getDeliveryTel());
            templateMessageService.pushMiniTemplateMessage(notification.getRoutineId(), map, userToken.getToken());
        }
    }

    /** ??????
     * @param request StoreOrderSendRequest ????????????
     * @param storeOrder StoreOrder ????????????
     * @author Mr.Zhang
     * @since 2020-06-10
     */
    private void virtual(StoreOrderSendRequest request, StoreOrder storeOrder) {
        //????????????
        storeOrder.setDeliveryType("fictitious");
        storeOrder.setStatus(1);

        Boolean execute = transactionTemplate.execute(i -> {
            updateById(storeOrder);
            //??????????????????
            storeOrderStatusService.createLog(request.getId(), Constants.ORDER_LOG_DELIVERY_VI, "??????????????????");
            return Boolean.TRUE;
        });
        if (!execute) throw new CrmebException("????????????????????????");
    }

    /**
     * ??????????????????
     * @param dateLimit ?????????
     * @param status String ??????
     * @return Integer
     */
    private Integer getCount(String dateLimit, String status) {
        //?????????????????????
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(dateLimit)) {
            dateLimitUtilVo dateLimitUtilVo = DateUtil.getDateLimit(dateLimit);
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
        getStatusWhereNew(queryWrapper, status);
        return dao.selectCount(queryWrapper);
    }

    /**
     * ??????????????????
     * @param dateLimit ?????????
     * @param status String ??????
     * @return Integer
     */
    private Integer getCount(String dateLimit, String status, Integer type) {
        //?????????????????????
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(dateLimit)) {
            dateLimitUtilVo dateLimitUtilVo = DateUtil.getDateLimit(dateLimit);
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
        getStatusWhereNew(queryWrapper, status);
        if (ObjectUtil.isNotNull(type)) {
            queryWrapper.eq("type", type);
        }
        return dao.selectCount(queryWrapper);
    }

    /**
     * ??????????????????
     * @param dateLimit ?????????
     * @param type  ????????????
     * @return Integer
     */
    private BigDecimal getAmount(String dateLimit, String type) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price");
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq("pay_type", type);
        }
        queryWrapper.isNotNull("pay_time");
        queryWrapper.eq("paid", 1);
        if (StringUtils.isNotBlank(dateLimit)) {
            dateLimitUtilVo dateLimitUtilVo = DateUtil.getDateLimit(dateLimit);
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
        StoreOrder storeOrder = dao.selectOne(queryWrapper);
        if (ObjectUtil.isNull(storeOrder)) {
            return BigDecimal.ZERO;
        }
        return storeOrder.getPayPrice();
    }

    /**
     * ??????request???where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param request StoreOrderSearchRequest ????????????
     */
    private void getRequestTimeWhere(QueryWrapper<StoreOrder> queryWrapper, StoreOrderSearchRequest request) {
        if (StringUtils.isNotBlank(request.getDateLimit())) {
            dateLimitUtilVo dateLimitUtilVo = DateUtil.getDateLimit(request.getDateLimit());
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
    }

    /**
     * ????????????????????????where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param status String ??????
     */
    private void getStatusWhereNew(QueryWrapper<StoreOrder> queryWrapper, String status) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        switch (status) {
            case Constants.ORDER_STATUS_ALL: //??????
                break;
            case Constants.ORDER_STATUS_UNPAID: //?????????
                queryWrapper.eq("paid", 0);//????????????
                queryWrapper.eq("status", 0); //????????????
                queryWrapper.eq("is_del", 0);//????????????
                break;
            case Constants.ORDER_STATUS_NOT_SHIPPED: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 1);//????????????
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_SPIKE: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 1);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_BARGAIN: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 2);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_COMPLETE: //????????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 3);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_TOBE_WRITTEN_OFF: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 2);//????????????
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDING: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.in("refund_status", 1,3);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDED: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("refund_status", 2);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_DELETED: //?????????
                queryWrapper.eq("is_del", 1);
                break;
            default:
                queryWrapper.eq("paid", 1);
                queryWrapper.ne("refund_status", 2);
                break;
        }
        queryWrapper.eq("is_system_del", 0);
    }

    /**
     * ????????????????????????where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param status String ??????
     */
    private void getStatusWhere(QueryWrapper<StoreOrder> queryWrapper, String status) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        switch (status) {
            case Constants.ORDER_STATUS_UNPAID: //?????????
                queryWrapper.eq("paid", 0);//????????????
                queryWrapper.eq("status", 0); //????????????
                queryWrapper.eq("is_del", 0);//????????????
                break;
            case Constants.ORDER_STATUS_NOT_SHIPPED: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 1);//????????????
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_SPIKE: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 1);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_BARGAIN: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 2);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_COMPLETE: //????????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 3);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_TOBE_WRITTEN_OFF: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 2);//????????????
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDING: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.in("refund_status", 1,3);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDED: //?????????
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("refund_status", 2);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_DELETED: //?????????
                queryWrapper.eq("is_del", 1);
                break;
            default:
                queryWrapper.eq("paid", 1);
                queryWrapper.ne("refund_status", 2);
                break;
        }
        queryWrapper.eq("is_system_del", 0);
    }

    /**
     * ??????????????????
     * @param storeOrder StoreOrder ????????????
     * @author Mr.Zhang
     * @since 2020-06-12
     */
    private Map<String, String> getStatus(StoreOrder storeOrder) {
        Map<String, String> map = new HashMap<>();
        map.put("key", "");
        map.put("value", "");
        if (null == storeOrder) {
            return map;
        }
        // ?????????
        if (!storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_UNPAID);
            map.put("value", Constants.ORDER_STATUS_STR_UNPAID);
            return map;
        }
        // ?????????
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_NOT_SHIPPED);
            map.put("value", Constants.ORDER_STATUS_STR_NOT_SHIPPED);
            return map;
        }
        // ?????????
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 1
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_SPIKE);
            map.put("value", Constants.ORDER_STATUS_STR_SPIKE);
            return map;
        }
        // ?????????
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 2
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_BARGAIN);
            map.put("value", Constants.ORDER_STATUS_STR_BARGAIN);
            return map;
        }
        // ????????????
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 3
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_COMPLETE);
            map.put("value", Constants.ORDER_STATUS_STR_COMPLETE);
            return map;
        }
        // ?????????
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_TOBE_WRITTEN_OFF);
            map.put("value", Constants.ORDER_STATUS_STR_TOBE_WRITTEN_OFF);
            return map;
        }

        //????????????
        if (storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_APPLY_REFUNDING);
            map.put("value", Constants.ORDER_STATUS_STR_APPLY_REFUNDING);
            return map;
        }

        //?????????
        if (storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 3
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_REFUNDING);
            map.put("value", Constants.ORDER_STATUS_STR_REFUNDING);
            return map;
        }

        //?????????
        if (storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_REFUNDED);
            map.put("value", Constants.ORDER_STATUS_STR_REFUNDED);
        }

        //?????????
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_DELETED);
            map.put("value", Constants.ORDER_STATUS_STR_DELETED);
        }

        return map;
    }
    /**
     * ??????????????????
     * @param payType String ????????????
     */
    private String getPayType(String payType) {
        switch (payType) {
            case Constants.PAY_TYPE_WE_CHAT:
                return Constants.PAY_TYPE_STR_WE_CHAT;
            case Constants.PAY_TYPE_YUE:
                return Constants.PAY_TYPE_STR_YUE;
            case Constants.PAY_TYPE_ALI_PAY:
                return Constants.PAY_TYPE_STR_ALI_PAY;
            default:
                return Constants.PAY_TYPE_STR_OTHER;
        }
    }

}

