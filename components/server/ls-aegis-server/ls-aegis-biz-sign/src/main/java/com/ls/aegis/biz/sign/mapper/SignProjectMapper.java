package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignContractOption;
import com.ls.aegis.biz.sign.domain.SignProject;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 签章项目管理Mapper接口
 */
public interface SignProjectMapper {

    /**
     * 查询项目列表
     */
    List<SignProject> selectProjectList(SignProject project);

    /**
     * 查询项目详情
     */
    SignProject selectProjectById(Long projectId);

    /**
     * 查询项目详情（关联印章和成员名称）
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
     * 删除项目（逻辑删除）
     */
    int deleteProjectByIds(Long[] projectIds);

    /**
     * 批量插入项目印章关联
     */
    int batchInsertProjectSeal(@Param("projectId") Long projectId, @Param("sealIds") List<Long> sealIds);

    /**
     * 批量插入项目成员关联
     */
    int batchInsertProjectUser(@Param("projectId") Long projectId, @Param("userIds") Long[] userIds);

    /**
     * 删除项目印章关联
     */
    int deleteProjectSeal(Long projectId);

    /**
     * 删除项目成员关联
     */
    int deleteProjectUser(Long projectId);

    /**
     * 刷新过期项目状态
     */
    int refreshExpiredProjects();

    /**
     * 检查项目编号是否存在
     */
    int existsByProjectCode(String projectCode);

    /**
     * 查询当日项目编号最大流水（{@code likePattern} 形如 {@code XM260819%}）
     */
    Long selectMaxDailySeq(@Param("likePattern") String likePattern, @Param("substringPos") int substringPos);

    /**
     * 查询用户参与的项目列表
     */
    List<SignProject> selectProjectListByUserId(Long userId);

    /**
     * 查询项目配置的印章数量
     */
    int countProjectSeals(Long projectId);

    /**
     * 检查印章是否属于项目可用范围
     */
    int existsProjectSeal(@Param("projectId") Long projectId, @Param("sealId") Long sealId);

    /**
     * 按类型+ID取一份合同（含往来单位名称）
     */
    SignContractOption selectContractById(@Param("contractType") String contractType, @Param("contractId") Long contractId);

    /**
     * 可选合同列表（排除已被其他项目绑定的）
     */
    List<SignContractOption> selectContractOptions(@Param("contractType") String contractType,
                                                   @Param("keyword") String keyword,
                                                   @Param("excludeProjectId") Long excludeProjectId);

    /**
     * 合同是否已绑定其他项目
     */
    int countBoundContract(@Param("contractType") String contractType,
                           @Param("contractId") Long contractId,
                           @Param("excludeProjectId") Long excludeProjectId);

    /**
     * 清除合同上的所属项目
     */
    int clearContractProject(@Param("contractType") String contractType, @Param("contractId") Long contractId);

    /**
     * 写入合同上的所属项目（反向指针）
     */
    int setContractProject(@Param("contractType") String contractType,
                           @Param("contractId") Long contractId,
                           @Param("projectId") Long projectId);
}
