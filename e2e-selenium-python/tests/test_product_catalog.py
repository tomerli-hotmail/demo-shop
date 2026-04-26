import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


class TestProductCatalog:

    def test_renders_product_cards(self, driver, base_url):
        driver.get(base_url)
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, "a[href*='/products/']")))
        cards = driver.find_elements(By.CSS_SELECTOR, "a[href*='/products/']")
        assert len(cards) >= 10, f"Expected at least 10 products, got {len(cards)}"

    def test_clicking_product_navigates_to_detail(self, driver, base_url):
        driver.get(base_url)
        wait = WebDriverWait(driver, 30)
        first_card = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, "a[href*='/products/']")))
        first_card.click()
        wait.until(EC.url_contains("/products/"))
        assert "/products/" in driver.current_url

    def test_search_narrows_results(self, driver, base_url):
        driver.get(base_url)
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, "a[href*='/products/']")))
        search = driver.find_element(By.XPATH, "//input[contains(@placeholder,'earch')]")
        search.clear()
        search.send_keys("bamboo")
        wait.until(EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'bamboo')]")))
        results = driver.find_elements(By.CSS_SELECTOR, "a[href*='/products/']")
        assert len(results) > 0

    def test_search_with_no_match_shows_empty_state(self, driver, base_url):
        driver.get(base_url)
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, "a[href*='/products/']")))
        search = driver.find_element(By.XPATH, "//input[contains(@placeholder,'earch')]")
        search.clear()
        search.send_keys("zzzznotaproduct")
        wait.until(EC.invisibility_of_element_located((By.CSS_SELECTOR, "a[href*='/products/']")))
        assert len(driver.find_elements(By.CSS_SELECTOR, "a[href*='/products/']")) == 0

    def test_price_is_correctly_formatted(self, driver, base_url):
        import re
        driver.get(base_url)
        wait = WebDriverWait(driver, 30)
        wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, "a[href*='/products/']")))
        cards = driver.find_elements(By.CSS_SELECTOR, "a[href*='/products/']")
        for card in cards[:5]:
            text = card.text
            assert re.search(r"\$\d+\.\d{2}", text), f"Price not formatted correctly in: {text}"
