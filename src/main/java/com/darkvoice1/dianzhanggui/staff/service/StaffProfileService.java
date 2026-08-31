package com.darkvoice1.dianzhanggui.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.staff.mapper.StaffProfileMapper;
import com.darkvoice1.dianzhanggui.staff.model.CreateStaffProfileRequest;
import com.darkvoice1.dianzhanggui.staff.model.StaffProfile;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.springframework.stereotype.Service;

/** 提供当前商家人员档案的新增和详情查询能力。 */
@Service
public class StaffProfileService {

    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final String DEFAULT_POSITION = "GENERAL";

    private final StaffProfileMapper staffProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final MerchantMemberMapper merchantMemberMapper;

    /** 创建人员档案服务并注入所需的数据访问组件。 */
    public StaffProfileService(StaffProfileMapper staffProfileMapper, UserAccountMapper userAccountMapper,
                               MerchantMemberMapper merchantMemberMapper) {
        this.staffProfileMapper = staffProfileMapper;
        this.userAccountMapper = userAccountMapper;
        this.merchantMemberMapper = merchantMemberMapper;
    }

    /** 在当前商家为员工创建人员档案。 */
    public StaffProfile create(CreateStaffProfileRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
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
        profile.setStatus("ACTIVE");
        staffProfileMapper.insert(profile);
        return profile;
    }

    /** 查询当前商家的人员档案，其他商家的同 ID 档案不可见。 */
    public StaffProfile findById(Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        StaffProfile profile = staffProfileMapper.selectOne(new LambdaQueryWrapper<StaffProfile>()
                .eq(StaffProfile::getId, id)
                .eq(StaffProfile::getMerchantId, merchantId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return profile;
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
