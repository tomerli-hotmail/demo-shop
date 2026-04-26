import type { Options } from '@wdio/types'

const BASE_URL = process.env.BASE_URL || 'https://frontend-production-3988.up.railway.app'

export const config: Options.Testrunner = {
    runner: 'local',
    autoCompileOpts: {
        autoCompile: true,
        tsNodeOpts: {
            project: './tsconfig.json',
            transpileOnly: true,
        },
    },

    specs: ['./test/specs/**/*.ts'],
    exclude: [],

    maxInstances: 1,

    capabilities: [{
        browserName: 'chrome',
        'goog:chromeOptions': {
            args: [
                '--headless=new',
                '--no-sandbox',
                '--disable-dev-shm-usage',
                '--window-size=1280,800',
            ],
        },
    }],

    logLevel: 'warn',
    bail: 0,
    baseUrl: BASE_URL,
    waitforTimeout: 30000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,

    services: ['chromedriver'],

    framework: 'mocha',
    reporters: [
        'spec',
        ['junit', {
            outputDir: './test-results',
            outputFileFormat(options: { cid: string }) {
                return `results-${options.cid}.xml`
            },
        }],
    ],

    mochaOpts: {
        ui: 'bdd',
        timeout: 60000,
    },
}
