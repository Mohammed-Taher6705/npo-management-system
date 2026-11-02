# Phase 1 frontend

Implemented in this working copy:
- Lookup CRUD for skills, item categories and need categories.
- Search, required trimmed fields, loading and empty states, mutation feedback and duplicate submission protection.
- Abort stale list requests when switching lookup types.
- Shared REST client for health, lookups and donations; configurable VITE_API_URL.
- Responsive lookup layout and labelled controls.
- Default landing route opens Lookups; health remains at /health.

Run backend and MongoDB first, then run npm ci and npm run dev inside frontend.
The default /api base uses Vite's proxy to localhost:8080. For other hosting, configure an /api reverse proxy or set VITE_API_URL before building and configure backend CORS.

Verification checklist with a running backend and disposable test data:
1. For each lookup type, create a record, refresh, edit, refresh, then delete.
2. Confirm the record persists after a page reload and is absent after deletion.
3. Reject whitespace-only fields; cancel deletion without changing data.
4. Switch types while a request is pending; the previous type must not overwrite the new table.
5. Stop the backend and check that a useful error appears; restart and Refresh.
6. Check keyboard navigation and mobile layout.

Build and lint pass. Lint reports React effect warnings for data loading in LookupPage and the existing DonationPage.
Live database CRUD and browser interactions have not been verified: port 8080 was unavailable.

Scope: frontend lookup CRUD and MVC integration. This does not certify the complete Phase 1 rubric. The existing design patterns, class diagram and client requirements still require team review. Other module placeholder pages are outside this frontend Phase 1 change.
