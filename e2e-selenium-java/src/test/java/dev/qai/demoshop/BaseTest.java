package dev.qai.demoshop;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected static final String BASE_URL =
        System.getProperty("base.url", "https://frontend-production-3988.up.railway.app");

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                             "--window-size=1280,800");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    protected void login() {
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
        driver.findElement(By.name("username")).sendKeys("demo");
        driver.findElement(By.name("password")).sendKeys("demo123");
        driver.findElement(By.xpath("//button[contains(text(),'Sign In')]")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'Hi, demo')]")));
    }

    protected void addFirstProductToCart() {
        driver.get(BASE_URL);
        WebElement firstProduct = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a[href*='/products/']")));
        firstProduct.click();
        WebElement addBtn = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Add to Cart')]")));
        addBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//button[contains(text(),'Added!')]")));
    }
}
