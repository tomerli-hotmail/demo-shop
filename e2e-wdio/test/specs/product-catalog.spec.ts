import { expect } from '@wdio/globals'

const BASE_URL = process.env.BASE_URL || 'https://frontend-production-3988.up.railway.app'

describe('Product Catalog', () => {
    beforeEach(async () => {
        await browser.url(BASE_URL)
        await $('a[href*="/products/"]').waitForDisplayed({ timeout: 30000 })
    })

    it('renders at least 10 product cards', async () => {
        const cards = await $$('a[href*="/products/"]')
        expect(cards.length).toBeGreaterThanOrEqual(10)
    })

    it('clicking a product navigates to detail page', async () => {
        await $('a[href*="/products/"]').click()
        await browser.waitUntil(
            async () => (await browser.getUrl()).includes('/products/'),
            { timeout: 15000 }
        )
        expect(await browser.getUrl()).toContain('/products/')
    })

    it('search narrows results to matching products', async () => {
        const search = await $('//input[contains(@placeholder,"earch")]')
        await search.clearValue()
        await search.setValue('bamboo')
        await browser.waitUntil(
            async () => {
                const src = await browser.getPageSource()
                return src.toLowerCase().includes('bamboo')
            },
            { timeout: 15000 }
        )
        const results = await $$('a[href*="/products/"]')
        expect(results.length).toBeGreaterThan(0)
    })

    it('search with no match shows empty state', async () => {
        const search = await $('//input[contains(@placeholder,"earch")]')
        await search.clearValue()
        await search.setValue('zzzznotaproduct')
        await browser.waitUntil(
            async () => {
                const cards = await $$('a[href*="/products/"]')
                return cards.length === 0
            },
            { timeout: 15000 }
        )
        const cards = await $$('a[href*="/products/"]')
        expect(cards.length).toBe(0)
    })

    it('prices are correctly formatted as $X.XX', async () => {
        const cards = await $$('a[href*="/products/"]')
        for (const card of cards.slice(0, 5)) {
            const text = await card.getText()
            expect(text).toMatch(/\$\d+\.\d{2}/)
        }
    })
})
