package com.demoshop.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CheckoutService {

    private final RestTemplate restTemplate;
    private final CouponService couponService;
    private final TaxService taxService;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    public CheckoutService(RestTemplate restTemplate, CouponService couponService, TaxService taxService) {
        this.restTemplate = restTemplate;
        this.couponService = couponService;
        this.taxService = taxService;
    }

    private HttpHeaders supabaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("Prefer", "return=minimal");
        return headers;
    }

    @SuppressWarnings("unchecked")
    public CheckoutResult processCheckout(CheckoutRequest request) {
        String sessionId = request.getSessionId();

        // 1. Fetch cart items from cart-svc
        Map<String, Object> cartResponse = restTemplate.getForObject(
            cartServiceUrl + "/cart/" + sessionId,
            Map.class
        );

        List<Map<String, Object>> items = (List<Map<String, Object>>) cartResponse.get("items");
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 2. Calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Map<String, Object> product = (Map<String, Object>) item.get("products");
            BigDecimal price = new BigDecimal(product.get("price_usd").toString());
            int quantity = (Integer) item.get("quantity");
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(quantity)));
        }

        // 3. Apply coupon discount
        String couponCode = request.getCouponCode();
        BigDecimal discountAmount = couponService.apply(couponCode, subtotal);
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);

        // 4. Calculate tax (null-safe)
        TaxResult tax = taxService.calculate(request.getShippingAddress(), afterDiscount);
        BigDecimal taxAmount = (tax != null && tax.getTaxAmount() != null)
            ? tax.getTaxAmount()
            : BigDecimal.ZERO;
        BigDecimal total = afterDiscount.add(taxAmount);

        // 5. Insert order via Supabase REST API
        String orderId = UUID.randomUUID().toString();
        Map<String, Object> orderPayload = new java.util.HashMap<>();
        orderPayload.put("id", orderId);
        orderPayload.put("session_id", sessionId);
        orderPayload.put("email", request.getEmail());
        orderPayload.put("shipping_address", request.getShippingAddress());
        orderPayload.put("total_usd", total);

        HttpEntity<Map<String, Object>> orderRequest = new HttpEntity<>(orderPayload, supabaseHeaders());
        restTemplate.postForObject(supabaseUrl + "/rest/v1/orders", orderRequest, Void.class);

        // 6. Insert order items via Supabase REST API
        for (Map<String, Object> item : items) {
            Map<String, Object> product = (Map<String, Object>) item.get("products");
            BigDecimal price = new BigDecimal(product.get("price_usd").toString());
            int quantity = (Integer) item.get("quantity");
            String productId = product.get("id").toString();

            Map<String, Object> itemPayload = new java.util.HashMap<>();
            itemPayload.put("order_id", orderId);
            itemPayload.put("product_id", productId);
            itemPayload.put("quantity", quantity);
            itemPayload.put("unit_price_usd", price);

            HttpEntity<Map<String, Object>> itemRequest = new HttpEntity<>(itemPayload, supabaseHeaders());
            restTemplate.postForObject(supabaseUrl + "/rest/v1/order_items", itemRequest, Void.class);
        }

        // 7. Call payment-svc (avoid Map.of to be robust against nulls)
        Map<String, Object> paymentPayload = new java.util.HashMap<>();
        paymentPayload.put("orderId", orderId);
        paymentPayload.put("amount", total);

        Map<String, Object> paymentResponse = restTemplate.postForObject(
            paymentServiceUrl + "/payments",
            paymentPayload,
            Map.class
        );
        String transactionId = paymentResponse != null
            ? (String) paymentResponse.get("transactionId")
            : null;

        // 8. Clear cart
        restTemplate.delete(cartServiceUrl + "/cart/" + sessionId);

        return new CheckoutResult(
            orderId, transactionId,
            subtotal, couponCode, discountAmount,
            tax != null ? tax.getState() : null,
            tax != null ? tax.getRate() : 0.0,
            taxAmount,
            total
        );
    }
}
