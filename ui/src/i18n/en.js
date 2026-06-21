// Generic bug-tracking i18n for the service-level "bt" plugin. Tool
// plugins (bt-jira) ship their own `service:bt:<tool>:*` parameter
// labels. Flat keys to match the host's vue-i18n resolver.
export default {
  'service:bt': 'Bug tracking',
  'service:bt:no-completion': 'No completion data',
  'service:bt:completion': 'Completion',
  'service:bt:statuses': 'Statuses',
  'service:bt:status-favorite': 'Favorite status',
  'service:bt:nb-issues': 'Issues count',
}
