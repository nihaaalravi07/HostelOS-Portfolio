import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, Clock, Star } from 'lucide-react'
import { useState } from 'react'
import { api } from '../lib/api'
import { fmtDateTime, fmtShortDate, fmtTime, money } from '../lib/format'
import type { Meal } from '../lib/types'
import { FoodTile } from './FoodTile'
import { Badge, Button, ErrorNote, Field, Modal, Stars, StarInput, inputCls, statusTone } from './ui'

const statusText: Record<string, string> = {
  NOT_BOOKED: 'Not booked', BOOKED: 'Booked', CANCELLED: 'Cancelled', CONSUMED: 'Eaten', MISSED: 'Missed',
}

export function MealCard({ meal, showDate = false }: { meal: Meal; showDate?: boolean }) {
  const qc = useQueryClient()
  const [rating, setRating] = useState(false)
  const refresh = () => ['meals', 'dashboard', 'history', 'account'].forEach((k) => qc.invalidateQueries({ queryKey: [k] }))
  const book = useMutation({ mutationFn: () => api<Meal>(`/api/meals/${meal.id}/book`, { method: 'POST' }), onSuccess: refresh })
  const cancel = useMutation({ mutationFn: () => api<Meal>(`/api/meals/${meal.id}/cancel`, { method: 'POST' }), onSuccess: refresh })
  const err = book.error ?? cancel.error

  return (
    <article className="flex flex-col overflow-hidden rounded-lg border border-line bg-paper shadow-[0_1px_2px_rgba(35,38,43,0.04)]" aria-label={`${meal.categoryLabel} ${meal.date}`}>
      <div className="relative">
        <FoodTile category={meal.category} imageUrl={meal.imageUrl} label={meal.menuItems[0]} className="h-32 w-full" />
        <span className="absolute left-3 top-3 rounded-full bg-paper/95 px-2.5 py-1 text-xs font-semibold text-forest-dark">
          {showDate ? `${fmtShortDate(meal.date)} · ` : ''}{meal.categoryLabel}
        </span>
        <span className="tabular absolute bottom-3 right-3 rounded-md bg-forest-dark px-2.5 py-1 text-sm font-semibold text-ivory">{money(meal.price)}</span>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <div>
          <p className="font-serif text-lg font-medium leading-snug text-forest-dark">{meal.menuItems.slice(0, 2).join(' · ')}</p>
          <p className="mt-1 text-[13px] leading-relaxed text-muted">{meal.menuItems.slice(2).join(', ') || ' '}</p>
        </div>

        <div className="flex items-center justify-between text-xs text-muted">
          <span className="inline-flex items-center gap-1"><Clock className="h-3.5 w-3.5" /> {fmtTime(meal.startTime)} – {fmtTime(meal.endTime)}</span>
          <Stars value={meal.avgTaste} size={13} />
        </div>

        <div className="mt-auto space-y-2.5 border-t border-line pt-3">
          <div className="flex items-center justify-between gap-2">
            <Badge tone={statusTone(meal.myStatus)}>{statusText[meal.myStatus]}</Badge>
            {meal.myStatus === 'BOOKED' && <span className="text-xs text-muted">Billed only once eaten</span>}
            {meal.myStatus === 'CONSUMED' && <span className="text-xs text-muted">Charged {money(meal.price)}</span>}
            {meal.myStatus === 'MISSED' && <span className="text-xs text-muted">Not charged</span>}
          </div>

          {(meal.myStatus === 'NOT_BOOKED' || meal.myStatus === 'CANCELLED') && (
            meal.bookingOpen
              ? <Button className="w-full" loading={book.isPending} onClick={() => book.mutate()}><Check className="h-4 w-4" /> Book this meal</Button>
              : <Button className="w-full" variant="secondary" disabled>{meal.available ? 'Booking closed' : 'Not available'}</Button>
          )}
          {meal.myStatus === 'BOOKED' && (
            meal.bookingOpen
              ? <Button className="w-full" variant="secondary" loading={cancel.isPending} onClick={() => cancel.mutate()}>Cancel booking</Button>
              : <p className="text-center text-xs text-muted">Cutoff passed at {fmtDateTime(meal.cutoffTime)}</p>
          )}
          {meal.myStatus === 'CONSUMED' && (
            <Button className="w-full" variant="secondary" onClick={() => setRating(true)}>
              <Star className="h-4 w-4" /> {meal.myFeedback ? 'Edit your review' : 'Rate this meal'}
            </Button>
          )}
          {meal.bookingOpen && (meal.myStatus === 'NOT_BOOKED' || meal.myStatus === 'CANCELLED' || meal.myStatus === 'BOOKED') && (
            <p className="text-center text-[11px] text-muted">Book or cancel until {fmtDateTime(meal.cutoffTime)}</p>
          )}
          <ErrorNote error={err} />
        </div>
      </div>
      {rating && <FeedbackModal meal={meal} onClose={() => setRating(false)} onDone={refresh} />}
    </article>
  )
}

function FeedbackModal({ meal, onClose, onDone }: { meal: Meal; onClose: () => void; onDone: () => void }) {
  const [taste, setTaste] = useState(meal.myFeedback?.tasteRating ?? 0)
  const [hygiene, setHygiene] = useState(meal.myFeedback?.hygieneRating ?? 0)
  const [comment, setComment] = useState(meal.myFeedback?.comment ?? '')
  const save = useMutation({
    mutationFn: () => api(`/api/meals/${meal.id}/feedback`, { body: { tasteRating: taste, hygieneRating: hygiene, comment } }),
    onSuccess: () => { onDone(); onClose() },
  })
  return (
    <Modal title={`Rate ${meal.categoryLabel.toLowerCase()} · ${fmtShortDate(meal.date)}`} onClose={onClose}>
      <p className="mb-4 text-sm text-muted">{meal.menuItems.join(', ')}</p>
      <div className="space-y-4">
        <StarInput label="Taste and quality" value={taste} onChange={setTaste} />
        <StarInput label="Hygiene" value={hygiene} onChange={setHygiene} />
        <Field label="Comments (optional)">
          <textarea className={inputCls} rows={3} maxLength={500} value={comment} onChange={(e) => setComment(e.target.value)} placeholder="What went well, or what should the mess fix?" />
        </Field>
        <ErrorNote error={save.error} />
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={!taste || !hygiene} loading={save.isPending} onClick={() => save.mutate()}>Submit review</Button>
        </div>
      </div>
    </Modal>
  )
}
