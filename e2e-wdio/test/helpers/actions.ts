const BASE_URL = process.env.BASE_URL || 'https://frontend-production-3988.up.railway.app'

export async function login() {
    await browser.url(BASE_URL + '/login')
    await $('[name="username"]').waitForDisplayed({ timeout: 30000 })
    await $('[name="username"]').setValue('demo')
    await $('[name="password"]').setValue('demo123')
    await $('//button[contains(text(),"Sign In")]').click()
    await $('//*[contains(text(),"Hi, demo")]').waitForDisplayed({ timeout: 30000 })
}

export async function addFirstProductToCart() {
    await browser.url(BASE_URL)
    const firstProduct = await $('a[href*="/products/"]')
    await firstProduct.waitForClickable({ timeout: 30000 })
    await firstProduct.click()
    const addBtn = await $('//button[contains(text(),"Add to Cart")]')
    await addBtn.waitForClickable({ timeout: 30000 })
    await addBtn.click()
    await $('//button[contains(text(),"Added!")]').waitForDisplayed({ timeout: 30000 })
}
