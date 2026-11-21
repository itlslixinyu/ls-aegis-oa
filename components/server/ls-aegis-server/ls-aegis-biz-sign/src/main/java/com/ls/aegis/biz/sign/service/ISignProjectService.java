package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignContractOption;
import com.ls.aegis.biz.sign.domain.SignProject;

import java.util.List;

/**
 * 签章项目管理服务接口
 */
public interface ISignProjectService {

    /**
     * 查询项目列表
     */
    List<SignProject> selectProjectList(SignProject project);

    /**
     * 查询项目详情
     */
    SignProject selectProjectDetail(Long projectId);

    /**
     * 新增项目
     */
    int insertProject(SignProject project);

    /**
     * 修改项目
     */
    int updateProject(SignProject project);

    /**
     * 删除项目
     */
    int deleteProjectByIds(Long[] projectIds);

    /**
     * 按开始/结束时间刷新项目状态：
     * 未开始且已到开始时间→进行中；未开始/进行中且已到结束时间→已完成。
     */
    int refreshExpiredProjects();

    /**
     * 审核项目（通过/驳回）。
     * 通过时按开始、结束时间即时落为未开始/进行中/已完成。
     */
    int reviewProject(Long projectId, String status);

    /**
     * 查询用户参与的项目列表
     */
    List<SignProject> selectProjectListByUserId(Long userId);

    /**
     * 列出可供绑定的合同；contractType 为空时返回销售+采购+维修全部可选合同
     */
    List<SignContractOption> listContractOptions(String contractType, String keyword, Long excludeProjectId);
}
