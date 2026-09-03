package com.darkvoice1.dianzhanggui.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.availability.mapper.ProductAvailabilityMapper;
import com.darkvoice1.dianzhanggui.availability.model.ProductAvailability;
import com.darkvoice1.dianzhanggui.catalog.mapper.ProductMapper;
import com.darkvoice1.dianzhanggui.catalog.model.Product;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
import com.darkvoice1.dianzhanggui.customer.mapper.CustomerProfileMapper;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfile;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.reservation.mapper.ReservationMapper;
import com.darkvoice1.dianzhanggui.reservation.model.CreateReservationRequest;
import com.darkvoice1.dianzhanggui.reservation.model.Reservation;
import com.darkvoice1.dianzhanggui.reservation.service.ReservationService;
import com.darkvoice1.dianzhanggui.tenant.mapper.MerchantMapper;
import com.darkvoice1.dianzhanggui.tenant.model.Merchant;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证商品预约在真实多线程竞争下不会超卖或重复创建有效预约。 */
@SpringBootTest
@Testcontainers
class ReservationConcurrencyIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ReservationConcurrencyIntegrationTest.class);
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String ON_SALE_STATUS = "ON_SALE";
    private static final String OPEN_STATUS = "OPEN";
    private static final String RESERVED_STATUS = "RESERVED";
    private static final String PRODUCT_TYPE = "PRODUCT";
    private static final String TEST_PASSWORD_HASH = "not-used-by-concurrency-test";

    /** 启动并发集成测试使用的临时 PostgreSQL 容器。 */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("dianzhanggui_reservation_concurrency_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductAvailabilityMapper productAvailabilityMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private CustomerProfileMapper customerProfileMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    /** 将临时 PostgreSQL 容器连接信息注入 Spring 配置。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** 验证多位顾客并发抢有限数量时，成功数不超过商品可预约数量。 */
    @Test
    void shouldNotOversellWhenDifferentCustomersReserveConcurrently() throws Exception {
        int capacity = 5;
        int requestCount = 20;
        Long merchantId = createMerchant();
        ProductAvailability availability = createAvailableProduct(merchantId, capacity);
        List<Long> customerUserIds = createCustomers(merchantId, requestCount);

        ConcurrencyMetrics metrics = reserveConcurrently(merchantId, availability.getId(), customerUserIds);
        logMetrics("different-customers-limited-capacity", metrics);

        assertEquals(capacity, metrics.successCount());
        assertEquals(requestCount - capacity, metrics.failureCount());
        assertEquals(requestCount - capacity, metrics.errorCount(ErrorCode.PRODUCT_AVAILABILITY_SOLD_OUT));
        assertEquals(0, currentRemainingCapacity(availability.getId()));
        assertEquals((long) capacity, activeReservationCount(merchantId, availability.getId()));
    }

    /** 验证同一顾客并发请求时，最终只保留一条有效预约。 */
    @Test
    void shouldKeepOneReservationWhenSameCustomerReservesConcurrently() throws Exception {
        int requestCount = 20;
        int capacity = 5;
        Long merchantId = createMerchant();
        ProductAvailability availability = createAvailableProduct(merchantId, capacity);
        Long customerUserId = createCustomer(merchantId, 1);
        List<Long> repeatedUserIds = java.util.Collections.nCopies(requestCount, customerUserId);

        ConcurrencyMetrics metrics = reserveConcurrently(merchantId, availability.getId(), repeatedUserIds);
        logMetrics("same-customer-duplicate-reservation", metrics);

        assertEquals(1, metrics.successCount());
        assertEquals(requestCount - 1, metrics.failureCount());
        assertEquals(requestCount - 1, metrics.errorCount(ErrorCode.RESERVATION_ALREADY_EXISTS));
        assertEquals(capacity - 1, currentRemainingCapacity(availability.getId()));
        assertEquals(1L, activeReservationCount(merchantId, availability.getId()));
    }

    /** 在多个线程中同步发起预约请求，并统计执行结果和耗时。 */
    private ConcurrencyMetrics reserveConcurrently(Long merchantId, Long availabilityId, List<Long> userIds)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ReservationAttempt>> futures = new ArrayList<>();
            for (Long userId : userIds) {
                futures.add(executor.submit(() -> reserveWhenReady(merchantId, availabilityId, userId, ready, start)));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "并发预约线程未在预期时间内准备完成");
            long startedAt = System.nanoTime();
            start.countDown();

            List<ReservationAttempt> attempts = new ArrayList<>();
            for (Future<ReservationAttempt> future : futures) {
                attempts.add(future.get(20, TimeUnit.SECONDS));
            }
            return new ConcurrencyMetrics(attempts, System.nanoTime() - startedAt);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "并发预约线程未在预期时间内结束");
        }
    }

    /** 等待统一起跑信号后，以当前线程的租户上下文调用预约服务。 */
    private ReservationAttempt reserveWhenReady(Long merchantId, Long availabilityId, Long userId,
            CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("等待并发预约起跑信号超时");
        }

        long startedAt = System.nanoTime();
        try {
            TenantContext.setMerchantId(merchantId);
            reservationService.create(userId, new CreateReservationRequest(availabilityId));
            return ReservationAttempt.success(System.nanoTime() - startedAt);
        } catch (BusinessException exception) {
            return ReservationAttempt.failure(exception.getErrorCode(), System.nanoTime() - startedAt);
        } finally {
            TenantContext.clear();
        }
    }

    /** 创建并返回用于并发预约测试的商家。 */
    private Long createMerchant() {
        Merchant merchant = new Merchant();
        merchant.setName("并发预约商家-" + UUID.randomUUID());
        merchantMapper.insert(merchant);
        return merchant.getId();
    }

    /** 创建一个已上架且未来可预约的商品可用性记录。 */
    private ProductAvailability createAvailableProduct(Long merchantId, int capacity) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setName("并发预约商品-" + UUID.randomUUID());
        product.setType(PRODUCT_TYPE);
        product.setOriginalPrice(BigDecimal.TEN);
        product.setSellingPrice(BigDecimal.TEN);
        product.setStatus(ON_SALE_STATUS);
        productMapper.insert(product);

        ProductAvailability availability = new ProductAvailability();
        availability.setMerchantId(merchantId);
        availability.setProductId(product.getId());
        availability.setStartAt(LocalDateTime.now().plusDays(1));
        availability.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
        availability.setCapacity(capacity);
        availability.setRemainingCapacity(capacity);
        availability.setStatus(OPEN_STATUS);
        productAvailabilityMapper.insert(availability);
        return availability;
    }

    /** 创建指定数量的有效客户，并返回其登录用户主键。 */
    private List<Long> createCustomers(Long merchantId, int count) {
        List<Long> userIds = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            userIds.add(createCustomer(merchantId, index));
        }
        return userIds;
    }

    /** 创建一个关联登录用户的有效客户档案，并返回用户主键。 */
    private Long createCustomer(Long merchantId, int index) {
        UserAccount user = new UserAccount();
        user.setEmail("concurrency-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(TEST_PASSWORD_HASH);
        userAccountMapper.insert(user);

        CustomerProfile customer = new CustomerProfile();
        customer.setMerchantId(merchantId);
        customer.setUserId(user.getId());
        customer.setName("并发客户-" + index);
        customer.setPhone(String.format("138%08d", index));
        customer.setStatus(ACTIVE_STATUS);
        customerProfileMapper.insert(customer);
        return user.getId();
    }

    /** 查询商品可用性的当前剩余数量。 */
    private int currentRemainingCapacity(Long availabilityId) {
        return productAvailabilityMapper.selectById(availabilityId).getRemainingCapacity();
    }

    /** 查询指定商品可用性的有效预约数量。 */
    private long activeReservationCount(Long merchantId, Long availabilityId) {
        return reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getMerchantId, merchantId)
                .eq(Reservation::getProductAvailabilityId, availabilityId)
                .eq(Reservation::getStatus, RESERVED_STATUS));
    }

    /** 输出当前并发场景的成功、失败、错误码和耗时指标。 */
    private void logMetrics(String scenario, ConcurrencyMetrics metrics) {
        log.info("event=reservation_concurrency_test scenario={} total={} success={} failure={} elapsed_ms={} "
                        + "throughput_per_second={} average_ms={} max_ms={} errors={}",
                scenario, metrics.totalCount(), metrics.successCount(), metrics.failureCount(),
                metrics.elapsedMillis(), metrics.throughputPerSecond(), metrics.averageMillis(),
                metrics.maxMillis(), metrics.errorCounts());
    }

    /** 表示单个并发预约请求的执行结果与耗时。 */
    private record ReservationAttempt(boolean succeeded, ErrorCode errorCode, long elapsedNanos) {

        /** 创建成功预约的执行结果。 */
        private static ReservationAttempt success(long elapsedNanos) {
            return new ReservationAttempt(true, null, elapsedNanos);
        }

        /** 创建失败预约的执行结果。 */
        private static ReservationAttempt failure(ErrorCode errorCode, long elapsedNanos) {
            return new ReservationAttempt(false, errorCode, elapsedNanos);
        }
    }

    /** 汇总一个并发预约场景的执行结果与性能指标。 */
    private record ConcurrencyMetrics(List<ReservationAttempt> attempts, long elapsedNanos) {

        /** 获取本次并发请求总数。 */
        private int totalCount() {
            return attempts.size();
        }

        /** 获取成功预约数量。 */
        private int successCount() {
            return (int) attempts.stream().filter(ReservationAttempt::succeeded).count();
        }

        /** 获取失败预约数量。 */
        private int failureCount() {
            return totalCount() - successCount();
        }

        /** 统计指定业务错误码的出现次数。 */
        private long errorCount(ErrorCode errorCode) {
            return attempts.stream().filter(attempt -> errorCode.equals(attempt.errorCode())).count();
        }

        /** 获取所有失败请求按错误码分组的数量。 */
        private Map<ErrorCode, Long> errorCounts() {
            return attempts.stream()
                    .filter(attempt -> attempt.errorCode() != null)
                    .collect(Collectors.groupingBy(ReservationAttempt::errorCode, Collectors.counting()));
        }

        /** 将总耗时转换为毫秒。 */
        private long elapsedMillis() {
            return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        }

        /** 计算本次并发场景的平均吞吐量。 */
        private double throughputPerSecond() {
            return totalCount() * 1_000_000_000D / Math.max(elapsedNanos, 1L);
        }

        /** 计算所有请求的平均耗时。 */
        private double averageMillis() {
            return attempts.stream().mapToLong(ReservationAttempt::elapsedNanos).average().orElse(0D) / 1_000_000D;
        }

        /** 获取单个请求的最大耗时。 */
        private double maxMillis() {
            return attempts.stream().mapToLong(ReservationAttempt::elapsedNanos).max().orElse(0L) / 1_000_000D;
        }
    }
}
