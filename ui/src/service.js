/*
 * Service layer for plugin "bt" (Bug Tracking, service-level).
 *
 * The legacy `service/bt/bt.js` base class rendered links and detail
 * carousels using tool-specific parameters (`service:bt:jira:*`) — the
 * generic parent did the JIRA tool's work because JIRA was the only
 * tool. In the Vue split, the tool owns its rendering and the parent
 * delegates, exactly like the `vm` → `vm-aws` pattern.
 *
 * So `bt` contributes:
 *   - generic bug-tracking i18n (see ./i18n);
 *   - delegation of the subscription-row hooks (`renderFeatures`,
 *     `renderDetailsKey`, `renderDetailsFeatures`) to the bt-<tool>
 *     sub-plugin resolved from the node id.
 *
 * Kept free of Vue SFC imports so it can be unit-tested without a DOM.
 */
import { pluginRegistry } from '@ligoj/host'

/**
 * Derive the sub-plugin id for a bug-tracker tool subscription. A bt
 * node id is `service:bt:<tool>[:<instance>]` — segment 3 is the tool,
 * so `service:bt:jira:1` → `bt-jira`. Returns null when there is no
 * tool segment to delegate to.
 */
export function subPluginIdFor(subscription) {
  const id = subscription?.node?.id || ''
  const parts = id.split(':').filter(Boolean)
  if (parts.length < 3) return null
  return `${parts[1]}-${parts[2]}`
}

/**
 * Calls `feature(action, subscription)` on the loaded bt-<tool>
 * sub-plugin and returns its VNodes (or an empty array). Degrades to
 * `[]` when nothing is registered, the plugin lacks the action, or the
 * call throws — a sub-plugin must never break the parent's rendering.
 */
export function delegateToToolPlugin(subscription, action) {
  const subId = subPluginIdFor(subscription)
  if (!subId) return []
  const plugin = pluginRegistry.get(subId)
  if (typeof plugin?.feature !== 'function') return []
  try {
    const result = plugin.feature(action, subscription)
    if (result == null) return []
    return Array.isArray(result) ? result : [result]
  } catch (err) {
    if (!new RegExp(`no feature ["']${action}["']`).test(err?.message || '')) {
      console.warn(`[plugin:bt] delegate to ${subId}.${action} threw`, err)
    }
    return []
  }
}

const service = {
  subPluginIdFor,
  delegateToToolPlugin,

  /** Subscription-row buttons — delegated wholesale to the bt-<tool>. */
  renderFeatures(subscription) {
    const out = delegateToToolPlugin(subscription, 'renderFeatures')
    return out.length ? out : []
  },

  /** Resource-key chips for the details column — delegated to the tool. */
  renderDetailsKey(subscription) {
    const out = delegateToToolPlugin(subscription, 'renderDetailsKey')
    return out.length ? out : null
  },

  /** Live detail chips — delegated to the tool. */
  renderDetailsFeatures(subscription) {
    const out = delegateToToolPlugin(subscription, 'renderDetailsFeatures')
    return out.length ? out : null
  },
}

export default service
