export function LogoMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="7" fill="#2E4A3A" />
      <path d="M7 15.5 16 8l9 7.5" fill="none" stroke="#F8F5EE" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M11 25V16.5h10V25" fill="none" stroke="#F8F5EE" strokeWidth="2.2" strokeLinejoin="round" />
      <path d="M14.5 25v-5a1.5 1.5 0 0 1 3 0v5" fill="#C9A24B" />
    </svg>
  )
}

export function Logo({ light = false }: { light?: boolean }) {
  return (
    <div className="flex items-center gap-2.5">
      <LogoMark />
      <span className={`font-serif text-[22px] font-semibold leading-none ${light ? 'text-ivory' : 'text-forest'}`}>
        Hostel<span className="text-gold">OS</span>
      </span>
    </div>
  )
}
