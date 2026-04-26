package dev.qai.demoshop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Catalog")
public class ProductCatalogTest extends BaseTest {

    @Test
    @DisplayName("renders product cards on homepage")
    void rendersProductCards() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("a[href*='/products/']")));
        List<WebElement> cards = driver.findElements(By.cssSelector("a[href*='/products/']"));
        assertTrue(cards.size() >= 10, "Expected at least 10 products, got " + cards.size());
    }

    @Test
    @DisplayName("clicking a product navigates to detail page")
    void clickingProductNavigatesToDetail() {
        driver.get(BASE_URL);
        WebElement firstCard = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='/products/']")));
        firstCard.click();
        wait.until(ExpectedConditions.urlContains("/products/"));
        assertTrue(driver.getCurrentUrl().contains("/products/"));
    }

    @Test
    @DisplayName("search narrows results")
    void searchNarrowsResults() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("a[href*='/products/']")));
        WebElement searchInput = driver.findElement(
            By.xpath("//input[contains(@placeholder,'earch') or contains(@placeholder,'earch')]"));
        searchInput.clear();
        searchInput.sendKeys("bamboo");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'bamboo')]")));
        List<WebElement> results = driver.findElements(By.cssSelector("a[href*='/products/']"));
        assertTrue(results.size() > 0, "Expected search results for 'bamboo'");
    }

    @Test
    @DisplayName("search with no match shows empty state")
    void searchWithNoMatchShowsEmptyState() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("a[href*='/products/']")));
        WebElement searchInput = driver.findElement(
            By.xpath("//input[contains(@placeholder,'earch')]"));
        searchInput.clear();
        searchInput.sendKeys("zzzznotaproduct");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector("a[href*='/products/']")));
        assertEquals(0, driver.findElements(By.cssSelector("a[href*='/products/']")).size());
    }
}
