# Kathirha — API Reference

Base URL: `/api` · Auth: `Authorization: Bearer <JWT>` (except public routes) · JSON throughout.

## Auth & demo (public)
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{phone, displayName, email, password}` | Returns `{otp, token, user}` (otp shown for the mock flow) |
| POST | `/auth/login` | `{phone, password}` | Returns `{token, user}` |
| POST | `/auth/verify-otp` | `{phone, code}` | Marks phone verified |
| GET  | `/auth/me` | — | Current user view |
| POST | `/demo/seed` | `?phone=` (optional) | **Judge Demo Mode** — builds scenario, returns `{token, user, scenario}` |
| POST | `/demo/reset` | `?phone=` (optional) | Clears generated demo state |
| GET  | `/health` | — | `{status, aiProvider}` |

## Banking (mock open banking)
| Method | Path | Notes |
|---|---|---|
| POST | `/bank/connect` | Mock consent flow → `{consentId, bankName, accountMask, status}` |
| POST | `/bank/import` | Body `{monthlyIncome?, months?, preset?}` → imports transactions + detects income |
| GET  | `/bank/transactions` | Imported transactions |
| GET  | `/bank/spending` | Category breakdown (monthly) |

## Dashboard & AI
| Method | Path | Notes |
|---|---|---|
| GET | `/dashboard` | Aggregate: personality, health score, anomalies, budget, cashback, breakdown, points, rank, missions, top goal, `aiProvider` |
| GET | `/cashback` | AI cashback‑card recommendation |

## Missions
| Method | Path | Notes |
|---|---|---|
| GET  | `/missions` | All missions |
| POST | `/missions/generate` | Generate fresh AI missions |
| POST | `/missions/{id}/complete` | Complete → `{mission, pointsAwarded, pointsType, newBalance, streakMessage}` |

## Goals
| Method | Path | Notes |
|---|---|---|
| GET  | `/goals` | List |
| POST | `/goals` | `{name, targetAmount, targetDate}` → AI inflation‑aware plan |
| GET  | `/goals/{id}` | One goal |
| POST | `/goals/{id}/contribute` | `{amount}` |
| POST | `/goals/{id}/rescue` | Generate rescue nudge (extend/increase) + WhatsApp message |
| POST | `/goals/{id}/rescue/apply` | `{option: "EXTEND"\|"INCREASE"\|"1"\|"2"}` |

## Leaderboard
| Method | Path | Notes |
|---|---|---|
| GET | `/leaderboard` | `{leagueLabel, capPercent, viewerRank, totalPlayers, entries[]}` |
| GET | `/leaderboard/explain` | AI rank explanation incl. the 40% cap & SAR needed to climb |

## Points, shop, quiz, seasons, WhatsApp
| Method | Path | Notes |
|---|---|---|
| GET  | `/points` | `{normalBalance, seasonalBalance, ledger[]}` |
| GET  | `/shop` | Items with `affordable` flag |
| GET  | `/shop/recommendation` | AI reward recommendation |
| POST | `/shop/{id}/redeem` | Redeem → coupon (also sent to WhatsApp) |
| GET  | `/shop/redemptions` | Your rewards |
| GET  | `/questions/today` | Today's quiz (answer hidden until answered) |
| POST | `/questions/answer` | `{index}` → `{correct, correctIndex, explanation, pointsAwarded, newBalance}` |
| GET  | `/seasons/current` | Active season |
| GET  | `/whatsapp` | Mock message thread |
| POST | `/whatsapp/reply` | `{body}` — two‑way (reply `1`/`2` to act on a goal rescue) |

## Admin (ROLE_ADMIN)
| Method | Path | Notes |
|---|---|---|
| GET | `/admin/insights` | AI product insights `{totalUsers, verifiedUsers, avgSavingsRatePercent, insights[]}` |

### Errors
Structured JSON `{timestamp, status, error, message}` via a global handler. `401` unauthenticated, `403` forbidden
(e.g. non‑admin hitting `/admin/**`), `400` validation/bad request, `404` not found.
