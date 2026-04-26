import { expect } from '@wdio/globals'
import { login, addFirstProductToCart } from '../helpers/actions'

const BASE_URL = process.env.BASE_URL || 'https://frontend-production-3988.up.railway.app'

describe('Checkout', () => {
    beforeEach(async () => {
        await login()
        await addFirstProductToCart()
        await browser.url(BASE_URL + '/checkout')
        await $('//input[contains(@placeholder,"Main St")]').waitForDisplayed({ timeout: 30000 })
    })

    it('all required fields are present', async () => {
        expect((await $$('//input[contains(@placeholder,"Main St")]')).length).toBeGreaterThan(0)
        expect((await $$('//input[contains(@placeholder,"1234 5678")]')).length).toBeGreaterThan(0)
        expect((await $$('//input[@placeholder="MM/YY"]')).length).toBeGreaterThan(0)
        expect((await $$('//input[@placeholder="123"]')).length).toBeGreaterThan(0)
        expect((await $$('//input[contains(@placeholder,"DEMO10")]')).length).toBeGreaterThan(0)
    })

    it('invalid coupon shows error message', async () => {
        const coupon = await $('//input[contains(@placeholder,"DEMO10")]')
        await coupon.clearValue()
        await coupon.setValue('BADCODE')
        await $('//button[contains(text(),"Apply")]').click()
        await browser.waitUntil(
            async () => (await browser.getPageSource()).toLowerCase().includes('invalid'),
            { timeout: 15000 }
        )
        const src = await browser.getPageSource()
        expect(src.toLowerCase()).toContain('invalid')
    })

    it('valid coupon DEMO10 applies 10% discount', async () => {
        const coupon = await $('//input[contains(@placeholder,"DEMO10")]')
        await coupon.clearValue()
        await coupon.setValue('DEMO10')
        await $('//button[contains(text(),"Apply")]').click()
        await $('//*[contains(text(),"10%")]').waitForDisplayed({ timeout: 15000 })
        const src = await browser.getPageSource()
        expect(src).toContain('10%')
    })

    it('completing order shows confirmation page', async () => {
        await $('//input[contains(@placeholder,"Main St")]').setValue('123 Test Ave, Springfield, CA 90210')
        await $('//input[contains(@placeholder,"1234 5678")]').setValue('4242 4242 4242 4242')
        await $('//input[@placeholder="MM/YY"]').setValue('12/28')
        await $('//input[@placeholder="123"]').setValue('123')

        const coupon = await $('//input[contains(@placeholder,"DEMO10")]')
        await coupon.setValue('DEMO10')
        await $('//button[contains(text(),"Apply")]').click()
        await $('//*[contains(text(),"10%")]').waitForDisplayed({ timeout: 15000 })

        await $('//button[contains(text(),"Place Order")]').click()
        await browser.waitUntil(
            async () => (await browser.getUrl()).includes('/order/'),
            { timeout: 30000 }
        )
        expect(await browser.getUrl()).toContain('/order/')
        await $('//*[contains(text(),"Order Confirmed")]').waitForDisplayed({ timeout: 30000 })
        const src = await browser.getPageSource()
        expect(src).toContain('Order Confirmed')
    })
})
