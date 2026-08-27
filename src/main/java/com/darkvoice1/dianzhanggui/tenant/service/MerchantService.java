package com.darkvoice1.dianzhanggui.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.darkvoice1.dianzhanggui.tenant.model.MerchantSummaryResponse;
import com.darkvoice1.dianzhanggui.tenant.model.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 提供商家和首个门店的创建能力。 */
@Service
public class MerchantService {

    private static final String OWNER_ROLE = "OWNER";
    private static final String MEMBER_ROLE = "MEMBER";

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

    /** 为当前用户创建指定商家的普通成员关系。 */
    public void joinMerchant(Long userId, Long merchantId) {
        verifyUserExists(userId);
        findMerchant(merchantId);
        if (findMember(userId, merchantId) != null) {
            throw new BusinessException(ErrorCode.MERCHANT_MEMBER_ALREADY_EXISTS);
        }

        MerchantMember member = new MerchantMember();
        member.setMerchantId(merchantId);
        member.setUserId(userId);
        member.setRole(MEMBER_ROLE);
        merchantMemberMapper.insert(member);
    }

    /** 查询当前用户所属商家及其在商家中的角色。 */
    public List<MerchantSummaryResponse> getMyMerchants(Long userId) {
        verifyUserExists(userId);
        return merchantMemberMapper.selectList(new LambdaQueryWrapper<MerchantMember>()
                        .eq(MerchantMember::getUserId, userId))
                .stream()
                .map(this::toMerchantSummary)
                .toList();
    }

    /** 校验当前用户的成员关系，并返回被选中的商家。 */
    public MerchantSummaryResponse switchMerchant(Long userId, Long merchantId) {
        verifyUserExists(userId);
        MerchantMember member = findMember(userId, merchantId);
        if (member == null) {
            throw new BusinessException(ErrorCode.MERCHANT_ACCESS_DENIED);
        }
        return toMerchantSummary(member);
    }

    /** 查询用户在指定商家的成员关系。 */
    private MerchantMember findMember(Long userId, Long merchantId) {
        return merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                .eq(MerchantMember::getUserId, userId)
                .eq(MerchantMember::getMerchantId, merchantId));
    }

    /** 查询商家，不存在时返回统一的资源不存在错误。 */
    private Merchant findMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return merchant;
    }

    /** 将成员关系和商家信息合成为接口返回数据。 */
    private MerchantSummaryResponse toMerchantSummary(MerchantMember member) {
        Merchant merchant = findMerchant(member.getMerchantId());
        return new MerchantSummaryResponse(merchant.getId(), merchant.getName(), member.getRole());
    }

    /** 校验 JWT 中的用户仍然存在。 */
    private void verifyUserExists(Long userId) {
        if (userAccountMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** 将空白地址统一保存为空值。 */
    private String normalizeAddress(String address) {
        return address == null || address.isBlank() ? null : address.trim();
    }
}
