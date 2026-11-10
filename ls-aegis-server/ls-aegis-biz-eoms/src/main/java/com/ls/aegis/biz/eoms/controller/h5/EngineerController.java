package com.ls.aegis.biz.eoms.controller.h5;

import com.ls.aegis.biz.eoms.utils.Coordinates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.RepairRecord;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.repair.IRepairRecordService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.biz.eoms.utils.GeoUtils;
import com.ls.aegis.biz.eoms.utils.WatermarkInfo;
import com.ls.aegis.biz.eoms.utils.WatermarkUtils;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;

/**
 * 工程师端 Controller。
 * 登录走平台 /auth/login（Sa-Token + 国密），本类仅业务接口。
 */
@RestController
@RequestMapping("/eoms/engineer")
public class EngineerController
{
    private static final Logger log = LoggerFactory.getLogger(EngineerController.class);

    @Autowired
    private IRepairOrderService repairOrderService;

    @Autowired
    private IRepairRecordService repairRecordService;

    @Autowired
    private GeoUtils geoUtils;

    @Autowired
    private EomsPlatformFileHelper platformFileHelper;

    private Long currentUserId()
    {
        return UserContextHolder.getUserId();
    }

    private String currentDisplayName()
    {
        String nickname = UserContextHolder.getNickname();
        if (nickname != null && !nickname.isEmpty())
        {
            return nickname;
        }
        String username = UserContextHolder.getUsername();
        return username != null ? username : "工程师";
    }

