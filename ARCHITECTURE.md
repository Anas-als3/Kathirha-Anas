# Kathirha — Architecture

## Overview

```
┌──────────────────────┐      HTTPS/JSON      ┌─────────────────────────────────────────────┐
│  React + Vite + TW    │  ───────────────▶   │  Spring Boot API  (JWT, stateless)           │
│  (SPA, nginx in prod) │  ◀───────────────   │                                              │
└──────────────────────┘                      │  web ──▶ service ──▶ repository ──▶ DB        │
                                              │                │                              │
                                              │                ├─ service/ai   (AiCoach)      │
                                              │                ├─ service/bank (mock OB)      │
                                              │                └─ service/whatsapp (mock)     │
                                              └───────────────┬──────────────────────────────┘
                                                              │
                                            H2 (dev, in‑memory) / PostgreSQL (prod)
```

Layered, dependency‑inwards:
- **web** — REST controllers + DTOs (`web/dto/Views`, `web/dto/Requests`). No entity ever leaves the service layer.
- **service** — business logic. The hub type is `SpendingProfile`, a monthly‑normalized view of a user's money
  (income, spend by category, amount saved, savings rate) that **every AI feature reads from**.
- **service/ai** — `AiCoach` interface; `DeterministicAiCoach` is the always‑on engine; `OpenAiClient` optionally
  rephrases narrative text; `AiInsightService` persists every output with provenance.
- **service/bank** — `MockOpenBankingProvider` (consent + realistic Saudi transactions, with a preset that shapes
  spending personality and injects a recent‑month anomaly).
- **service/whatsapp** — `WhatsAppService` mock outbox/inbox (+ Twilio structure, inert without creds).
- **repository / domain** — Spring Data JPA over the entities below.

## Data model (key entities)

`User` (phone login, role, integrity status, verified `baselineIncome`, `IncomeLeague`, streak, personality, health score) ·
`Transaction` (signed amount, `TransactionCategory`) · `SavingsGoal` (inflation‑adjusted target + AI plan) ·
`Mission` (type, difficulty, reward, NORMAL/SEASONAL) · `DailyQuestion` (per user/day) ·
`PointsLedgerEntry` (tamper‑evident dual‑wallet ledger) · `ShopItem` / `Redemption` · `Season` ·
`CashbackCard` (seeded catalog) · `WhatsAppMessage` (mock) · `AiInsight` (persisted AI output + `AiSource`).

> Seasonal "challenges" are modelled as missions with `pointsType = SEASONAL` tied to a `seasonId`, keeping the
> schema lean while covering the seasonal‑competition concept.

## The fairness leaderboard (40% cap)

For each user in your `IncomeLeague`, `fairScore = min(savingsRatePercent, 40)`. Players are ranked by `fairScore`
(then raw rate, then id). Because the score is capped at 40% of income, **a student saving 30% out‑ranks a wealthy
saver** — and points earned beyond the cap still grow your wallet but not your rank. `LeaderboardService.explain()`
tells you exactly how much more to save (in SAR) to pass the person above, or that you're tied at the cap.

## Income verification (anti‑cheat)

`IncomeService.detectAndVerify()` derives `baselineIncome` from recurring **SALARY** credits (not self‑reported),
assigns an income league, and flips the account to `VERIFIED`. Only bank‑verified, opted‑in users appear on boards.

## AI strategy — deterministic first, OpenAI optional

1. `DeterministicAiCoach` computes **all numbers and decisions** from `SpendingProfile` (personality thresholds,
   weighted health score, mission/goal math, inflation buffer, anomaly comparison vs prior months, quiz bank).
2. If `OPENAI_API_KEY` is set (`kathirha.ai.provider=auto|openai`), `OpenAiClient` **rephrases narrative strings only**
   — it is instructed never to change numbers — and the result is tagged `AiSource.OPENAI`. Any failure or missing key
   silently falls back to the deterministic text. **The demo is identical and fully functional with no key.**

## Security

- Stateless JWT (`Authorization: Bearer`), BCrypt password hashing.
- Public: `/api/auth/**`, `/api/demo/**`, `/api/health`. Admin‑only: `/api/admin/**` (`ROLE_ADMIN`). Everything else authenticated.
- CORS restricted to configured origins; secrets via env (`JWT_SECRET`, `OPENAI_API_KEY`, DB creds).
- Bean validation on request DTOs; central `GlobalExceptionHandler` returns structured errors.

## Environments

- **dev (default):** H2 in‑memory, `ddl-auto=create-drop`, `DataSeeder` seeds cards, rewards, an active season, and
  9 users (2 demo users + competitors + admin) with imported transactions and detected income.
- **prod:** PostgreSQL, `ddl-auto=update`, seeding runs once when the DB is empty.

## Extensibility

Swap the mock bank for Tarabut/Lean by implementing the provider behind `MockOpenBankingProvider`'s shape; swap the
mock WhatsApp for real Twilio by setting `WHATSAPP_PROVIDER=twilio` + credentials. No other layer changes.
