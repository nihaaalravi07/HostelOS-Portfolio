# Demo sequence (about 8 minutes)

Have two browser windows ready: one signed in as **Aarav (student)**, one as **Sunil (mess staff)**.
Use the one-click buttons on the login screen.

1. **The idea (30 s).** Login screen: "Pay for the meals you actually eat."
2. **Student dashboard.** Aarav's room, this month's mess spend ("only meals you actually ate"), balance, upcoming cleaning, announcements, today's menu with prices.
3. **Book a meal.** *Meals* → book today's Dinner. Point out the status badge: *"Billed only once eaten"*. Open *Account*: **nothing was charged**. Booking is not consumption.
4. **Cancel and re-book.** Cancel the booking before the cutoff, then book again.
5. **Staff confirmation (the key moment).** Mess staff window → *Today's board* → find Aarav under Dinner → **Confirm eaten**. Counts, revenue and pending update at once.
6. **Exactly one charge.** Back to Aarav → *Account*: a charge equal to the dinner price appears and the balance rises by exactly that amount. Try confirming again as staff: *"Consumption was already confirmed"*. (The ledger also has a database constraint and an append-only trigger.)
7. **Food accountability.** Aarav → *Meals* → *Rate this meal* (taste + hygiene + comment). Admin → *Food feedback* shows it; *Mess analytics* shows rating distribution, popular and poorly rated meals, expected vs eaten.
8. **Admin overview.** Sign in as admin: occupancy, today's plates, revenue, outstanding balances, charts from real records.
9. **Cleaning.** Aarav → *Room services* → pick a slot (taken slots are disabled; his roommate cannot double-book). Housekeeping staff → *Take this job* → *Start* → *Mark completed* with notes. Aarav → *Track requests* shows the status.
10. **Maintenance.** Aarav → *Report a problem*. Maintenance staff → *Take this* → *Start work* → *Record resolution*. Aarav confirms and closes. Show that illegal jumps are refused (tests cover this).
11. **Accommodation.** Warden → *Residents & rooms*: allocate Karan Mehta (unallocated) using the recommended beds; transfer and release.
12. **Checkout.** Aarav → *Checkout* request. Warden → *Service requests → Checkouts*: blocked by dues and open items. Admin → *Finance* → record payment; then Warden clears it and the bed is released while history is kept.
13. **Permissions.** Swagger UI: call an admin endpoint with the student token and get 403.
