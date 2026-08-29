package com.darkvoice1.dianzhanggui.common.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.common.ApiResponse;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMemberMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 校验当前商家请求头，并把已验证的商家标识写入请求上下文。 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final MerchantMemberMapper merchantMemberMapper;
    private final ObjectMapper objectMapper;

    /** 创建租户上下文过滤器并注入成员关系查询组件。 */
    public TenantContextFilter(MerchantMemberMapper merchantMemberMapper, ObjectMapper objectMapper) {
        this.merchantMemberMapper = merchantMemberMapper;
        this.objectMapper = objectMapper;
    }

    /** 在 JWT 验证后校验用户是否属于请求头指定的商家。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String merchantIdHeader = request.getHeader(TenantContext.MERCHANT_ID_HEADER);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (merchantIdHeader == null || merchantIdHeader.isBlank()
                    || !(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long merchantId = parseMerchantId(merchantIdHeader, response);
            if (merchantId == null) {
                return;
            }

            Long userId = Long.valueOf(jwtAuthentication.getToken().getSubject());
            MerchantMember member = merchantMemberMapper.selectOne(new LambdaQueryWrapper<MerchantMember>()
                    .eq(MerchantMember::getUserId, userId)
                    .eq(MerchantMember::getMerchantId, merchantId));
            if (member == null) {
                log.warn("event=tenant_access_denied user_id={} merchant_id={} operation={} path={}",
                        userId, merchantId, request.getMethod(), request.getRequestURI());
                writeFailure(response, HttpStatus.FORBIDDEN, ErrorCode.MERCHANT_ACCESS_DENIED);
                return;
            }

            TenantContext.setMerchantId(merchantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /** 解析请求头中的商家标识。 */
    private Long parseMerchantId(String merchantIdHeader, HttpServletResponse response) throws IOException {
        try {
            Long merchantId = Long.valueOf(merchantIdHeader);
            if (merchantId <= 0) {
                throw new NumberFormatException();
            }
            return merchantId;
        } catch (NumberFormatException exception) {
            writeFailure(response, HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_TYPE_ERROR);
            return null;
        }
    }

    /** 使用统一响应格式写出过滤器阶段的错误。 */
    private void writeFailure(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(errorCode, null));
    }
}
