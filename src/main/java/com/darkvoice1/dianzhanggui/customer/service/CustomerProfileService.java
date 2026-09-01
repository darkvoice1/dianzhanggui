package com.darkvoice1.dianzhanggui.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.common.page.PageResult;
import com.darkvoice1.dianzhanggui.common.page.PageResults;
import com.darkvoice1.dianzhanggui.customer.mapper.CustomerProfileMapper;
import com.darkvoice1.dianzhanggui.customer.model.CreateCustomerProfileRequest;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfile;
import com.darkvoice1.dianzhanggui.customer.model.UpdateCustomerProfileRequest;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfileQuery;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.permission.service.PermissionResolver;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.springframework.stereotype.Service;

/** 提供当前商家客户档案的新增、查询、编辑和停用能力。 */
@Service
public class CustomerProfileService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INACTIVE_STATUS = "INACTIVE";
    private static final String PROFILE_MANAGE_PERMISSION = "PROFILE_MANAGE";

    private final CustomerProfileMapper customerProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final MerchantMemberMapper merchantMemberMapper;
    private final PermissionResolver permissionResolver;

    /** 创建客户档案服务并注入所需的数据访问组件。 */
    public CustomerProfileService(CustomerProfileMapper customerProfileMapper, UserAccountMapper userAccountMapper,
                                  MerchantMemberMapper merchantMemberMapper, PermissionResolver permissionResolver) {
        this.customerProfileMapper = customerProfileMapper;
        this.userAccountMapper = userAccountMapper;
        this.merchantMemberMapper = merchantMemberMapper;
        this.permissionResolver = permissionResolver;
    }

    /** 在当前商家创建客户档案，可选择关联系统用户。 */
    public CustomerProfile create(Long operatorUserId, CreateCustomerProfileRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        if (request.userId() != null) {
            verifyUserBelongsToMerchant(request.userId(), merchantId);
            CustomerProfile existing = findByUserId(merchantId, request.userId());
            if (existing != null) {
                throw new BusinessException(ErrorCode.CUSTOMER_PROFILE_ALREADY_EXISTS);
            }
        }

        CustomerProfile profile = new CustomerProfile();
        profile.setMerchantId(merchantId);
        profile.setUserId(request.userId());
        profile.setName(request.name().trim());
        profile.setPhone(normalize(request.phone()));
        profile.setStatus(ACTIVE_STATUS);
        customerProfileMapper.insert(profile);
        return profile;
    }

    /** 在当前商家编辑客户档案，停用档案不能继续编辑。 */
    public CustomerProfile update(Long operatorUserId, Long id, UpdateCustomerProfileRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        CustomerProfile profile = findByMerchantAndId(merchantId, id);
        ensureActive(profile);
        profile.setName(request.name().trim());
        profile.setPhone(normalize(request.phone()));
        customerProfileMapper.updateById(profile);
        return profile;
    }

    /** 停用当前商家的客户档案。 */
    public void deactivate(Long operatorUserId, Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        CustomerProfile profile = findByMerchantAndId(merchantId, id);
        if (INACTIVE_STATUS.equals(profile.getStatus())) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_INACTIVE);
        }
        profile.setStatus(INACTIVE_STATUS);
        customerProfileMapper.updateById(profile);
    }

    /** 查询当前商家的客户档案，其他商家的同 ID 档案不可见。 */
    public CustomerProfile findById(Long operatorUserId, Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        return findByMerchantAndId(merchantId, id);
    }

    /** 分页查询当前商家的客户档案。 */
    public PageResult<CustomerProfile> page(Long operatorUserId, CustomerProfileQuery query) {
        Long merchantId = TenantContext.requireMerchantId();
        permissionResolver.requirePermission(operatorUserId, PROFILE_MANAGE_PERMISSION);
        LambdaQueryWrapper<CustomerProfile> wrapper = new LambdaQueryWrapper<CustomerProfile>()
                .eq(CustomerProfile::getMerchantId, merchantId)
                .orderByDesc(CustomerProfile::getId);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(CustomerProfile::getName, keyword)
                    .or().like(CustomerProfile::getPhone, keyword));
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(CustomerProfile::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (query.getCreatedFrom() != null) {
            wrapper.ge(CustomerProfile::getCreatedAt, query.getCreatedFrom());
        }
        if (query.getCreatedTo() != null) {
            wrapper.le(CustomerProfile::getCreatedAt, query.getCreatedTo());
        }
        return PageResults.from(customerProfileMapper.selectPage(
                new Page<>(query.currentPage(), query.getSize()), wrapper));
    }

    /** 按商家和档案主键查询客户档案。 */
    private CustomerProfile findByMerchantAndId(Long merchantId, Long id) {
        CustomerProfile profile = customerProfileMapper.selectOne(new LambdaQueryWrapper<CustomerProfile>()
                .eq(CustomerProfile::getId, id)
                .eq(CustomerProfile::getMerchantId, merchantId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return profile;
    }

    /** 校验客户档案仍处于可编辑状态。 */
    private void ensureActive(CustomerProfile profile) {
        if (INACTIVE_STATUS.equals(profile.getStatus())) {
            throw new BusinessException(ErrorCode.PROFILE_INACTIVE);
        }
    }

    /** 校验关联用户属于当前商家。 */
    private void verifyUserBelongsToMerchant(Long userId, Long merchantId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        MerchantMember member = merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                .eq(MerchantMember::getUserId, user.getId())
                .eq(MerchantMember::getMerchantId, merchantId));
        if (member == null) {
            throw new BusinessException(ErrorCode.MERCHANT_ACCESS_DENIED);
        }
    }

    /** 按当前商家和关联用户查询已有档案。 */
    private CustomerProfile findByUserId(Long merchantId, Long userId) {
        return customerProfileMapper.selectOne(new LambdaQueryWrapper<CustomerProfile>()
                .eq(CustomerProfile::getMerchantId, merchantId)
                .eq(CustomerProfile::getUserId, userId));
    }

    /** 将空白电话统一保存为空值。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
