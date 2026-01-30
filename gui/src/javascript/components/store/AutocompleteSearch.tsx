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

import { useEffect, useRef } from 'react'
import { autocomplete } from '@algolia/autocomplete-js'
import type SearchResult from '../../api/entities/SearchResult'

type AutocompleteSearchProps = Readonly<{
  search: (query: string) => Promise<SearchResult[]>
  onSelect: (id: string) => void
  placeholder: string
  noResultsLabel: string
}>

type AutocompleteItem = {
  type: string
  identifier: string
  explain: string
  value: unknown
  [key: string]: unknown
}

const HIGHLIGHT_START = "<span class='highlighted'>"
const HIGHLIGHT_END = '</span>'

const escapeHtml = (value: string) =>
  value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const toSafeHighlightHtml = (value: string) => {
  if (!value) return ''
  const normalized = value.replaceAll('<span class=\"highlighted\">', HIGHLIGHT_START)
  let result = ''
  let index = 0
  while (index < normalized.length) {
    const start = normalized.indexOf(HIGHLIGHT_START, index)
    if (start === -1) {
      result += escapeHtml(normalized.slice(index))
      break
    }
    if (start > index) {
      result += escapeHtml(normalized.slice(index, start))
    }
    const end = normalized.indexOf(HIGHLIGHT_END, start + HIGHLIGHT_START.length)
    if (end === -1) {
      result += escapeHtml(normalized.slice(start))
      break
    }
    const text = normalized.slice(start + HIGHLIGHT_START.length, end)
    result += `${HIGHLIGHT_START}${escapeHtml(text)}${HIGHLIGHT_END}`
    index = end + HIGHLIGHT_END.length
  }
  return result
}

export function AutocompleteSearch({ search, onSelect, placeholder, noResultsLabel }: AutocompleteSearchProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!containerRef.current) return

    const instance = autocomplete<AutocompleteItem>({
      container: containerRef.current,
      placeholder,
      openOnFocus: true,
      detachedMediaQuery: '',
      getSources({ query }) {
        const trimmed = query.trim()
        if (!trimmed) return []
        return [
          {
            sourceId: 'store-search',
            async getItems() {
              const results = await search(trimmed).catch(() => [])
              return results.map((item) => ({
                type: item.type,
                identifier: item.identifier,
                explain: item.explain,
                value: item.value,
              }))
            },
            onSelect({ item, setQuery, setIsOpen }) {
              setQuery('')
              setIsOpen(false)
              onSelect(item.identifier)
            },
            templates: {
              item({ item }) {
                const title = escapeHtml(item.identifier ?? '')
                const meta = escapeHtml(item.type ?? '')
                const explain = item.explain ? toSafeHighlightHtml(item.explain) : ''
                return `
                  <div class="mbyte-search-item">
                    <div class="mbyte-search-item__header">
                      <div class="mbyte-search-item__title" title="${title}">${title}</div>
                      ${meta ? `<div class="mbyte-search-item__meta">${meta}</div>` : ''}
                    </div>
                    ${explain ? `<div class="mbyte-search-item__snippet">${explain}</div>` : ''}
                  </div>
                `
              },
              noResults() {
                return `<div class="mbyte-search-empty">${escapeHtml(noResultsLabel)}</div>`
              },
            },
          },
        ]
      },
    })

    return () => {
      instance.destroy()
    }
  }, [onSelect, placeholder, search])

  return <div className="mbyte-search" ref={containerRef} />
}
