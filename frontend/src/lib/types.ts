export type Role = 'STUDENT' | 'WARDEN' | 'ADMIN' | 'MESS_STAFF' | 'HOUSEKEEPING_STAFF' | 'MAINTENANCE_STAFF'
export interface User { id: number; email: string; fullName: string; role: Role }
export interface DemoAccount { email: string; password: string; fullName: string; role: Role }

export type BookingStatus = 'NOT_BOOKED' | 'BOOKED' | 'CANCELLED' | 'CONSUMED' | 'MISSED'
export type MealCategory = 'BREAKFAST' | 'LUNCH' | 'SNACKS' | 'DINNER'
export interface Meal {
  id: number; date: string; category: MealCategory; categoryLabel: string; startTime: string; endTime: string
  menuItems: string[]; price: number; cutoffTime: string; bookingOpen: boolean; available: boolean; imageUrl?: string
  myStatus: BookingStatus; myBookingId?: number
  myFeedback?: { tasteRating: number; hygieneRating: number; comment?: string }
  avgTaste?: number; avgHygiene?: number; ratingCount: number
}
export interface BookingHistoryItem { bookingId: number; meal: Meal; status: BookingStatus; bookedAt: string; consumedAt?: string; chargedAmount?: number }

export interface Accommodation {
  allocationId: number; hostelId: number; hostelName: string; roomId: number; roomNumber: string; floorNo: number
  bedNumber: number; roomCapacity: number; since: string; roommates: string[]
}
export interface ResidentSummary {
  id: number; userId: number; fullName: string; email: string; enrollmentNo: string; course: string
  yearOfStudy: number; phone?: string; status: 'ACTIVE' | 'CHECKED_OUT'
}
export interface FinanceSummary { totalCharges: number; totalPayments: number; totalCredits: number; outstanding: number }
export interface StatementLine {
  id: number; reference: string; type: 'CHARGE' | 'PAYMENT' | 'REFUND' | 'ADJUSTMENT_DEBIT' | 'ADJUSTMENT_CREDIT'
  amount: number; signedAmount: number; description: string; createdAt: string; sourceModule: string
  sourceRef: string; status: string; runningBalance: number
}

export type CleaningStatus = 'REQUESTED' | 'SCHEDULED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export interface Cleaning {
  id: number; residentId: number; residentName: string; enrollmentNo: string; roomLabel: string; date: string
  slot: string; slotLabel: string; instructions?: string; status: CleaningStatus; assignedToId?: number
  assignedToName?: string; completionNotes?: string; cancellable: boolean
}
export interface SlotAvailability { slot: string; label: string; available: boolean }

export type ComplaintStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type ComplaintCategory = 'ELECTRICAL' | 'PLUMBING' | 'FURNITURE' | 'INTERNET' | 'CLEANLINESS' | 'OTHER'
export type ComplaintPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export interface Complaint {
  id: number; residentId: number; residentName: string; roomLabel: string; category: ComplaintCategory
  priority: ComplaintPriority; description: string; status: ComplaintStatus; assignedToId?: number
  assignedToName?: string; resolutionNotes?: string; createdAt: string; updatedAt: string; nextStatuses: ComplaintStatus[]
}

export interface Announcement { id: number; title: string; body: string; createdAt: string }
export interface StudentDashboard {
  profile: ResidentSummary; accommodation?: Accommodation; messSpendThisMonth: number; mealsConsumedThisMonth: number
  upcomingBookedMeals: number; account: FinanceSummary; nextCleaning?: Cleaning; openComplaints: number
  todaysMenu: Meal[]; announcements: Announcement[]
}

export interface MealCounts { booked: number; consumed: number; cancelled: number; missed: number; expected: number; pending: number; revenue: number }
export interface BookingRow {
  bookingId: number; residentId: number; residentName: string; enrollmentNo: string; status: BookingStatus
  bookedAt: string; consumedAt?: string; chargedAmount?: number
}
export interface MealBoard { meal: Meal; counts: MealCounts; bookings: BookingRow[] }
export interface DayBoard { date: string; totals: MealCounts; meals: MealBoard[] }
export interface FeedbackRow {
  id: number; mealId: number; mealDate: string; categoryLabel: string; menu: string; residentName: string
  tasteRating: number; hygieneRating: number; comment?: string; createdAt: string
}

export interface Analytics {
  from: string; to: string
  totals: { expected: number; consumed: number; cancelled: number; missed: number; revenue: number; avgTaste?: number; avgHygiene?: number; reviews: number }
  trend: { date: string; expected: number; consumed: number; cancelled: number; missed: number; revenue: number }[]
  categories: { category: MealCategory; label: string; expected: number; consumed: number; revenue: number; avgTaste?: number; avgHygiene?: number; reviews: number }[]
  popularMeals: MealRank[]; lowestRatedMeals: MealRank[]
  ratingDistribution: { rating: number; taste: number; hygiene: number }[]
}
export interface MealRank { mealId: number; date: string; categoryLabel: string; menu: string; consumed: number; avgTaste?: number; avgHygiene?: number; reviews: number }
export interface Occupancy { totalBeds: number; occupiedBeds: number; availableBeds: number }
export interface AdminDashboard {
  totalResidents: number; occupancy: Occupancy; todayExpected: number; todayConsumed: number; todayPending: number
  todayRevenue: number; monthRevenue: number; avgTaste?: number; avgHygiene?: number; outstandingBalances: number
  pendingCleaning: number; openComplaints: number; mess: Analytics
}
export interface WardenDashboard { totalResidents: number; occupancy: Occupancy; pendingCleaning: number; openComplaints: number }
export interface FinanceReport {
  totalOutstanding: number; totalCharges: number; totalPayments: number
  balances: { residentId: number; fullName: string; enrollmentNo: string; summary: FinanceSummary }[]
}

export interface BedView { id: number; bedNumber: number; residentId?: number; residentName?: string }
export interface RoomView { id: number; roomNumber: string; floorNo: number; capacity: number; occupied: number; beds: BedView[] }
export interface HostelView { id: number; name: string; totalBeds: number; occupiedBeds: number; rooms: RoomView[] }
export interface ResidentRow { resident: ResidentSummary; accommodation?: Accommodation }
export interface BedSuggestion { bedId: number; label: string; occupantsInRoom: number; reason: string }
export interface Staff { id: number; fullName: string; role: Role; email: string }

export interface Clearance { outstanding: number; openComplaints: number; pendingCleaning: number; canClear: boolean; blockers: string[] }
export interface Checkout {
  id: number; residentId: number; residentName: string; enrollmentNo: string; room: string; requestedOn: string
  plannedDate: string; status: 'PENDING' | 'CLEARED' | 'REJECTED'; reviewNote?: string; duesAtReview?: number; clearance?: Clearance
}
