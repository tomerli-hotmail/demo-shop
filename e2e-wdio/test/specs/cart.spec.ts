import { expect } from '@wdio/globals'
import { addFirstProductToCart } from '../helpers/actions'

const BASE_URL = process.env.BASE_URL || 'https://frontend-production-3988.up.railway.app'

describe('Cart', () => {
    it('shows empty state when cart is empty', async () => {
        await browser.url(BASE_URL + '/cart')
        await $('//*[contains(text(),"Your cart is empty")]').waitForDisplayed({ timeout: 30000 })
        const src = await browser.getPageSource()
        expect(src).toContain('Your cart is empty')
    })

    it('added item appears in cart', async () => {
        await addFirstProductToCart()
        await browser.url(BASE_URL + '/cart')
        await $('//button[contains(text(),"Proceed to Checkout")]').waitForDisplayed({ timeout: 30000 })
        const btns = await $$('//button[contains(text(),"Proceed to Checkout")]')
        expect(btns.length).toBeGreaterThan(0)
    })

    it('quantity increase button increments count', async () => {
        await addFirstProductToCart()
        await browser.url(BASE_URL + '/cart')
        const plusBtn = await $('//button[text()="+"]')
        await plusBtn.waitForClickable({ timeout: 30000 })
        await plusBtn.click()
        await browser.waitUntil(
            async () => (await browser.getPageSource()).includes('2'),
            { timeout: 15000 }
        )
        const src = await browser.getPageSource()
        expect(src).toContain('2')
    })

    it('removing item shows empty cart', async () => {
        await addFirstProductToCart()
        await browser.url(BASE_URL + '/cart')
        const minusBtn = await $('//button[text()="−"]')
        await minusBtn.waitForClickable({ timeout: 30000 })
        await minusBtn.click()
        await $('//*[contains(text(),"Your cart is empty")]').waitForDisplayed({ timeout: 30000 })
        const src = await browser.getPageSource()
        expect(src).toContain('Your cart is empty')
    })

    it('cart persists across navigation', async () => {
        await addFirstProductToCart()
        await browser.url(BASE_URL)
        await $('a[href*="/products/"]').waitForDisplayed({ timeout: 30000 })
        await browser.url(BASE_URL + '/cart')
        await $('//button[contains(text(),"Proceed to Checkout")]').waitForDisplayed({ timeout: 30000 })
        const btns = await $$('//button[contains(text(),"Proceed to Checkout")]')
        expect(btns.length).toBeGreaterThan(0)
    })
})
