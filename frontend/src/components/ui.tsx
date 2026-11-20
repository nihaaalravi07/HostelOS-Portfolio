import clsx from 'clsx'
import { Loader2, Star, X } from 'lucide-react'
import { useEffect, type ButtonHTMLAttributes, type ReactNode } from 'react'

export function Button({ variant = 'primary', size = 'md', loading, className, children, disabled, ...rest }:
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger'; size?: 'sm' | 'md'; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={clsx(
        'inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'px-3 py-1.5 text-[13px]' : 'px-4 py-2.5 text-sm',
        variant === 'primary' && 'bg-forest text-ivory hover:bg-forest-dark',
        variant === 'secondary' && 'border border-line bg-paper text-ink hover:bg-forest-soft',
        variant === 'ghost' && 'text-forest hover:bg-forest-soft',
        variant === 'danger' && 'border border-clay/30 bg-paper text-clay hover:bg-clay-soft',
        className,
      )}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  )
}

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return <section className={clsx('min-w-0 rounded-lg border border-line bg-paper shadow-[0_1px_2px_rgba(35,38,43,0.04)]', className)}>{children}</section>
}

export function CardHeader({ title, action, subtitle }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-line px-5 py-3.5">
      <div>
        <h3 className="text-[15px] font-semibold">{title}</h3>
        {subtitle && <p className="mt-0.5 text-[13px] text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-[30px] font-medium leading-tight text-forest-dark">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

type Tone = 'green' | 'amber' | 'red' | 'gray' | 'blue'
const tones: Record<Tone, string> = {
  green: 'bg-forest-soft text-forest',
  amber: 'bg-gold-soft text-gold',
  red: 'bg-clay-soft text-clay',
  gray: 'bg-black/5 text-muted',
  blue: 'bg-slate-soft text-slate',
}
export function Badge({ tone = 'gray', children }: { tone?: Tone; children: ReactNode }) {
  return <span className={clsx('inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium', tones[tone])}>{children}</span>
}

export const statusTone = (s: string): Tone => ({
  BOOKED: 'blue', CONSUMED: 'green', CANCELLED: 'gray', MISSED: 'red', NOT_BOOKED: 'gray',
  REQUESTED: 'amber', SCHEDULED: 'blue', ASSIGNED: 'blue', IN_PROGRESS: 'amber', COMPLETED: 'green',
  OPEN: 'red', RESOLVED: 'green', CLOSED: 'gray',
  LOW: 'gray', MEDIUM: 'blue', HIGH: 'amber', URGENT: 'red',
} as Record<string, Tone>)[s] ?? 'gray'

export function Stat({ label, value, hint, tone }: { label: string; value: ReactNode; hint?: ReactNode; tone?: 'warn' | 'good' }) {
  return (
    <div className="rounded-lg border border-line bg-paper px-4 py-3.5">
      <p className="text-xs font-medium uppercase tracking-wide text-muted">{label}</p>
      <p className={clsx('tabular mt-1 text-2xl font-semibold', tone === 'warn' && 'text-clay', tone === 'good' && 'text-forest')}>{value}</p>
      {hint && <p className="mt-0.5 text-xs text-muted">{hint}</p>}
    </div>
  )
}

export function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-16 text-sm text-muted" role="status">
      <Loader2 className="h-4 w-4 animate-spin" /> {label}…
    </div>
  )
}

export function ErrorNote({ error }: { error: unknown }) {
  if (!error) return null
  return <p role="alert" className="rounded-md border border-clay/30 bg-clay-soft px-3 py-2 text-sm text-clay">{(error as Error).message}</p>
}

export function Empty({ icon, title, hint }: { icon?: ReactNode; title: string; hint?: string }) {
  return (
    <div className="flex flex-col items-center gap-1.5 px-6 py-10 text-center">
      {icon && <div className="mb-1 text-muted">{icon}</div>}
      <p className="text-sm font-medium">{title}</p>
      {hint && <p className="max-w-sm text-[13px] text-muted">{hint}</p>}
    </div>
  )
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink/40 p-0 sm:items-center sm:p-4" onMouseDown={onClose}>
      <div role="dialog" aria-modal="true" aria-label={title} onMouseDown={(e) => e.stopPropagation()}
        className="max-h-[92vh] w-full max-w-lg overflow-y-auto rounded-t-xl bg-paper shadow-xl sm:rounded-xl">
        <div className="flex items-center justify-between border-b border-line px-5 py-3.5">
          <h3 className="font-serif text-lg font-medium text-forest-dark">{title}</h3>
          <button onClick={onClose} aria-label="Close" className="rounded p-1 text-muted hover:bg-black/5"><X className="h-4 w-4" /></button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  )
}

export function Stars({ value, size = 14 }: { value?: number | null; size?: number }) {
  if (value == null) return <span className="text-xs text-muted">No ratings yet</span>
  return (
    <span className="inline-flex items-center gap-1 text-sm font-medium text-gold" title={`${value} out of 5`}>
      <Star className="fill-current" style={{ width: size, height: size }} /> <span className="tabular text-ink">{value.toFixed(1)}</span>
    </span>
  )
}

export function StarInput({ label, value, onChange }: { label: string; value: number; onChange: (n: number) => void }) {
  return (
    <div>
      <p className="mb-1 text-sm font-medium">{label}</p>
      <div className="flex gap-1" role="radiogroup" aria-label={label}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button key={n} type="button" role="radio" aria-checked={value === n} aria-label={`${n} star${n > 1 ? 's' : ''}`}
            onClick={() => onChange(n)} className="rounded p-0.5">
            <Star className={clsx('h-7 w-7', n <= value ? 'fill-gold text-gold' : 'text-line')} />
          </button>
        ))}
      </div>
    </div>
  )
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-muted">{hint}</span>}
    </label>
  )
}

export const inputCls = 'w-full rounded-md border border-line bg-paper px-3 py-2 text-sm placeholder:text-muted/70 focus:border-forest focus:outline-none'

export function Bar({ value, max, tone = 'green' }: { value: number; max: number; tone?: 'green' | 'amber' | 'red' }) {
  const pct = max > 0 ? Math.min(100, (value / max) * 100) : 0
  return (
    <div className="h-1.5 w-full overflow-hidden rounded-full bg-black/5">
      <div className={clsx('h-full rounded-full', tone === 'green' && 'bg-forest', tone === 'amber' && 'bg-gold', tone === 'red' && 'bg-clay')} style={{ width: `${pct}%` }} />
    </div>
  )
}

/** Horizontal tab switcher. */
export function Tabs<T extends string>({ tabs, value, onChange }: { tabs: { id: T; label: string }[]; value: T; onChange: (t: T) => void }) {
  return (
    <div role="tablist" className="mb-5 inline-flex max-w-full overflow-x-auto rounded-lg border border-line bg-paper p-1">
      {tabs.map((t) => (
        <button key={t.id} role="tab" aria-selected={value === t.id} onClick={() => onChange(t.id)}
          className={clsx('whitespace-nowrap rounded-md px-3.5 py-1.5 text-sm font-medium transition-colors', value === t.id ? 'bg-forest text-ivory' : 'text-muted hover:text-ink')}>
          {t.label}
        </button>
      ))}
    </div>
  )
}
