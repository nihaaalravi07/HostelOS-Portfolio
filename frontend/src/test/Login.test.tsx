import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth'
import Login from '../pages/Login'

const demo = [{ email: 'admin@hostelos.in', password: 'hostelos123', fullName: 'Dr. Meera Nair', role: 'ADMIN' }]

function setup() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}><AuthProvider><MemoryRouter initialEntries={['/login']}>
      <Routes><Route path="/login" element={<Login />} /><Route path="/admin" element={<p>Admin home</p>} /></Routes>
    </MemoryRouter></AuthProvider></QueryClientProvider>)
}

describe('Login', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.unstubAllGlobals())

  it('lists demo accounts and signs in through the backend API', async () => {
    const fetchMock = vi.fn(async (url: string, _init?: RequestInit) => {
      if (url === '/api/auth/demo-accounts') return new Response(JSON.stringify(demo), { status: 200 })
      if (url === '/api/auth/login') return new Response(JSON.stringify({ token: 'jwt-token', user: { id: 1, email: 'admin@hostelos.in', fullName: 'Dr. Meera Nair', role: 'ADMIN' } }), { status: 200 })
      return new Response('{}', { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)
    setup()
    await userEvent.click(await screen.findByRole('button', { name: /administrator/i }))
    expect(await screen.findByText('Admin home')).toBeInTheDocument()
    expect(localStorage.getItem('hostelos.token')).toBe('jwt-token')
    const loginCall = fetchMock.mock.calls.find((c) => c[0] === '/api/auth/login')!
    expect(JSON.parse(loginCall[1]!.body as string)).toEqual({ email: 'admin@hostelos.in', password: 'hostelos123' })
  })

  it('shows the server message when credentials are wrong', async () => {
    vi.stubGlobal('fetch', vi.fn(async (url: string) =>
      url === '/api/auth/login' ? new Response(JSON.stringify({ message: 'Invalid email or password' }), { status: 401 }) : new Response('[]', { status: 200 })))
    setup()
    await userEvent.type(screen.getByLabelText('Email'), 'x@y.in')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Invalid email or password'))
  })
})
