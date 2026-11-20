import { useQuery } from '@tanstack/react-query'
import { ChefHat, ClipboardCheck, Wallet } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { homeFor, useAuth } from '../auth'
import { Logo } from '../components/Logo'
import { Button, ErrorNote, Field, inputCls } from '../components/ui'
import { api } from '../lib/api'
import { roleLabel } from '../lib/format'
import type { DemoAccount } from '../lib/types'

export default function Login() {
  const { user, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<Error | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const demo = useQuery({ queryKey: ['demo-accounts'], queryFn: () => api<DemoAccount[]>('/api/auth/demo-accounts'), retry: false })

  if (user) return <Navigate to={homeFor(user.role)} replace />

  async function submit(e?: FormEvent, creds?: { email: string; password: string }) {
    e?.preventDefault()
    const c = creds ?? { email, password }
    setBusy(c.email)
    setError(null)
    try { await login(c.email.trim(), c.password) } catch (err) { setError(err as Error) } finally { setBusy(null) }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      <div className="hidden flex-col justify-between bg-forest-dark p-12 text-ivory lg:flex">
        <Logo light />
        <div>
          <h1 className="max-w-md text-5xl font-medium leading-[1.1]">Pay for the meals you actually eat.</h1>
          <p className="mt-5 max-w-md text-[15px] leading-relaxed text-ivory/75">
            HostelOS gives residents control over what they eat and spend, and gives the mess and administration
            real visibility into demand, hygiene and everyday hostel operations.
          </p>
          <ul className="mt-10 space-y-4 text-sm text-ivory/85">
            <li className="flex items-center gap-3"><ChefHat className="h-5 w-5 text-[#C9A24B]" strokeWidth={1.6} /> Eat on your terms: book meals, billed only when eaten</li>
            <li className="flex items-center gap-3"><ClipboardCheck className="h-5 w-5 text-[#C9A24B]" strokeWidth={1.6} /> Your room, taken care of: cleaning and maintenance</li>
            <li className="flex items-center gap-3"><Wallet className="h-5 w-5 text-[#C9A24B]" strokeWidth={1.6} /> Everything in one place: a single, auditable account</li>
          </ul>
        </div>
        <p className="text-xs text-ivory/50">A better way to live on campus.</p>
      </div>

      <div className="flex items-center justify-center px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden"><Logo /></div>
          <h2 className="text-3xl font-medium text-forest-dark">Welcome back</h2>
          <p className="mt-1 text-sm text-muted">Sign in to your HostelOS account.</p>

          <form onSubmit={submit} className="mt-6 space-y-4">
            <Field label="Email"><input className={inputCls} type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required /></Field>
            <Field label="Password"><input className={inputCls} type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required /></Field>
            <ErrorNote error={error} />
            <Button type="submit" className="w-full" loading={busy === email && !!email}>Sign in</Button>
          </form>

          {demo.data && demo.data.length > 0 && (
            <div className="mt-8">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">Demo accounts · password <code className="rounded bg-black/5 px-1">{demo.data[0].password}</code></p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {demo.data.map((a) => (
                  <button key={a.email} disabled={busy !== null} onClick={() => submit(undefined, a)}
                    className="rounded-lg border border-line bg-paper px-3 py-2.5 text-left transition-colors hover:border-forest hover:bg-forest-soft disabled:opacity-60">
                    <span className="block text-sm font-medium">{roleLabel(a.role)}</span>
                    <span className="block truncate text-xs text-muted">{a.fullName}</span>
                    <span className="block truncate text-xs text-muted">{a.email}</span>
                  </button>
                ))}
              </div>
              <p className="mt-3 text-xs text-muted">These buttons sign in through the real backend. Roles are enforced by the server.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
