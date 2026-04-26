import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


def login(driver, base_url):
    driver.get(base_url + "/login")
    wait = WebDriverWait(driver, 30)
    wait.until(EC.visibility_of_element_located((By.NAME, "username")))
    driver.find_element(By.NAME, "username").send_keys("demo")
    driver.find_element(By.NAME, "password").send_keys("demo123")
    driver.find_element(By.XPATH, "//button[contains(text(),'Sign In')]").click()
    wait.until(EC.visibility_of_element_located(
        (By.XPATH, "//*[contains(text(),'Hi, demo')]")))


def add_first_product(driver, base_url):
    driver.get(base_url)
    wait = WebDriverWait(driver, 30)
    first_product = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, "a[href*='/products/']")))
    first_product.click()
    wait.until(EC.element_to_be_clickable(
        (By.XPATH, "//button[contains(text(),'Add to Cart')]"))).click()
    wait.until(EC.visibility_of_element_located(
        (By.XPATH, "//button[contains(text(),'Added!')]")))


class TestCheckout:

    @pytest.fixture(autouse=True)
    def setup_checkout(self, driver, base_url):
        login(driver, base_url)
        add_first_product(driver, base_url)
        driver.get(base_url + "/checkout")
        WebDriverWait(driver, 30).until(
            EC.visibility_of_element_located((By.XPATH, "//input[contains(@placeholder,'Main St')]")))

    def test_all_fields_are_present(self, driver, base_url):
        assert len(driver.find_elements(By.XPATH, "//input[contains(@placeholder,'Main St')]")) > 0
        assert len(driver.find_elements(By.XPATH, "//input[contains(@placeholder,'1234 5678')]")) > 0
        assert len(driver.find_elements(By.XPATH, "//input[@placeholder='MM/YY']")) > 0
        assert len(driver.find_elements(By.XPATH, "//input[@placeholder='123']")) > 0
        assert len(driver.find_elements(By.XPATH, "//input[contains(@placeholder,'DEMO10')]")) > 0

    def test_invalid_coupon_shows_error(self, driver, base_url):
        wait = WebDriverWait(driver, 15)
        coupon = driver.find_element(By.XPATH, "//input[contains(@placeholder,'DEMO10')]")
        coupon.clear()
        coupon.send_keys("BADCODE")
        driver.find_element(By.XPATH, "//button[contains(text(),'Apply')]").click()
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(translate(text(),'INVALID','invalid'),'invalid')]")))
        assert "invalid" in driver.page_source.lower()

    def test_valid_coupon_applies_discount(self, driver, base_url):
        wait = WebDriverWait(driver, 15)
        coupon = driver.find_element(By.XPATH, "//input[contains(@placeholder,'DEMO10')]")
        coupon.clear()
        coupon.send_keys("DEMO10")
        driver.find_element(By.XPATH, "//button[contains(text(),'Apply')]").click()
        wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(text(),'10%')]")))
        assert "10%" in driver.page_source

    def test_complete_order_shows_confirmation(self, driver, base_url):
        wait = WebDriverWait(driver, 30)
        driver.find_element(By.XPATH, "//input[contains(@placeholder,'Main St')]") \
              .send_keys("123 Test Ave, Springfield, CA 90210")
        driver.find_element(By.XPATH, "//input[contains(@placeholder,'1234 5678')]") \
              .send_keys("4242 4242 4242 4242")
        driver.find_element(By.XPATH, "//input[@placeholder='MM/YY']").send_keys("12/28")
        driver.find_element(By.XPATH, "//input[@placeholder='123']").send_keys("123")

        coupon = driver.find_element(By.XPATH, "//input[contains(@placeholder,'DEMO10')]")
        coupon.send_keys("DEMO10")
        driver.find_element(By.XPATH, "//button[contains(text(),'Apply')]").click()
        wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(text(),'10%')]")))

        driver.find_element(By.XPATH, "//button[contains(text(),'Place Order')]").click()
        wait.until(EC.url_contains("/order/"))
        assert "/order/" in driver.current_url
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(text(),'Order Confirmed')]")))
        assert "Order Confirmed" in driver.page_source
