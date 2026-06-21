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
import { toolPluginId, delegateFeature } from '@ligoj/host'

/**
 * Derive the sub-plugin id for a bug-tracker tool subscription. A bt
 * node id is `service:bt:<tool>[:<instance>]` — segment 3 is the tool,
 * so `service:bt:jira:1` → `bt-jira`. Returns null when there is no
 * tool segment to delegate to.
 */
export const subPluginIdFor = toolPluginId

/**
 * Calls `feature(action, subscription)` on the loaded bt-<tool>
 * sub-plugin and returns its VNodes (or an empty array). Degrades to
 * `[]` when nothing is registered, the plugin lacks the action, or the
 * call throws — a sub-plugin must never break the parent's rendering.
 */
export const delegateToToolPlugin = (subscription, action) => delegateFeature(subscription, action, 'bt')

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
