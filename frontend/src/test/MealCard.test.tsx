import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MealCard } from '../components/MealCard'
import type { Meal } from '../lib/types'

const base: Meal = {
  id: 7, date: '2026-11-14', category: 'LUNCH', categoryLabel: 'Lunch', startTime: '12:30:00', endTime: '14:00:00',
  menuItems: ['Steamed rice', 'Dal tadka', 'Roti', 'Paneer butter masala'], price: 90, cutoffTime: '2026-11-14T23:59:00',
  bookingOpen: true, available: true, myStatus: 'NOT_BOOKED', ratingCount: 0,
}

function renderCard(meal: Meal) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={qc}><MemoryRouter><MealCard meal={meal} /></MemoryRouter></QueryClientProvider>)
}

describe('MealCard', () => {
  const fetchMock = vi.fn()
  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ ...base, myStatus: 'BOOKED' }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('shows the price before booking and books through the API', async () => {
    renderCard(base)
    expect(screen.getByText('₹90')).toBeInTheDocument()
    expect(screen.getByText('Not booked')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /book this meal/i }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalled())
    expect(fetchMock.mock.calls[0][0]).toBe('/api/meals/7/book')
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
  })

  it('tells a booked student they are only billed once they eat, and lets them cancel', async () => {
    renderCard({ ...base, myStatus: 'BOOKED' })
    expect(screen.getByText(/billed only once eaten/i)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /cancel booking/i }))
    await waitFor(() => expect(fetchMock.mock.calls[0][0]).toBe('/api/meals/7/cancel'))
  })

  it('disables booking after the cutoff and hides the cancel button', () => {
    renderCard({ ...base, bookingOpen: false, myStatus: 'BOOKED' })
    expect(screen.queryByRole('button', { name: /cancel booking/i })).not.toBeInTheDocument()
    expect(screen.getByText(/cutoff passed/i)).toBeInTheDocument()
  })

  it('offers a review only for meals that were eaten', () => {
    const { unmount } = renderCard({ ...base, myStatus: 'CONSUMED' })
    expect(screen.getByRole('button', { name: /rate this meal/i })).toBeInTheDocument()
    expect(screen.getByText(/charged ₹90/i)).toBeInTheDocument()
    unmount()
    renderCard({ ...base, myStatus: 'BOOKED' })
    expect(screen.queryByRole('button', { name: /rate this meal/i })).not.toBeInTheDocument()
  })

  it('a missed meal is not charged', () => {
    renderCard({ ...base, myStatus: 'MISSED', bookingOpen: false })
    expect(screen.getByText('Not charged')).toBeInTheDocument()
  })

  it('requires both ratings before a review can be submitted', async () => {
    renderCard({ ...base, myStatus: 'CONSUMED' })
    await userEvent.click(screen.getByRole('button', { name: /rate this meal/i }))
    const submit = screen.getByRole('button', { name: /submit review/i })
    expect(submit).toBeDisabled()
    await userEvent.click(screen.getAllByRole('radio', { name: '4 stars' })[0])
    expect(submit).toBeDisabled()
    await userEvent.click(screen.getAllByRole('radio', { name: '3 stars' })[1])
    expect(submit).toBeEnabled()
  })
})
