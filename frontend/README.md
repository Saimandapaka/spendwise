# SpendWise — Chat UI (Angular)

A basic chat interface wired to your Spring Boot backend from step 3.
Type an expense, or ask a question — replies render as text or a chart
depending on what the backend sends back.

Built with the real Angular CLI (`ng new`), so this is a genuine, complete
Angular project — not hand-assembled files. It's been verified with an
actual `ng build` (production compile) and `ng serve` (dev server boot),
both clean, no errors.

## What's here

```
spendwise-chat-ui/
  src/app/
    models/chat.model.ts        # matches the backend's JSON contract exactly
    services/chat-api.service.ts # calls POST http://localhost:8080/api/chat
    chat-message/                # renders one message as text or chart
    chat/                        # the chat page: input box + message list
    app.config.ts                 # HttpClient + Chart.js providers
```

## Setup

```
npm install
npm start
```

Then open http://localhost:4200. Make sure your Spring Boot backend
(step 3) is running on `localhost:8080` first — this UI calls it directly,
no proxy configured.

If your backend runs somewhere else, change `baseUrl` in
`src/app/services/chat-api.service.ts`.

## How it works

1. You type a message, hit Send (or Enter).
2. `ChatApiService` POSTs `{ userId, message }` to `/api/chat`.
3. The backend's response (`{ replyText, render, chart }`) comes back.
4. `ChatComponent` pushes it into the message list as an assistant message.
5. `ChatMessageComponent` shows `replyText` always, and additionally
   renders a chart via `ng2-charts` when `render === "chart"`.

`userId` is hardcoded to `1` in `ChatComponent` for now, matching the
single dev user seeded in step 1 — swap this out once you add real auth.

## Known gaps (fine for a demo, worth fixing before anything real)

- **No loading/error polish beyond the basics** — a "Thinking…" bubble and
  a simple error banner if the backend call fails, nothing fancier.
- **`userId` hardcoded** — expected until auth exists.
- **No transaction history / dashboard view yet** — this UI only covers
  the chat surface; a list/dashboard view is a separate future step.

## Next step

Drop the updated `WebConfig.java` (included alongside this) into your
backend's `com.spendwise.config` package — it adds CORS support so the
browser will actually let this UI talk to `localhost:8080`. Without it,
every request from this UI would fail with a CORS error in the browser
console even though `curl` worked fine earlier (CORS is enforced by
browsers, not by `curl`, which is why this didn't show up during backend
testing).
