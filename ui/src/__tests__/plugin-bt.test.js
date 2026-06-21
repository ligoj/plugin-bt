/*
 * Contract tests for plugin-bt (service-level Bug Tracking plugin).
 *
 * Covers the manifest, i18n merge, the `subPluginIdFor` mapping, and the
 * parent → child delegation: when a `bt-jira` sub-plugin is registered,
 * plugin-bt's renderFeatures / renderDetailsKey append its VNodes. The
 * sibling plugin-bt-jira repo sits beside this one in the workspace.
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { pluginRegistry, useI18nStore } from '@ligoj/host'
import pluginBtDef from '../index.js'
import { subPluginIdFor } from '../service.js'
import pluginBtJiraDef from '../../../../plugin-bt-jira/ui/src/index.js'

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('plugin-bt manifest', () => {
  it('exports required service-level fields (no requires, no routes)', () => {
    expect(pluginBtDef.id).toBe('bt')
    expect(typeof pluginBtDef.label).toBe('string')
    expect(pluginBtDef.requires).toBeUndefined()
    expect(pluginBtDef.routes).toBeUndefined()
    expect(typeof pluginBtDef.install).toBe('function')
    expect(typeof pluginBtDef.feature).toBe('function')
    expect(pluginBtDef.service).toBeTypeOf('object')
    expect(pluginBtDef.meta).toMatchObject({ icon: expect.any(String), color: expect.any(String) })
  })

  it('install() merges i18n', () => {
    const i18n = useI18nStore()
    pluginBtDef.install()
    expect(i18n.t('service:bt')).toBe('Bug tracking')
    expect(i18n.t('service:bt:nb-issues')).toBe('Issues count')
  })

  it('feature() throws for an unknown action', () => {
    expect(() => pluginBtDef.feature('nope')).toThrow(/no feature "nope"/)
  })

  it('renders nothing without a registered tool plugin', () => {
    expect(pluginBtDef.feature('renderFeatures', { node: { id: 'service:bt:jira:1' }, parameters: {} })).toEqual([])
    expect(pluginBtDef.feature('renderDetailsKey', { node: { id: 'service:bt:jira:1' }, parameters: {} })).toBeNull()
  })
})

describe('subPluginIdFor', () => {
  it('maps a tool/instance node to bt-<tool>', () => {
    expect(subPluginIdFor({ node: { id: 'service:bt:jira:1' } })).toBe('bt-jira')
    expect(subPluginIdFor({ node: { id: 'service:bt:jira' } })).toBe('bt-jira')
  })
  it('returns null when there is no tool segment', () => {
    expect(subPluginIdFor({ node: { id: 'service:bt' } })).toBeNull()
    expect(subPluginIdFor({})).toBeNull()
  })
})

describe('plugin-bt → plugin-bt-jira delegation', () => {
  beforeEach(() => {
    pluginBtDef.install()
    pluginBtJiraDef.install()
    pluginRegistry.register('bt-jira', pluginBtJiraDef)
  })
  afterEach(() => {
    pluginRegistry.remove('bt-jira')
  })

  it('appends the JIRA project browse link to renderFeatures output', () => {
    const result = pluginBtDef.feature('renderFeatures', {
      id: 3,
      node: { id: 'service:bt:jira:1' },
      parameters: { 'service:bt:jira:url': 'https://jira.example.org', 'service:bt:jira:pkey': 'LIGOJ' },
    })
    expect(result.length).toBe(1)
    for (const node of result) expect(node.__v_isVNode).toBe(true)
    expect(result[0].props.href).toBe('https://jira.example.org/browse/LIGOJ')
  })

  it('appends the PKEY chip to renderDetailsKey output', () => {
    const result = pluginBtDef.feature('renderDetailsKey', {
      id: 3,
      node: { id: 'service:bt:jira:1' },
      parameters: { 'service:bt:jira:pkey': 'LIGOJ' },
    })
    // delegateToToolPlugin returns an array; the parent passes it through.
    expect(Array.isArray(result)).toBe(true)
    expect(result.length).toBe(1)
    expect(result[0].__v_isVNode).toBe(true)
  })

  it('does not delegate for a non-jira tool', () => {
    const result = pluginBtDef.feature('renderFeatures', {
      id: 3,
      node: { id: 'service:bt:other:1' },
      parameters: { 'service:bt:jira:url': 'https://jira.example.org', 'service:bt:jira:pkey': 'LIGOJ' },
    })
    expect(result).toEqual([])
  })
})
