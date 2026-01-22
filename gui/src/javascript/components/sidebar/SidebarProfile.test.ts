///
/// Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
///
/// This program is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// This program is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with this program.  If not, see <https://www.gnu.org/licenses/>.
///

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

