# Kathirha — External Integrations Setup

Everything below is **optional**. With nothing configured, the app runs on mocks + the deterministic
AI engine. Add a provider's tokens to flip that integration to **real**. Each section gives you (1) where
to sign up, (2) the **`.env` lines**, and (3) the **PowerShell `$env:` commands** for a local run.

> On this machine the backend runs on **8099** (8080 is taken) and the frontend (Vite) on **5173**.
> After adding any tokens, **restart the backend** so it picks them up.

---

## 0. The local run command (all env in one place)

Set whichever blocks you need in the **same PowerShell window**, then start the backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:SERVER_PORT='8099'

# ... paste any of the $env blocks below here ...

& 'C:\Users\Pc Force\.devtools\apache-maven-3.9.9\bin\mvn.cmd' -f 'C:\Users\Pc Force\kathirha\backend\pom.xml' -DskipTests spring-boot:run
```

Frontend (separate window): `cd kathirha\frontend; npm run dev`

---

## 1. OpenAI (AI text) — you already have this ✅
**.env**
```
AI_PROVIDER=auto
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```
**PowerShell**
```powershell
$env:AI_PROVIDER='auto'; $env:OPENAI_API_KEY='sk-...'; $env:OPENAI_MODEL='gpt-4o-mini'
```
Verify: `GET http://localhost:8099/api/health` → `"aiProvider":"openai"`.

---

## 2. Open banking — GoCardless Bank Account Data (Nordigen) sandbox 🏦
Real, self-serve aggregator with a ready sandbox dataset (the practical alternative to Neotek, which is a
licensed KSA partnership, not a developer sandbox — same `BankProvider` interface, swappable later).

**Get tokens:** sign up at **https://bankaccountdata.gocardless.com** → *Developers → User secrets* →
create a secret → copy **SECRET_ID** and **SECRET_KEY** (free).

**.env**
```
BANK_PROVIDER=gocardless
GOCARDLESS_SECRET_ID=your-secret-id
GOCARDLESS_SECRET_KEY=your-secret-key
GOCARDLESS_COUNTRY=GB
GOCARDLESS_REDIRECT_URL=http://localhost:5173/bank/callback
```
**PowerShell**
```powershell
$env:BANK_PROVIDER='gocardless'
$env:GOCARDLESS_SECRET_ID='your-secret-id'
$env:GOCARDLESS_SECRET_KEY='your-secret-key'
$env:GOCARDLESS_COUNTRY='GB'
$env:GOCARDLESS_REDIRECT_URL='http://localhost:5173/bank/callback'
```
**Use it:** in the app → **Connect bank** → pick **Sandbox Finance** → authorize (sandbox auto-approves) →
you're redirected back and your real (sandbox) transactions are imported, categorized, and your income detected.
> If testing on a phone via ngrok, set `GOCARDLESS_REDIRECT_URL` to `https://<your-ui-ngrok>/bank/callback`.

---

## 3. WhatsApp — Twilio sandbox 💬 (daily question + notifications + two-way)
**Get tokens:** https://console.twilio.com → copy **Account SID** + **Auth Token**.
**Join sandbox:** Console → *Messaging → Try it out → WhatsApp sandbox*; from your phone's WhatsApp,
send `join <your-sandbox-code>` to the sandbox number (e.g. +1 415 523 8886).
**Inbound webhook:** in the sandbox settings, set **"When a message comes in"** to
`https://<your-api-ngrok>/api/whatsapp/webhook` (see ngrok section).

**.env**
```
WHATSAPP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=your-auth-token
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
```
**PowerShell**
```powershell
$env:WHATSAPP_PROVIDER='twilio'
$env:TWILIO_ACCOUNT_SID='AC...'
$env:TWILIO_AUTH_TOKEN='your-auth-token'
$env:TWILIO_WHATSAPP_FROM='whatsapp:+14155238886'
```
**Use it:** in the app → **WhatsApp** page → put your real number in *"Live WhatsApp"* → **Save number** →
**Send daily question**. You'll get it on WhatsApp; reply **A/B/C/D** to answer (points awarded), or **1/2**
to a goal-rescue prompt. Notifications (coupons, mission/goal nudges) also send automatically.
> A daily cron (09:00 Asia/Riyadh) pushes the question to everyone; `POST /api/admin/notify/daily-question`
> (as admin) triggers it on demand.

---

## 4. Email — Mailtrap ✉️
**Get tokens:** https://mailtrap.io.
- **Email Sandbox** (captures mail in a test inbox — best for demos): *Email Testing → Inboxes → SMTP* →
  host `sandbox.smtp.mailtrap.io`, port `2525`, copy username + password.
- **Live send** (real delivery): *Sending Domains → SMTP* → host `live.smtp.mailtrap.io`, port `587`,
  username `api`, password = your API token.

**.env** (sandbox example)
```
EMAIL_ENABLED=true
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your-mailtrap-username
MAIL_PASSWORD=your-mailtrap-password
MAIL_FROM=Kathirha <no-reply@kathirha.app>
```
**PowerShell**
```powershell
$env:EMAIL_ENABLED='true'
$env:MAIL_HOST='sandbox.smtp.mailtrap.io'
$env:MAIL_PORT='2525'
$env:MAIL_USERNAME='your-mailtrap-username'
$env:MAIL_PASSWORD='your-mailtrap-password'
$env:MAIL_FROM='Kathirha <no-reply@kathirha.app>'
```
**Use it:** a welcome email is sent on `POST /api/auth/register`. Check your Mailtrap inbox.

---

## 5. ngrok — view on your phone + receive WhatsApp replies 📱
Free signup at https://ngrok.com → copy your authtoken into an `ngrok.yml` file (kept out of the repo).

```powershell
ngrok start --all --config "path\to\ngrok.yml"
```
This opens two public URLs:
- **ui** → forwards to the React app (5173). **Open this URL on your phone** to use the whole app
  (the app's API calls are proxied to your local backend automatically).
- **api** → forwards to the backend (8099). Put `https://<api-url>/api/whatsapp/webhook` into Twilio's
  inbound webhook, and use `https://<ui-url>/bank/callback` as `GOCARDLESS_REDIRECT_URL`.

> Add your ngrok UI URL to `CORS_ORIGINS` if you hit CORS errors:
> `$env:CORS_ORIGINS='http://localhost:5173,https://<your-ui-ngrok>'`

---

## Quick verification
| Integration | Check |
|---|---|
| OpenAI | `/api/health` → `aiProvider: openai` |
| Bank (GoCardless) | `/api/bank/status` → `realEnabled: true`; Connect bank → import works |
| WhatsApp (Twilio) | WhatsApp page → Send daily question → arrives on your phone; reply A/B/C/D |
| Email (Mailtrap) | Register a user → email appears in Mailtrap inbox |
| Phone | Open the ngrok **ui** URL on your phone → Instant Demo works |
