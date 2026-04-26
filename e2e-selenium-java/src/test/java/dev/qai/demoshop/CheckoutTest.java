package dev.qai.demoshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkout")
public class CheckoutTest extends BaseTest {

    @BeforeEach
    void setUpCheckout() {
        login();
        addFirstProductToCart();
        driver.get(BASE_URL + "/checkout");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//input[contains(@placeholder,'Main St')]")));
    }

    @Test
    @DisplayName("all payment and shipping fields are present")
    void checkoutFieldsArePresent() {
        assertTrue(driver.findElements(
            By.xpath("//input[contains(@placeholder,'Main St')]")).size() > 0, "Address field missing");
        assertTrue(driver.findElements(
            By.xpath("//input[contains(@placeholder,'1234 5678')]")).size() > 0, "Card number field missing");
        assertTrue(driver.findElements(
            By.xpath("//input[@placeholder='MM/YY']")).size() > 0, "Expiry field missing");
        assertTrue(driver.findElements(
            By.xpath("//input[@placeholder='123']")).size() > 0, "CVV field missing");
        assertTrue(driver.findElements(
            By.xpath("//input[contains(@placeholder,'DEMO10')]")).size() > 0, "Coupon field missing");
    }

    @Test
    @DisplayName("invalid coupon shows error")
    void invalidCouponShowsError() {
        WebElement couponInput = driver.findElement(
            By.xpath("//input[contains(@placeholder,'DEMO10')]"));
        couponInput.clear();
        couponInput.sendKeys("BADCODE");
        driver.findElement(By.xpath("//button[contains(text(),'Apply')]")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(translate(text(),'INVALID','invalid'),'invalid')]")));
        assertTrue(driver.getPageSource().toLowerCase().contains("invalid"));
    }

    @Test
    @DisplayName("DEMO10 coupon applies 10% discount")
    void validCouponAppliesDiscount() {
        WebElement couponInput = driver.findElement(
            By.xpath("//input[contains(@placeholder,'DEMO10')]"));
        couponInput.clear();
        couponInput.sendKeys("DEMO10");
        driver.findElement(By.xpath("//button[contains(text(),'Apply')]")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'10%')]")));
        assertTrue(driver.getPageSource().contains("10%"));
    }

    @Test
    @DisplayName("completes full order and shows confirmation")
    void completesFullOrderAndShowsConfirmation() {
        driver.findElement(By.xpath("//input[contains(@placeholder,'Main St')]"))
              .sendKeys("123 Test Ave, Springfield, CA 90210");
        driver.findElement(By.xpath("//input[contains(@placeholder,'1234 5678')]"))
              .sendKeys("4242 4242 4242 4242");
        driver.findElement(By.xpath("//input[@placeholder='MM/YY']")).sendKeys("12/28");
        driver.findElement(By.xpath("//input[@placeholder='123']")).sendKeys("123");

        WebElement couponInput = driver.findElement(
            By.xpath("//input[contains(@placeholder,'DEMO10')]"));
        couponInput.sendKeys("DEMO10");
        driver.findElement(By.xpath("//button[contains(text(),'Apply')]")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'10%')]")));

        driver.findElement(By.xpath("//button[contains(text(),'Place Order')]")).click();
        wait.until(ExpectedConditions.urlContains("/order/"));
        assertTrue(driver.getCurrentUrl().contains("/order/"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'Order Confirmed')]")));
        assertTrue(driver.getPageSource().contains("Order Confirmed"));
    }
}
