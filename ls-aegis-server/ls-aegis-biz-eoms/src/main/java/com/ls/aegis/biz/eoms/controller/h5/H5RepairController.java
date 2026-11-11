package com.ls.aegis.biz.eoms.controller.h5;

import com.ls.aegis.biz.eoms.utils.Coordinates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import cn.dev33.satoken.annotation.SaIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.RepairRecord;
import com.ls.aegis.biz.eoms.interceptor.H5TokenInterceptor;
import com.ls.aegis.biz.eoms.service.repair.IRepairApplyService;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.repair.IRepairRecordService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.biz.eoms.utils.GeoUtils;
import com.ls.aegis.biz.eoms.utils.WatermarkInfo;
import com.ls.aegis.biz.eoms.utils.WatermarkUtils;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;

/**
 * H5 客户报修 Controller。
 * 鉴权：H5TokenInterceptor（h5-token）；@SaIgnore 放行 Sa-Token。
 */
@SaIgnore
@RestController
@RequestMapping("/eoms/h5/repair")
public class H5RepairController
{
    private static final Logger log = LoggerFactory.getLogger(H5RepairController.class);

    @Autowired
    private IRepairApplyService repairApplyService;

    @Autowired
    private IRepairOrderService repairOrderService;

    @Autowired
    private IRepairRecordService repairRecordService;

    @Autowired
    private com.ls.aegis.biz.eoms.service.h5.CustomerConfirmationService customerConfirmationService;

    @Autowired
    private GeoUtils geoUtils;

    @Autowired
    private EomsPlatformFileHelper platformFileHelper;

    @Autowired
    private com.ls.aegis.biz.eoms.service.h5.H5AttachmentService h5AttachmentService;

    /**
     * H5 端带水印图片上传
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> uploadWithWatermark(HttpServletRequest request,
                                          MultipartFile file,
                                          String title, String taskName, String executor, String remark,
                                          String serviceAddress, String projectName, String workTime,
                                          String location, String longitude, String latitude)
    {
        try
        {
            Coordinates.optional(longitude, latitude);
            H5CustomerInfo authenticatedCustomer = customerSession(request);
            if (!WatermarkUtils.isSupportedImage(file))
            {
                return R.fail("1", "仅支持 jpg/jpeg/png 格式的图片");
            }
            log.debug("H5现场照片上传，客户身份校验完成");
            H5CustomerInfo customerInfo = (H5CustomerInfo) request.getAttribute(H5TokenInterceptor.ATTR_CUSTOMER_INFO);
            String customerName = customerInfo != null ? customerInfo.getCustomerName() : null;
            WatermarkInfo info = new WatermarkInfo();
            info.setTitle(customerName != null && !customerName.isEmpty() ? customerName
                    : (title != null && !title.isEmpty() ? title : "现场照片"));
            info.setTaskName(customerName != null && !customerName.isEmpty() ? customerName : taskName);
            info.setExecutor(executor);
            info.setRemark(remark);
            String addr = serviceAddress != null && !serviceAddress.isEmpty() ? serviceAddress : location;
            if ((addr == null || addr.isEmpty()) && longitude != null && !longitude.isEmpty()
                    && latitude != null && !latitude.isEmpty())
            {
                addr = geoUtils.reverseGeocode(longitude, latitude);
            }
            info.setServiceAddress(addr);
            info.setProjectName(projectName);
            info.setWorkTime(workTime);
            // 客户报修现场照片：时间标签为「报修时间」
            info.setWorkTimeLabel("报修时间");
            info.setLongitude(longitude);
            info.setLatitude(latitude);
            byte[] watermarkedBytes = WatermarkUtils.addWatermark(file.getBytes(), info);
            var uploaded = platformFileHelper.uploadWatermarkedJpeg(watermarkedBytes, file.getOriginalFilename(), "/eoms/repair");
            if (uploaded == null || uploaded.getId() == null)
            {
                return R.fail("1", "写入平台文件中心失败");
            }
            h5AttachmentService.bindNewUpload(authenticatedCustomer, Long.valueOf(uploaded.getId()));
            return R.ok(platformFileHelper.h5UploadPayload(uploaded, file.getOriginalFilename()));
        }
        catch (Exception e)
        {
            log.error("H5水印图片上传失败", e);
            String msg = e.getMessage();
            return R.fail("1", (msg != null && !msg.isBlank()) ? msg : "图片上传失败");
        }
    }

    /**
     * 提交报修申请。customerId 由拦截器注入，不做 @Validated 全量校验。
     */
    @Log(module = "H5报修-提交")
    @PostMapping("/apply")
    public R<Map<String, Object>> apply(HttpServletRequest request, @RequestBody RepairApply repairApply)
    {
        H5CustomerInfo session = customerSession(request);
        Long customerId = session.getCustomerId();
        if (customerId == null)
        {
            return R.fail("1", "客户身份信息缺失");
        }
        if (repairApply.getContactName() == null || repairApply.getContactName().isEmpty())
        {
            return R.fail("1", "联系人姓名不能为空");
        }
        if (repairApply.getContactPhone() == null || repairApply.getContactPhone().isEmpty())
        {
            return R.fail("1", "联系电话不能为空");
        }
        if (!repairApply.getContactPhone().matches("^1[3-9]\\d{9}$"))
        {
            return R.fail("1", "请输入正确的手机号码");
        }
        if (repairApply.getFaultType() == null || repairApply.getFaultType().isEmpty())
        {
            return R.fail("1", "故障类型不能为空");
        }
        if (repairApply.getFaultDesc() == null || repairApply.getFaultDesc().isEmpty())
        {
            return R.fail("1", "故障描述不能为空");
        }
        repairApply = h5AttachmentService.prepareNewApply(session, repairApply);
        int rows = repairApplyService.insertRepairApply(repairApply);
        if (rows > 0)
        {
            Map<String, Object> data = new HashMap<>();
            data.put("id", repairApply.getId());
            data.put("repairNo", repairApply.getRepairNo());
            return R.ok(data);
        }
        return R.fail("1", "提交报修失败");
    }

