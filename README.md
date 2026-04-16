# QAI Demo Shop

A sample e-commerce app for trying out [QAI Platform](https://useqai.dev) — AI-powered test intelligence for your CI pipeline.

## Try QAI in 3 steps

1. **Fork this repo** — click Fork above
2. **Add your API key** — in your fork: Settings → Secrets and variables → Actions → New secret
   - Name: `QAI_API_KEY`
   - Value: your key from [useqai.dev/settings/api-keys](https://useqai.dev/settings/api-keys)
3. **Open a pull request** — create a branch, make any small change, open a PR

That's it. The CI will run Playwright E2E tests and QAI will analyze the results — AI root cause analysis, failure clusters, and fix suggestions will appear in your dashboard and PR comment.

## What you'll see

- **AI Root Cause Analysis** — why each test failed and how to fix it
- **Trace Analysis** — deep RCA from Playwright traces (network errors, console exceptions, slow actions)
- **Failure Clusters** — recurring patterns across runs
- **Risk Score** — merge recommendation based on test results
- **Code Fixes** — copy-paste ready fixes grounded in your test source
- **Ask QAI** — chat with your test results to dig deeper into any failure

## About the app

The demo shop runs permanently at `https://frontend-production-3988.up.railway.app`. Your fork's tests run against this live instance — no deployment needed.

> Ready to connect your own repo? [Get started →](https://useqai.dev/setup)
