import { Coffee, Cookie, Soup, Sunrise, Utensils } from 'lucide-react'
import { useState } from 'react'
import type { MealCategory } from '../lib/types'

const style: Record<MealCategory, { bg: string; fg: string; Icon: typeof Soup }> = {
  BREAKFAST: { bg: 'from-[#f1e3bd] to-[#e8d29a]', fg: 'text-[#8a6a1c]', Icon: Sunrise },
  LUNCH: { bg: 'from-[#dbe5cf] to-[#c3d3b2]', fg: 'text-forest', Icon: Utensils },
  SNACKS: { bg: 'from-[#efd5c4] to-[#e3b99f]', fg: 'text-clay', Icon: Cookie },
  DINNER: { bg: 'from-[#d6dde6] to-[#bcc8d7]', fg: 'text-slate', Icon: Soup },
}

/** Meal image with a designed fallback tile, used when no photo is available or it fails to load. */
export function FoodTile({ category, imageUrl, label, className = '' }: { category: MealCategory; imageUrl?: string; label: string; className?: string }) {
  const [failed, setFailed] = useState(false)
  const s = style[category] ?? { bg: 'from-line to-line', fg: 'text-muted', Icon: Coffee }
  if (imageUrl && !failed) {
    return <img src={imageUrl} alt={label} loading="lazy" onError={() => setFailed(true)} className={`object-cover ${className}`} />
  }
  return (
    <div className={`flex items-center justify-center bg-gradient-to-br ${s.bg} ${className}`} role="img" aria-label={label}>
      <s.Icon className={`h-10 w-10 opacity-70 ${s.fg}`} strokeWidth={1.4} />
    </div>
  )
}
