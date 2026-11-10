package com.ls.aegis.biz.eoms.service.repair;

import java.util.List;
import java.util.Map;
import com.ls.aegis.biz.eoms.domain.RepairOrder;

/**
 * RepairOrder Service接口
 *
 * @author EOMS Generator
 */
public interface IRepairOrderService
{
    /**
     * 查询RepairOrder
     *
     * @param id 主键
     * @return RepairOrder
     */
    public RepairOrder selectRepairOrderById(Long id);

    /**
     * 查询RepairOrder列表
     *
     * @param repairOrder RepairOrder
     * @return RepairOrder集合
     */
    public List<RepairOrder> selectRepairOrderList(RepairOrder repairOrder);

    /**
     * 查询工程师角色用户选项（用于工单派单下拉选择）
     * 仅返回具有 eoms_engineer 角色且状态正常的用户
     *
     * @return List&lt;Map&lt;id, nickName&gt;&gt;
     */
    public List<Map<String, Object>> selectEngineerOptions();

    /**
     * 新增RepairOrder
     * 
     * @param repairOrder RepairOrder
     * @return 结果
     */
    public int insertRepairOrder(RepairOrder repairOrder);

    /**
     * 修改RepairOrder
     * 
     * @param repairOrder RepairOrder
     * @return 结果
     */
    public int updateRepairOrder(RepairOrder repairOrder);

    /**
     * 批量删除RepairOrder
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairOrderByIds(Long[] ids);

    /**
     * 删除RepairOrder信息
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairOrderById(Long id);

    /**
     * 派单（分配工程师）
     * 工单状态不变（WAIT_ACCEPT），仅设置工程师ID
     *
     * @param orderId    工单ID
     * @param engineerId 工程师用户ID
     * @param remark     备注（可选）
     * @return 结果
     */
    public int assignEngineer(Long orderId, Long engineerId, String remark);

    /**
     * 接单（WAIT_ACCEPT → ACCEPTED）
     *
     * @param orderId 工单ID
     * @param remark  备注（可选）
     * @return 结果
     */
    public int acceptOrder(Long orderId, String remark);

    /**
     * 开始维修（ACCEPTED → PROCESSING）
     *
     * @param orderId 工单ID
     * @param remark  备注（可选）
     * @return 结果
     */
    public int startProcess(Long orderId, String remark);

    /**
     * 开始维修（ACCEPTED → PROCESSING）含签到位置
     * 工程师签到时记录经纬度，后端逆地理编码为中文地址一并入库
     *
     * @param orderId   工单ID
     * @param remark    备注（可选）
     * @param longitude 签到经度
     * @param latitude  签到纬度
     * @return 结果
     */
    public int startProcess(Long orderId, String remark, String longitude, String latitude);

    /**
     * 提交结算（PROCESSING → WAIT_SETTLEMENT）
     * 维修完成，提交进入结算环节
     *
     * @param orderId 工单ID
     * @param remark  备注（可选）
     * @return 结果
     */
    public int submitSettlement(Long orderId, String remark);

    /**
     * 提交结算（PROCESSING → WAIT_SETTLEMENT）含签退位置
     * 工程师签退时记录经纬度，后端逆地理编码为中文地址一并入库
     *
     * @param orderId   工单ID
     * @param remark    备注（可选）
     * @param longitude 签退经度
     * @param latitude  签退纬度
     * @return 结果
     */
    public int submitSettlement(Long orderId, String remark, String longitude, String latitude);

    /**
     * 关闭工单（FINISHED → CLOSED）
     *
     * @param orderId 工单ID
     * @param remark  备注（可选）
     * @return 结果
     */
    public int closeOrder(Long orderId, String remark);

    /**
     * 结算确认完成工单（WAIT_SETTLEMENT → FINISHED）
     * <p>内部封装：状态机合法性校验 + 工单流转操作日志，由结算模块统一调用，
     * 禁止外部直接用 mapper 更新工单状态绕过校验与日志。</p>
     *
     * @param orderId  工单ID
     * @param updateBy 当前操作人（可为空，空时使用登录上下文取）
     * @param remark   备注（可选）
     * @return 结果
     */
    public int finishSettlement(Long orderId, String updateBy, String remark);
    /** 仅明确反确认且全部库存/财务已补偿的待结算工单可撤销关闭。 */
    int cancelAfterReversal(Long orderId, String reason);

    int reverseSettlement(Long orderId, String operator, String reason);
}
