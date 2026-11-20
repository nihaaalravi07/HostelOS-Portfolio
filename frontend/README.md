# HostelOS frontend

React 19 + TypeScript single-page app built with Vite, Tailwind CSS 4 and TanStack Query.

```bash
npm ci
npm run dev      # http://localhost:5173, proxies /api to http://localhost:8080
npm test         # Vitest + Testing Library
npm run build    # type-check (tsc) and production build into dist/
npx oxlint .     # lint
```

Structure: `src/pages/{student,staff,admin}` hold the role-specific screens, `src/components` the shared UI, `src/lib/api.ts` the fetch client (Bearer token in `localStorage`), and `src/auth.tsx` the auth context. Routes are declared in `src/App.tsx`, each wrapped in a role guard that mirrors the backend's `@PreAuthorize` rules (the server remains the authority).

Meal cards show a fallback tile unless an image is placed in `public/food/`; see the main README.