    /**
     * 当前客户报修列表
     */
    @GetMapping("/todos")
    public R<PageResp<RepairApply>> todos(HttpServletRequest request, @Valid PageQuery pageQuery)
    {
        H5CustomerInfo session = customerSession(request);
        return R.ok(PageQueryHelper.page(pageQuery, () -> customerConfirmationService.pendingConfirmations(
            session.getTenantId(), session.getCustomerId())));
    }

    @GetMapping("/list")
    public R<PageResp<RepairApply>> list(HttpServletRequest request, RepairApply query, @Valid PageQuery pageQuery)
    {
        Long customerId = (Long) request.getAttribute(H5TokenInterceptor.ATTR_CUSTOMER_ID);
        if (customerId == null)
        {
            return R.ok(PageQueryHelper.toPageResp(new ArrayList<>()));
        }
        query.setCustomerId(customerId);
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairApplyService.selectRepairApplyList(query)));
    }

    /**
     * 维修详情（报修 + 工单 + 记录）
     */
    @GetMapping("/detail/{id}")
    public R<Map<String, Object>> detail(HttpServletRequest request, @PathVariable Long id)
    {
        H5CustomerInfo session = customerSession(request);
        Long customerId = session.getCustomerId();
        if (customerId == null)
        {
            return R.fail("1", "用户未登录或会话已过期");
        }
        RepairApply apply = repairApplyService.selectRepairApplyById(id);
        if (apply == null || !customerId.equals(apply.getCustomerId()) || !session.getTenantId().equals(apply.getTenantId()))
        {
            return R.fail("1", "报修记录不存在或无权查看");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("apply", apply);
        RepairOrder orderQuery = new RepairOrder();
        orderQuery.setApplyId(id);
        List<RepairOrder> orders = repairOrderService.selectRepairOrderList(orderQuery);
        data.put("customerConfirmation", null);
        if (orders != null) orders = orders.stream().filter(o -> customerId.equals(o.getCustomerId()) && session.getTenantId().equals(o.getTenantId())).toList();
        if (orders != null && !orders.isEmpty())
        {
            RepairOrder order = orders.get(0);
            data.put("order", order);
            data.put("customerConfirmation", customerConfirmationService.forCustomer(session.getTenantId(),customerId,order.getId()));
            RepairRecord recordQuery = new RepairRecord();
            recordQuery.setRepairOrderId(order.getId());
            List<RepairRecord> records = repairRecordService.selectRepairRecordList(recordQuery);
            data.put("records", records == null ? List.of() : records.stream().filter(r -> session.getTenantId().equals(r.getTenantId())).toList());
        }
        return R.ok(data);
    }

    /**
     * 客户确认维修完成（持久化业务确认，不改变结算或财务状态）
     */
    @Log(module = "H5报修-确认完成")
    @PostMapping("/confirm")
    public R<com.ls.aegis.biz.eoms.model.resp.CustomerConfirmationResp> confirm(HttpServletRequest request, @Validated @RequestBody Map<String, Object> body)
    {
        H5CustomerInfo session = customerSession(request);
        Long customerId = session.getCustomerId();
        if (customerId == null)
        {
            return R.fail("1", "用户未登录或会话已过期");
        }
        Object orderIdObj = body.get("orderId");
        if (orderIdObj == null)
        {
            return R.fail("1", "工单ID不能为空");
        }
        Long orderId;
        try
        {
            orderId = Long.parseLong(String.valueOf(orderIdObj));
        }
        catch (NumberFormatException e)
        {
            return R.fail("1", "工单ID格式不合法");
        }
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(customerConfirmationService.confirm(session.getTenantId(), customerId, orderId, remark));
    }
    private H5CustomerInfo customerSession(HttpServletRequest request) {
        H5CustomerInfo info = (H5CustomerInfo) request.getAttribute(H5TokenInterceptor.ATTR_CUSTOMER_INFO);
        Object customerId = request.getAttribute(H5TokenInterceptor.ATTR_CUSTOMER_ID);
        Object tenantId = request.getAttribute(H5TokenInterceptor.ATTR_TENANT_ID);
        if (info == null || info.getCustomerId() == null || info.getTenantId() == null
                || !info.getCustomerId().equals(customerId) || !info.getTenantId().equals(tenantId))
            throw new com.ls.aegis.compat.common.exception.ServiceException("客户会话无效或已过期，请重新登录");
        return info;
    }

}
