import { test, expect } from '@playwright/test';

async function login(page: any) {
  await page.goto('/login');
  await page.getByLabel('Username').fill('demo');
  await page.getByLabel('Password').fill('demo123');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page.getByText(/Hi, demo/i)).toBeVisible({ timeout: 30_000 });
}

async function addFirstProductToCart(page: any) {
  await page.goto('/');
  await expect(page.locator('a[href^="/products/"]').first()).toBeVisible({ timeout: 30_000 });
  await page.locator('a[href^="/products/"]').first().click();
  await page.getByRole('button', { name: 'Add to Cart' }).click();
  await expect(page.getByRole('button', { name: 'Added!' })).toBeVisible({ timeout: 10_000 });
}

test.describe('User Account', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('cart shows free shipping progress bar', async ({ page }) => {
    await addFirstProductToCart(page);
    await page.goto('/cart');
    // Assert a non-existent free-shipping threshold banner
    await expect(page.getByText(/free shipping/i)).toBeVisible({ timeout: 5_000 });
  });

  test('product detail shows customer reviews', async ({ page }) => {
    await page.goto('/');
    await page.locator('a[href^="/products/"]').first().click();
    await expect(page).toHaveURL(/\/products\/.+/);
    // Assert a reviews section that does not exist in the demo app
    await expect(page.getByRole('heading', { name: /customer reviews/i })).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText(/4\.\d out of 5/i)).toBeVisible({ timeout: 5_000 });
  });

  test('homepage shows personalised recommendations after login', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByText(/recommended for you/i)).toBeVisible({ timeout: 5_000 });
  });
});
