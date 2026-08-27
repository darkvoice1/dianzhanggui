package com.darkvoice1.dianzhanggui.tenant.service;

import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMapper;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.mapper.StoreMapper;
import com.darkvoice1.dianzhanggui.tenant.model.CreateMerchantRequest;
import com.darkvoice1.dianzhanggui.tenant.model.Merchant;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantCreationResponse;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import com.darkvoice1.dianzhanggui.tenant.model.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 提供商家和首个门店的创建能力。 */
@Service
public class MerchantService {

    private static final String OWNER_ROLE = "OWNER";

    private final UserAccountMapper userAccountMapper;
    private final MerchantMapper merchantMapper;
    private final StoreMapper storeMapper;
    private final MerchantMemberMapper merchantMemberMapper;

    /** 创建商家服务并注入所需的数据访问组件。 */
    public MerchantService(UserAccountMapper userAccountMapper, MerchantMapper merchantMapper,
                           StoreMapper storeMapper, MerchantMemberMapper merchantMemberMapper) {
        this.userAccountMapper = userAccountMapper;
        this.merchantMapper = merchantMapper;
        this.storeMapper = storeMapper;
        this.merchantMemberMapper = merchantMemberMapper;
    }

    /** 为当前登录用户创建商家、首个门店及创建者成员关系。 */
    @Transactional
    public MerchantCreationResponse createMerchant(Long userId, CreateMerchantRequest request) {
        if (userAccountMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Merchant merchant = new Merchant();
        merchant.setName(request.merchantName().trim());
        merchantMapper.insert(merchant);

        Store store = new Store();
        store.setMerchantId(merchant.getId());
        store.setName(request.firstStoreName().trim());
        store.setAddress(normalizeAddress(request.firstStoreAddress()));
        storeMapper.insert(store);

        MerchantMember member = new MerchantMember();
        member.setMerchantId(merchant.getId());
        member.setUserId(userId);
        member.setRole(OWNER_ROLE);
        merchantMemberMapper.insert(member);

        return new MerchantCreationResponse(merchant.getId(), merchant.getName(), store.getId(), store.getName());
    }

    /** 将空白地址统一保存为空值。 */
    private String normalizeAddress(String address) {
        return address == null || address.isBlank() ? null : address.trim();
    }
}
