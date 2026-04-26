import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


def add_first_product_to_cart(driver, base_url):
    driver.get(base_url)
    wait = WebDriverWait(driver, 30)
    first_product = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, "a[href*='/products/']")))
    first_product.click()
    add_btn = wait.until(EC.element_to_be_clickable(
        (By.XPATH, "//button[contains(text(),'Add to Cart')]")))
    add_btn.click()
    wait.until(EC.visibility_of_element_located(
        (By.XPATH, "//button[contains(text(),'Added!')]")))


class TestCart:

    def test_empty_cart_shows_empty_state(self, driver, base_url):
        driver.get(base_url + "/cart")
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(text(),'Your cart is empty')]")))
        assert "Your cart is empty" in driver.page_source

    def test_added_item_appears_in_cart(self, driver, base_url):
        add_first_product_to_cart(driver, base_url)
        driver.get(base_url + "/cart")
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//button[contains(text(),'Proceed to Checkout')]")))
        assert len(driver.find_elements(
            By.XPATH, "//button[contains(text(),'Proceed to Checkout')]")) > 0

    def test_quantity_increase_button_works(self, driver, base_url):
        add_first_product_to_cart(driver, base_url)
        driver.get(base_url + "/cart")
        wait = WebDriverWait(driver, 30)
        plus_btn = wait.until(EC.element_to_be_clickable((By.XPATH, "//button[text()='+']")))
        plus_btn.click()
        wait.until(EC.visibility_of_element_located((By.XPATH, "//*[text()='2']")))
        assert "2" in driver.page_source

    def test_removing_item_shows_empty_cart(self, driver, base_url):
        add_first_product_to_cart(driver, base_url)
        driver.get(base_url + "/cart")
        wait = WebDriverWait(driver, 30)
        minus_btn = wait.until(EC.element_to_be_clickable((By.XPATH, "//button[text()='−']")))
        minus_btn.click()
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(text(),'Your cart is empty')]")))
        assert "Your cart is empty" in driver.page_source

    def test_cart_persists_across_navigation(self, driver, base_url):
        add_first_product_to_cart(driver, base_url)
        driver.get(base_url)
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, "a[href*='/products/']")))
        driver.get(base_url + "/cart")
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//button[contains(text(),'Proceed to Checkout')]")))
        assert len(driver.find_elements(
            By.XPATH, "//button[contains(text(),'Proceed to Checkout')]")) > 0
