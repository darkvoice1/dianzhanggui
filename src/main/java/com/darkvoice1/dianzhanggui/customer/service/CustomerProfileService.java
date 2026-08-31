package com.darkvoice1.dianzhanggui.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.customer.mapper.CustomerProfileMapper;
import com.darkvoice1.dianzhanggui.customer.model.CreateCustomerProfileRequest;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfile;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.springframework.stereotype.Service;

/** 提供当前商家客户档案的新增和详情查询能力。 */
@Service
public class CustomerProfileService {

    private final CustomerProfileMapper customerProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final MerchantMemberMapper merchantMemberMapper;

    /** 创建客户档案服务并注入所需的数据访问组件。 */
    public CustomerProfileService(CustomerProfileMapper customerProfileMapper, UserAccountMapper userAccountMapper,
                                  MerchantMemberMapper merchantMemberMapper) {
        this.customerProfileMapper = customerProfileMapper;
        this.userAccountMapper = userAccountMapper;
        this.merchantMemberMapper = merchantMemberMapper;
    }

    /** 在当前商家创建客户档案，可选择关联系统用户。 */
    public CustomerProfile create(CreateCustomerProfileRequest request) {
        Long merchantId = TenantContext.requireMerchantId();
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
        profile.setStatus("ACTIVE");
        customerProfileMapper.insert(profile);
        return profile;
    }

    /** 查询当前商家的客户档案，其他商家的同 ID 档案不可见。 */
    public CustomerProfile findById(Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        CustomerProfile profile = customerProfileMapper.selectOne(new LambdaQueryWrapper<CustomerProfile>()
                .eq(CustomerProfile::getId, id)
                .eq(CustomerProfile::getMerchantId, merchantId));
        if (profile == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return profile;
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
