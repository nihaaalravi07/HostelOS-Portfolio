import { describe, expect, it } from 'vitest'
import { addDays, iso, money, moneyExact, parseDate, titleCase } from '../lib/format'

describe('format helpers', () => {
  it('formats rupees in the Indian numbering system', () => {
    expect(money(107305)).toBe('₹1,07,305')
    expect(money(45)).toBe('₹45')
    expect(moneyExact(90)).toBe('₹90.00')
  })
  it('does not shift date-only strings across time zones', () => {
    expect(iso(parseDate('2026-11-14'))).toBe('2026-11-14')
    expect(iso(addDays(parseDate('2026-11-30'), 1))).toBe('2026-12-01')
  })
  it('title-cases enum values', () => {
    expect(titleCase('IN_PROGRESS')).toBe('In progress')
  })
})
