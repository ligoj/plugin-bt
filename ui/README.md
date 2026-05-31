# plugin-bt — Vue UI

Vue source for the **bt** service-level plugin (`service:bt`, "Bug
Tracker"), parent of the bug-tracker tools (`bt-jira`). Compiled by Vite
into the Maven plugin JAR at
`../src/main/resources/META-INF/resources/webjars/bt/vue/`, served by the
host at `/main/bt/vue/index.js`.

The legacy `bt.html` was an empty title well — there is no per-subscription
configuration screen. This plugin therefore ships only:

- **i18n** — generic bug-tracking labels (`service:bt:*`).
- **delegation hooks** — `renderFeatures` / `renderDetailsKey` /
  `renderDetailsFeatures` resolve the `bt-<tool>` sub-plugin via
  `subPluginIdFor` (`service:bt:jira:1` → `bt-jira`) and merge its VNodes
  in. A tool plugin (e.g. `plugin-bt-jira`) implements those actions and
  declares `requires: ['bt']`.

## Commands

```bash
npm install
npm run build   # → ../src/main/resources/.../webjars/bt/vue/
npm run lint
npm test        # vitest — manifest + delegation contract tests
npm run dev     # standalone dev harness on :5180
```
