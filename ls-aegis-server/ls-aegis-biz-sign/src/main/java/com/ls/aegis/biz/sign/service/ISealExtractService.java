package com.ls.aegis.biz.sign.service;

import java.util.List;
import java.util.Map;

/**
 * 印章提取服务接口
 */
public interface ISealExtractService {

    /**
     * 从图片中提取印章（默认方式）
     *
     * @param imageBytes 原始图片字节数组
     * @param userId 当前用户ID
     * @param userName 当前用户名
     * @return 提取结果，包含提取后的印章图片字节
     * @throws Exception 如果提取失败
     */
    Map<String, Object> extractSeal(byte[] imageBytes, Long userId, String userName) throws Exception;

    /**
     * 从图片中提取印章（按圆形提取，大小按类型生成）
     *
     * @param imageBytes 原始图片字节数组
     * @param userId 当前用户ID
     * @param userName 当前用户名
     * @param sealCategory 印章类型（用于确定目标尺寸）
     * @return 提取结果，包含提取后的圆形印章图片字节
     * @throws Exception 如果提取失败
     */
    Map<String, Object> extractSealCircle(byte[] imageBytes, Long userId, String userName, String sealCategory) throws Exception;

    /**
     * 保存提取的印章
     *
     * @param extractId 提取记录ID
     * @param sealName 印章名称
     * @param width 印章宽度
     * @param height 印章高度
     * @param userId 当前用户ID
     * @param userName 当前用户名
     * @return 保存后的印章ID
     * @throws Exception 如果保存失败
     */
    Long saveExtractedSeal(Long extractId, String sealName, Integer width, Integer height, Long userId, String userName) throws Exception;

    /**
     * 查询用户的印章提取记录列表
     *
     * @param params 查询参数（包含 userId 等）
     * @return 提取记录列表
     */
    List<Map<String, Object>> listExtractRecords(Map<String, Object> params);

    /**
     * 预览提取效果（默认方式）
     *
     * @param imageBytes 原始图片字节数组
     * @return 预览图片URL
     * @throws Exception 如果预览失败
     */
    String previewExtractedSeal(byte[] imageBytes) throws Exception;

    /**
     * 预览提取效果（按圆形提取）
     *
     * @param imageBytes 原始图片字节数组
     * @param sealCategory 印章类型
     * @return 预览图片URL
     * @throws Exception 如果预览失败
     */
    String previewExtractedSealCircle(byte[] imageBytes, String sealCategory) throws Exception;
}
