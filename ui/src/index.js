/*
 * Plugin "bt" — Bug Tracking (service-level).
 *
 * Parent of the bt-<tool> plugins (bt-jira). It owns no view of its own
 * — the legacy `bt.html` was an empty title well — so it ships only:
 *   - generic bug-tracking i18n;
 *   - the parent→child delegation hooks that merge a tool plugin's row
 *     features / detail chips (e.g. plugin-bt-jira's project link) into
 *     its own output, resolved via `subPluginIdFor`.
 *
 * Authored as source — compiled to `/main/bt/vue/index.js` by Vite.
 * Shared host surface is imported from `@ligoj/host` and kept external
 * at build so plugin and host share the same instances.
 */
import { useI18nStore } from '@ligoj/host'
import enMessages from './i18n/en.js'
import frMessages from './i18n/fr.js'
import service from './service.js'

const features = {
  renderFeatures: service.renderFeatures,
  renderDetailsKey: service.renderDetailsKey,
  renderDetailsFeatures: service.renderDetailsFeatures,
}

export default {
  id: 'bt',
  label: 'Bug Tracker',
  // No routes / component — bug-tracker screens come from the tool
  // plugins and the host's generic subscription rows.
  install() {
    const i18n = useI18nStore()
    i18n.merge(enMessages, 'en')
    i18n.merge(frMessages, 'fr')
  },
  feature(action, ...args) {
    const fn = features[action]
    if (!fn) throw new Error(`Plugin "bt" has no feature "${action}"`)
    return fn(...args)
  },
  service,
  meta: { icon: 'mdi-bug', color: 'red-darken-2' },
}

export { service }
