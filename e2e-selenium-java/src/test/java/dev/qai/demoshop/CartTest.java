package dev.qai.demoshop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart")
public class CartTest extends BaseTest {

    @Test
    @DisplayName("empty cart shows empty state message")
    void emptyCartShowsEmptyState() {
        driver.get(BASE_URL + "/cart");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'Your cart is empty')]")));
        assertTrue(driver.getPageSource().contains("Your cart is empty"));
    }

    @Test
    @DisplayName("added item appears in cart")
    void addedItemAppearsInCart() {
        addFirstProductToCart();
        driver.get(BASE_URL + "/cart");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//button[contains(text(),'Proceed to Checkout')]")));
        assertTrue(driver.findElements(
            By.xpath("//button[contains(text(),'Proceed to Checkout')]")).size() > 0);
    }

    @Test
    @DisplayName("quantity increase button works")
    void quantityIncreaseWorks() {
        addFirstProductToCart();
        driver.get(BASE_URL + "/cart");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[text()='+']")));
        driver.findElement(By.xpath("//button[text()='+']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='2']")));
        assertTrue(driver.getPageSource().contains("2"));
    }

    @Test
    @DisplayName("removing item shows empty cart")
    void removingItemShowsEmptyCart() {
        addFirstProductToCart();
        driver.get(BASE_URL + "/cart");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[text()='−']")));
        driver.findElement(By.xpath("//button[text()='−']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'Your cart is empty')]")));
        assertTrue(driver.getPageSource().contains("Your cart is empty"));
    }

    @Test
    @DisplayName("cart persists across navigation")
    void cartPersistsAcrossNavigation() {
        addFirstProductToCart();
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("a[href*='/products/']")));
        driver.get(BASE_URL + "/cart");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//button[contains(text(),'Proceed to Checkout')]")));
        assertTrue(driver.findElements(
            By.xpath("//button[contains(text(),'Proceed to Checkout')]")).size() > 0);
    }
}
