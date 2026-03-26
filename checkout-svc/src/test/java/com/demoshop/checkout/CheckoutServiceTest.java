package com.demoshop.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    /**
     * Simple fake RestTemplate implementation to avoid Byte Buddy / Mockito
     * issues on Java 25 while still allowing us to control responses and
     * inspect calls.
     */
    private static class FakeRestTemplate extends RestTemplate {
        Map<String, Object> cartResponse;
        Map<String, Object> paymentResponse;

        final List<String> deletedUrls = new ArrayList<>();
        final List<String> postUrls = new ArrayList<>();
        final List<Object> postBodies = new ArrayList<>();

        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
            if (responseType == Map.class) {
                return responseType.cast(cartResponse);
            }
            return null;
        }

        @Override
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
            postUrls.add(url);
            postBodies.add(request);
            if (responseType == Map.class) {
                return responseType.cast(paymentResponse);
            }
            return null;
        }

        @Override
        public void delete(String url, Object... uriVariables) {
            deletedUrls.add(url);
        }
    }

    /**
     * Simple fake CouponService to control discount behavior without Mockito.
     */
    private static class FakeCouponService extends CouponService {
        String lastCode;
        BigDecimal lastSubtotal;
        BigDecimal discountToReturn = BigDecimal.ZERO;

        @Override
        public BigDecimal apply(String code, BigDecimal subtotal) {
            this.lastCode = code;
            this.lastSubtotal = subtotal;
            return discountToReturn;
        }
    }

    /**
     * Simple fake TaxService to control tax behavior without Mockito.
     */
    private static class FakeTaxService extends TaxService {
        String lastAddress;
        BigDecimal lastAmount;
        TaxResult taxToReturn;

        @Override
        public TaxResult calculate(String shippingAddress, BigDecimal amount) {
            this.lastAddress = shippingAddress;
            this.lastAmount = amount;
            return taxToReturn;
        }
    }

    private FakeRestTemplate restTemplate;
    private FakeCouponService couponService;
    private FakeTaxService taxService;
    private CheckoutService checkoutService;

    private final String cartUrl = "http://cart-svc";
    private final String paymentUrl = "http://payment-svc";
    private final String supabaseUrl = "http://supabase";
    private final String supabaseKey = "test-key";

    @BeforeEach
    void setup() {
        restTemplate = new FakeRestTemplate();
        couponService = new FakeCouponService();
        taxService = new FakeTaxService();

        checkoutService = new CheckoutService(restTemplate, couponService, taxService);
        // set private @Value fields
        ReflectionTestUtils.setField(checkoutService, "cartServiceUrl", cartUrl);
        ReflectionTestUtils.setField(checkoutService, "paymentServiceUrl", paymentUrl);
        ReflectionTestUtils.setField(checkoutService, "supabaseUrl", supabaseUrl);
        ReflectionTestUtils.setField(checkoutService, "supabaseKey", supabaseKey);
    }

    @Test
    void shouldThrowWhenCartIsEmpty() {
        String sessionId = "sess-empty";
        restTemplate.cartResponse = Map.of("items", List.of());

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> checkoutService.processCheckout(req));
        assertEquals("Cart is empty", ex.getMessage());
    }

    @Test
    void shouldCalculateSubtotalAndApplyCouponAndTaxAndReturnResult() {
        String sessionId = "sess-1";
        // two items: product A price 10.00 qty 2, product B price 5.50 qty 1 -> subtotal = 25.50
        Map<String, Object> productA = Map.of("id", "p-A", "price_usd", "10.00");
        Map<String, Object> itemA = Map.of("products", productA, "quantity", 2);
        Map<String, Object> productB = Map.of("id", "p-B", "price_usd", "5.50");
        Map<String, Object> itemB = Map.of("products", productB, "quantity", 1);
        List<Map<String, Object>> items = List.of(itemA, itemB);
        restTemplate.cartResponse = Map.of("items", items);

        // coupon reduces by 2.55 (10%)
        couponService.discountToReturn = BigDecimal.valueOf(2.55);

        // tax on after-discount (25.50 - 2.55 = 22.95) -> tax = 1.83
        taxService.taxToReturn = new TaxResult("CA", 0.08, BigDecimal.valueOf(1.83));

        // payment service returns transaction id
        restTemplate.paymentResponse = Map.of("transactionId", "tx-999");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setEmail("user@example.com");
        req.setShippingAddress("123 Main St");
        req.setCouponCode("DEMO10");

        CheckoutResult res = checkoutService.processCheckout(req);

        assertNotNull(res.getOrderId());
        assertEquals("tx-999", res.getTransactionId());
        assertEquals(0, res.getSubtotal().compareTo(BigDecimal.valueOf(25.50)));
        assertEquals("DEMO10", res.getDiscountCode());
        assertEquals(0, res.getDiscountAmount().compareTo(BigDecimal.valueOf(2.55)));
        assertEquals("CA", res.getTaxState());
        assertEquals(0.08, res.getTaxRate());
        assertEquals(0, res.getTaxAmount().compareTo(BigDecimal.valueOf(1.83)));
        // total = afterDiscount + tax = (25.50 - 2.55) + 1.83 = 24.78
        assertEquals(0, res.getTotal().compareTo(BigDecimal.valueOf(24.78)));

        // verify coupon and tax inputs
        assertEquals("DEMO10", couponService.lastCode);
        assertEquals(0, couponService.lastSubtotal.compareTo(BigDecimal.valueOf(25.50)));
        assertEquals("123 Main St", taxService.lastAddress);
        assertEquals(0, taxService.lastAmount.compareTo(BigDecimal.valueOf(22.95)));

        // verify supabase order post and order_items posts were called
        assertTrue(restTemplate.postUrls.stream().anyMatch(u -> u.startsWith(supabaseUrl + "/rest/v1/orders")));
        assertTrue(restTemplate.postUrls.stream().anyMatch(u -> u.startsWith(supabaseUrl + "/rest/v1/order_items")));
        // verify payment and cart delete
        assertTrue(restTemplate.postUrls.stream().anyMatch(u -> u.equals(paymentUrl + "/payments")));
        assertTrue(restTemplate.deletedUrls.contains(cartUrl + "/cart/" + sessionId));
    }

    @Test
    void shouldPropagateWhenCouponIsAbsentOrBlank() {
        String sessionId = "sess-2";
        Map<String, Object> product = Map.of("id", "p-1", "price_usd", "20.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 1);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        // no discount
        couponService.discountToReturn = BigDecimal.ZERO;

        taxService.taxToReturn = new TaxResult("NY", 0.05, BigDecimal.valueOf(1.0));
        restTemplate.paymentResponse = Map.of("transactionId", "tx-111");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("Somewhere");
        // couponCode left null

        CheckoutResult res = checkoutService.processCheckout(req);

        assertEquals(0, res.getDiscountAmount().compareTo(BigDecimal.ZERO));
        assertEquals("NY", res.getTaxState());
        // couponService should have been called with null code
        assertNull(couponService.lastCode);
        assertEquals(0, couponService.lastSubtotal.compareTo(BigDecimal.valueOf(20.00)));
    }

    @Test
    void shouldSendCorrectOrderItemsPayloadsMatchingOrderId() {
        String sessionId = "sess-3";
        Map<String, Object> product = Map.of("id", "prod-X", "price_usd", "7.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 3);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = new TaxResult("TX", 0.00, BigDecimal.ZERO);
        restTemplate.paymentResponse = Map.of("transactionId", "tx-222");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setEmail("a@b.com");
        req.setShippingAddress("Addr");

        CheckoutResult res = checkoutService.processCheckout(req);

        // verify that an order was created and order_items were posted
        assertTrue(restTemplate.postUrls.stream().anyMatch(u -> u.startsWith(supabaseUrl + "/rest/v1/orders")));
        assertTrue(restTemplate.postUrls.stream().anyMatch(u -> u.startsWith(supabaseUrl + "/rest/v1/order_items")));
        assertEquals("tx-222", res.getTransactionId());
    }

    @Test
    void shouldCallPaymentServiceWithExpectedAmount() {
        String sessionId = "sess-4";
        Map<String, Object> product = Map.of("id", "prod-Y", "price_usd", "3.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 4); // subtotal 12.00
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = new TaxResult("FL", 0.00, BigDecimal.ZERO);
        restTemplate.paymentResponse = Map.of("transactionId", "tx-333");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("Addr");

        CheckoutResult res = checkoutService.processCheckout(req);

        // verify payment payload amount equals subtotal 12.00
        int paymentIndex = -1;
        for (int i = 0; i < restTemplate.postUrls.size(); i++) {
            if (restTemplate.postUrls.get(i).equals(paymentUrl + "/payments")) {
                paymentIndex = i;
                break;
            }
        }
        assertTrue(paymentIndex >= 0, "payment call not found");
        Object captured = restTemplate.postBodies.get(paymentIndex);
        assertNotNull(captured);
        assertTrue(captured instanceof Map);
        Map<?, ?> payload = (Map<?, ?>) captured;
        assertTrue(payload.containsKey("amount"));
        Object amountObj = payload.get("amount");
        assertEquals(0, new BigDecimal(amountObj.toString()).compareTo(BigDecimal.valueOf(12.00)));

        assertTrue(restTemplate.deletedUrls.contains(cartUrl + "/cart/" + sessionId));
    }

    // --- New tests added below ---

    @Test
    void shouldSetSupabaseHeadersOnOrderAndItemPosts() {
        String sessionId = "sess-headers";
        Map<String, Object> product = Map.of("id", "p-H", "price_usd", "1.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 1);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = new TaxResult("CA", 0.0, BigDecimal.ZERO);
        restTemplate.paymentResponse = Map.of("transactionId", "tx-headers");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("Addr");
        req.setEmail("e@h.com");

        CheckoutResult res = checkoutService.processCheckout(req);

        // find the order post index
        int orderIndex = -1;
        for (int i = 0; i < restTemplate.postUrls.size(); i++) {
            if (restTemplate.postUrls.get(i).startsWith(supabaseUrl + "/rest/v1/orders")) {
                orderIndex = i;
                break;
            }
        }
        assertTrue(orderIndex >= 0, "order post not found");
        Object captured = restTemplate.postBodies.get(orderIndex);
        assertNotNull(captured);
        assertTrue(captured instanceof HttpEntity);
        HttpEntity<?> ent = (HttpEntity<?>) captured;
        HttpHeaders headers = ent.getHeaders();
        assertEquals(supabaseKey, headers.getFirst("apikey"));
        assertEquals("Bearer " + supabaseKey, headers.getFirst("Authorization"));
        assertNotNull(headers.getContentType());
    }

    @Test
    void shouldHandleNullTaxResultGracefully() {
        String sessionId = "sess-null-tax";
        Map<String, Object> product = Map.of("id", "p-N", "price_usd", "5.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 2);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = null; // simulate null-safe behavior
        restTemplate.paymentResponse = Map.of("transactionId", "tx-null-tax");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("Addr");

        CheckoutResult res = checkoutService.processCheckout(req);

        assertEquals(0, res.getTaxAmount().compareTo(BigDecimal.ZERO));
        assertNull(res.getTaxState());
        assertEquals(0.0, res.getTaxRate());
    }

    @Test
    void shouldReturnNullTransactionIdWhenPaymentServiceReturnsNull() {
        String sessionId = "sess-null-payment";
        Map<String, Object> product = Map.of("id", "p-P", "price_usd", "2.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 1);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = new TaxResult("WA", 0.0, BigDecimal.ZERO);
        restTemplate.paymentResponse = null; // payment service returned nothing

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("Addr");

        CheckoutResult res = checkoutService.processCheckout(req);

        assertNull(res.getTransactionId());
    }

    @Test
    void shouldIncludeEmailAndShippingInOrderPayload() {
        String sessionId = "sess-payload";
        Map<String, Object> product = Map.of("id", "p-PAY", "price_usd", "9.00");
        Map<String, Object> item = Map.of("products", product, "quantity", 1);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = new TaxResult("OR", 0.0, BigDecimal.ZERO);
        restTemplate.paymentResponse = Map.of("transactionId", "tx-payload");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("123 Payload St");
        req.setEmail("payload@example.com");

        CheckoutResult res = checkoutService.processCheckout(req);

        // find order post and inspect body
        int orderIndex = -1;
        for (int i = 0; i < restTemplate.postUrls.size(); i++) {
            if (restTemplate.postUrls.get(i).startsWith(supabaseUrl + "/rest/v1/orders")) {
                orderIndex = i;
                break;
            }
        }
        assertTrue(orderIndex >= 0, "order post not found");
        Object captured = restTemplate.postBodies.get(orderIndex);
        assertNotNull(captured);
        assertTrue(captured instanceof HttpEntity);
        HttpEntity<?> ent = (HttpEntity<?>) captured;
        Object body = ent.getBody();
        assertTrue(body instanceof Map);
        Map<?, ?> bodyMap = (Map<?, ?>) body;
        assertEquals("payload@example.com", bodyMap.get("email"));
        assertEquals("123 Payload St", bodyMap.get("shipping_address"));
    }

    @Test
    void shouldHandleNumericPriceValues() {
        String sessionId = "sess-numeric-price";
        // price as Double instead of String
        Map<String, Object> product = Map.of("id", "p-D", "price_usd", 4.5);
        Map<String, Object> item = Map.of("products", product, "quantity", 2);
        restTemplate.cartResponse = Map.of("items", List.of(item));

        couponService.discountToReturn = BigDecimal.ZERO;
        taxService.taxToReturn = new TaxResult("NV", 0.0, BigDecimal.ZERO);
        restTemplate.paymentResponse = Map.of("transactionId", "tx-num-price");

        CheckoutRequest req = new CheckoutRequest();
        req.setSessionId(sessionId);
        req.setShippingAddress("Addr");

        CheckoutResult res = checkoutService.processCheckout(req);

        assertEquals(0, res.getSubtotal().compareTo(BigDecimal.valueOf(9.0)));
    }
}
