package com.darkvoice1.dianzhanggui.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.common.page.PageResult;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.permission.service.PermissionResolver;
import com.darkvoice1.dianzhanggui.staff.mapper.StaffProfileMapper;
import com.darkvoice1.dianzhanggui.staff.model.CreateStaffProfileRequest;
import com.darkvoice1.dianzhanggui.staff.model.StaffProfile;
import com.darkvoice1.dianzhanggui.staff.model.UpdateStaffProfileRequest;
import com.darkvoice1.dianzhanggui.staff.model.StaffProfileQuery;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.springframework.stereotype.Service;

/** 提供当前商家人员档案的新增、查询、编辑和停用能力。 */
@Service
public class StaffProfileService {

    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final String DEFAULT_POSITION = "GENERAL";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INACTIVE_STATUS = "INACTIVE";
    private static final String PROFILE_MANAGE_PERMISSION = "PROFILE_MANAGE";

    private final StaffProfileMapper staffProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final MerchantMemberMapper merchantMemberMapper;
    private final PermissionResolver permissionResolver;

    /** 创建人员档案服务并注入所需的数据访问组件。 */
    public StaffProfileService(StaffProfileMapper staffProfileMapper, UserAccountMapper userAccountMapper,
                               MerchantMemberMapper merchantMemberMapper, PermissionResolver permissionResolver) {
        this.staffProfileMapper = staffProfileMapper;
        this.userAccountMapper = userAccountMapper;
        this.merchantMemberMapper = merchantMemberMapper;
        this.permissionResolver = permissionResolver;
    }

    /** 在当前商家为员工创建人员档案。 */
    public StaffProfile create(Long operatorUserId, CreateStaffProfileRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        UserAccount user = userAccountMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        MerchantMember member = findMember(merchantId, request.userId());
        if (member == null) {
            throw new BusinessException(ErrorCode.MERCHANT_ACCESS_DENIED);
        }
        if (!EMPLOYEE_ROLE.equals(member.getRole())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有员工角色可以创建人员档案");
        }
        if (findByUserId(merchantId, request.userId()) != null) {
            throw new BusinessException(ErrorCode.STAFF_PROFILE_ALREADY_EXISTS);
        }

        StaffProfile profile = new StaffProfile();
        profile.setMerchantId(merchantId);
        profile.setUserId(request.userId());
        profile.setName(request.name().trim());
        profile.setPhone(normalize(request.phone()));
        profile.setPosition(request.position() == null || request.position().isBlank()
                ? DEFAULT_POSITION : request.position().trim());
        profile.setStatus(ACTIVE_STATUS);
        staffProfileMapper.insert(profile);
        return profile;
    }

    /** 在当前商家编辑人员档案，停用档案不能继续编辑。 */
    public StaffProfile update(Long operatorUserId, Long id, UpdateStaffProfileRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        StaffProfile profile = findByMerchantAndId(merchantId, id);
        ensureActive(profile);
        profile.setName(request.name().trim());
        profile.setPhone(normalize(request.phone()));
        profile.setPosition(request.position() == null || request.position().isBlank()
                ? DEFAULT_POSITION : request.position().trim());
        staffProfileMapper.updateById(profile);
        return profile;
    }

    /** 停用当前商家的人员档案。 */
    public void deactivate(Long operatorUserId, Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        StaffProfile profile = findByMerchantAndId(merchantId, id);
        if (INACTIVE_STATUS.equals(profile.getStatus())) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_INACTIVE);
        }
        profile.setStatus(INACTIVE_STATUS);
        staffProfileMapper.updateById(profile);
    }

    /** 查询当前商家的人员档案，其他商家的同 ID 档案不可见。 */
    public StaffProfile findById(Long operatorUserId, Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        return findByMerchantAndId(merchantId, id);
    }

    /** 分页查询当前商家的人员档案。 */
    public PageResult<StaffProfile> page(Long operatorUserId, StaffProfileQuery query) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        LambdaQueryWrapper<StaffProfile> wrapper = new LambdaQueryWrapper<StaffProfile>()
                .eq(StaffProfile::getMerchantId, merchantId)
                .orderByDesc(StaffProfile::getId);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(StaffProfile::getName, keyword)
                    .or().like(StaffProfile::getPhone, keyword)
                    .or().like(StaffProfile::getPosition, keyword));
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(StaffProfile::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (query.getCreatedFrom() != null) {
            wrapper.ge(StaffProfile::getCreatedAt, query.getCreatedFrom());
        }
        if (query.getCreatedTo() != null) {
            wrapper.le(StaffProfile::getCreatedAt, query.getCreatedTo());
        }
        return com.darkvoice1.dianzhanggui.common.page.PageResults.from(staffProfileMapper.selectPage(
                new Page<>(query.currentPage(), query.getSize()), wrapper));
    }

    /** 按商家和档案主键查询人员档案。 */
    private StaffProfile findByMerchantAndId(Long merchantId, Long id) {
        StaffProfile profile = staffProfileMapper.selectOne(new LambdaQueryWrapper<StaffProfile>()
                .eq(StaffProfile::getId, id)
                .eq(StaffProfile::getMerchantId, merchantId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return profile;
    }

    /** 校验人员档案仍处于可编辑状态。 */
    private void ensureActive(StaffProfile profile) {
        if (INACTIVE_STATUS.equals(profile.getStatus())) {
            throw new BusinessException(ErrorCode.PROFILE_INACTIVE);
        }
    }

    /** 查询当前商家指定用户的成员关系。 */
    private MerchantMember findMember(Long merchantId, Long userId) {
        return merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                .eq(MerchantMember::getMerchantId, merchantId)
                .eq(MerchantMember::getUserId, userId));
    }

    /** 查询当前商家指定用户已有的人员档案。 */
    private StaffProfile findByUserId(Long merchantId, Long userId) {
        return staffProfileMapper.selectOne(new LambdaQueryWrapper<StaffProfile>()
                .eq(StaffProfile::getMerchantId, merchantId)
                .eq(StaffProfile::getUserId, userId));
    }

    /** 将空白电话统一保存为空值。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
