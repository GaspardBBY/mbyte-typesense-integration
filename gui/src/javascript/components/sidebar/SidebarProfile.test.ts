import { describe, expect, it } from 'vitest'

describe('email truncation', () => {
  it('keeps short emails untouched', () => {
    const email = 'a@b.c'
    const max = 50
    const truncated = email.length <= max ? email : email.slice(0, max - 3) + '...'
    expect(truncated).toBe(email)
  })

  it('truncates long emails to max length with ellipsis', () => {
    const email = 'x'.repeat(80) + '@example.com'
    const max = 50
    const truncated = email.length <= max ? email : email.slice(0, max - 3) + '...'
    expect(truncated.length).toBe(max)
    expect(truncated.endsWith('...')).toBe(true)
  })
})