    /**
     * 工程师任务列表
     */
    @SaCheckPermission("eoms:order:query")
    @GetMapping("/tasks")
    public R<PageResp<RepairOrder>> tasks(RepairOrder repairOrder, @Valid PageQuery pageQuery)
    {
        repairOrder.setEngineerId(currentUserId());
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairOrderService.selectRepairOrderList(repairOrder)));
    }

    /**
     * 任务详情
     */
    @SaCheckPermission("eoms:order:query")
    @GetMapping("/task/{id}")
    public R<Map<String, Object>> taskDetail(@PathVariable Long id)
    {
        RepairOrder order = repairOrderService.selectRepairOrderById(id);
        if (order == null)
        {
            return R.fail("1", "工单不存在");
        }
        RepairRecord recordQuery = new RepairRecord();
        recordQuery.setRepairOrderId(id);
        List<RepairRecord> records = repairRecordService.selectRepairRecordList(recordQuery);
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("records", records);
        return R.ok(data);
    }

    /**
     * 接单（WAIT_ACCEPT → ACCEPTED）
     */
    @Log(module = "工程师-接单")
    @SaCheckPermission("eoms:order:accept")
    @PostMapping("/accept/{id}")
    public R<Void> accept(@PathVariable Long id,
                             @RequestParam(required = false) String remark)
    {
        RepairOrder order = repairOrderService.selectRepairOrderById(id);
        if (order == null || !currentUserId().equals(order.getEngineerId()))
        {
            return R.fail("1", "工单不存在或您无权操作此工单");
        }
        if (repairOrderService.acceptOrder(id, remark) <= 0)
        {
            return R.fail("1", "接单失败");
        }
        return R.ok();
    }

    /**
     * 开始维修（ACCEPTED → PROCESSING），须带定位
     */
    @Log(module = "工程师-开始维修")
    @SaCheckPermission("eoms:order:start")
    @PostMapping("/start/{id}")
    public R<Void> start(@PathVariable Long id,
                            @RequestParam(required = false) String remark,
                            @RequestParam(required = false) String longitude,
                            @RequestParam(required = false) String latitude)
    {
        Coordinates.require(longitude, latitude);
        RepairOrder order = repairOrderService.selectRepairOrderById(id);
        if (order == null || !currentUserId().equals(order.getEngineerId()))
        {
            return R.fail("1", "工单不存在或您无权操作此工单");
        }
        if (longitude == null || longitude.isEmpty() || latitude == null || latitude.isEmpty())
        {
            return R.fail("1", "签到失败：未获取到定位信息，请开启定位后重试");
        }
        if (repairOrderService.startProcess(id, remark, longitude, latitude) <= 0)
        {
            return R.fail("1", "开始维修失败");
        }
        return R.ok();
    }

    /**
     * 工程师上传带水印维修图片
     */
    @SaCheckPermission("eoms:order:update")
    @PostMapping("/upload")
    public R<Map<String, Object>> uploadRepairPhoto(MultipartFile file,
                                        @RequestParam Long orderId,
                                        @RequestParam String photoType,
                                        @RequestParam(required = false) String longitude,
                                        @RequestParam(required = false) String latitude)
    {
        try
        {
            Coordinates.optional(longitude, latitude);
            if (!WatermarkUtils.isSupportedImage(file))
            {
                return R.fail("1", "仅支持 jpg/jpeg/png 格式的图片");
            }
            RepairOrder order = repairOrderService.selectRepairOrderById(orderId);
            if (order == null || !currentUserId().equals(order.getEngineerId()))
            {
                return R.fail("1", "工单不存在或您无权操作此工单");
            }
            String engineerName = currentDisplayName();
            boolean processing = "processing".equals(photoType);
            WatermarkInfo info = new WatermarkInfo();
            info.setTitle(engineerName + "（维修" + (processing ? "中" : "完成") + "）");
            info.setTaskName(order.getOrderNo());
            info.setExecutor(engineerName);
            // 维修中 → 维修时间；维修完成 → 维修完毕时间
            info.setWorkTimeLabel(processing ? "维修时间" : "维修完毕时间");
            if (longitude != null && !longitude.isEmpty() && latitude != null && !latitude.isEmpty())
            {
                String addr = geoUtils.reverseGeocode(longitude, latitude);
                info.setServiceAddress(addr);
            }
            info.setLongitude(longitude);
            info.setLatitude(latitude);
            byte[] watermarkedBytes = WatermarkUtils.addWatermark(file.getBytes(), info);
            var uploaded = platformFileHelper.uploadWatermarkedJpeg(watermarkedBytes, file.getOriginalFilename(), "/eoms/repair");
            if (uploaded == null || uploaded.getId() == null)
            {
                return R.fail("1", "写入平台文件中心失败");
            }
            Map<String, Object> data = platformFileHelper.h5UploadPayload(uploaded, file.getOriginalFilename());
            data.put("photoType", photoType);
            return R.ok(data);
        }
        catch (Exception e)
        {
            log.error("工程师水印照片上传失败", e);
            return R.fail("1", e.getMessage());
        }
    }

    /**
     * 提交维修记录
     */
    @Log(module = "工程师-提交维修记录")
    @SaCheckPermission("eoms:order:update")
    @PostMapping("/record")
    public R<Void> submitRecord(@Validated @RequestBody RepairRecord repairRecord)
    {
        if (repairRecord.getRepairOrderId() == null)
        {
            return R.fail("1", "工单ID不能为空");
        }
        RepairOrder order = repairOrderService.selectRepairOrderById(repairRecord.getRepairOrderId());
        if (order == null || !currentUserId().equals(order.getEngineerId()))
        {
            return R.fail("1", "工单不存在或您无权操作此工单");
        }
        repairRecord.setCreateBy(UserContextHolder.getUsername());
        if (repairRecordService.insertRepairRecord(repairRecord) <= 0)
        {
            return R.fail("1", "提交维修记录失败");
        }
        return R.ok();
    }

    /**
     * 完成维修（PROCESSING → WAIT_SETTLEMENT），须带定位
     */
    @Log(module = "工程师-完成维修")
    @SaCheckPermission("eoms:order:finish")
    @PostMapping("/finish/{id}")
    public R<Void> finish(@PathVariable Long id,
                             @RequestParam(required = false) String remark,
                             @RequestParam(required = false) String longitude,
                             @RequestParam(required = false) String latitude)
    {
        Coordinates.require(longitude, latitude);
        RepairOrder order = repairOrderService.selectRepairOrderById(id);
        if (order == null || !currentUserId().equals(order.getEngineerId()))
        {
            return R.fail("1", "工单不存在或您无权操作此工单");
        }
        if (longitude == null || longitude.isEmpty() || latitude == null || latitude.isEmpty())
        {
            return R.fail("1", "签退失败：未获取到定位信息，请开启定位后重试");
        }
        if (repairOrderService.submitSettlement(id, remark, longitude, latitude) <= 0)
        {
            return R.fail("1", "完成维修失败");
        }
        return R.ok();
    }

    /**
     * 个人中心 - 任务统计
     */
    @SaCheckPermission("eoms:order:query")
    @GetMapping("/profile")
    public R<Map<String, Object>> profile()
    {
        RepairOrder query = new RepairOrder();
        query.setEngineerId(currentUserId());
        List<RepairOrder> allOrders = repairOrderService.selectRepairOrderList(query);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allOrders.size());
        stats.put("waitAccept", allOrders.stream().filter(o -> "WAIT_ACCEPT".equals(o.getStatus())).count());
        stats.put("accepted", allOrders.stream().filter(o -> "ACCEPTED".equals(o.getStatus())).count());
        stats.put("processing", allOrders.stream().filter(o -> "PROCESSING".equals(o.getStatus())).count());
        stats.put("waitSettlement", allOrders.stream().filter(o -> "WAIT_SETTLEMENT".equals(o.getStatus())).count());
        stats.put("finished", allOrders.stream().filter(o -> "FINISHED".equals(o.getStatus())).count());
        stats.put("closed", allOrders.stream().filter(o -> "CLOSED".equals(o.getStatus())).count());
        return R.ok(stats);
    }
}
