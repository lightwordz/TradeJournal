# Google Play Console Copy-Paste Packet — TradeJournal

Package ID: `com.aistudio.tradejournal.kxmpzq`  
Version: `1.0.0` (`versionCode = 1`)  
Grievance / Support Email: `sauravojha124@gmail.com`

---

## 1. Main Store Listing

- **App name (12 / 30 chars):**
  `TradeJournal`

- **Short description (76 / 80 chars):**
  `Offline trading diary, analytics and record-keeping tool for NSE, BSE & MCX.`

- **Full description:**
```text
TradeJournal is an offline trading diary, analytics and record-keeping tool for organizing your past Indian equity, F&O, and MCX trades privately on your device.

What TradeJournal Does
• Personal trade ledger: Log your own completed buys/sells across Equity Delivery, Equity Intraday, Futures, Options, and MCX Commodities using FIFO or Average matching.
• Estimated charge breakdown: Preview editable brokerage presets alongside estimated STT/CTT, exchange fees, SEBI charges, stamp duty, and GST.
• Historical analytics & calendar: Inspect descriptive equity curves, weekday breakdowns, and your tagged setups, moods, and discipline ratings.
• Read-only CSV import & Photo Picker attachment: Import broker CSV files you pick (read-only) and save chart screenshots locally with optional on-device text pre-fill.
• Full local control: Export CSV files or erase all local records anytime.

Important Notice
TradeJournal does NOT execute trades, hold funds, connect to brokers for order placement, or give investment advice, tips, signals, or profit guarantees. Any future broker import integration will be strictly read-only.
Record-keeping tool. Not investment advice. Not SEBI-registered. Calculations are estimates; verify with your broker's contract note. Independent app — not affiliated with NSE, BSE, MCX, Zerodha, Upstox, Groww, or Angel One.
```

- **Store Graphics Ready in `store-assets/`:**
  - `store-assets/play_icon_512.png` (`512 × 512 px`, 32-bit PNG)
  - `store-assets/feature_graphic_1024x500.png` (`1024 × 500 px`, PNG)
  - Capture 2–8 real `1080 × 1920` device screenshots from the live app before uploading.

---

## 2. Privacy & Account Deletion URLs (Host `/docs` via GitHub Pages)

In GitHub **Settings → Pages → Source: Deploy from a branch** → choose **`main`** branch and **`/docs`** folder (zero GitHub Actions required). Once live, replace `<username>` and `<repo>`:
- **Privacy Policy URL:** `https://<username>.github.io/<repo>/privacy-policy.html`
- **Account / Data Deletion URL:** `https://<username>.github.io/<repo>/delete-account.html`

---

## 3. App Content Declarations

### App Access (For Play Reviewers)
```text
No sign-in, account, OTP, or test credentials are required. On first launch, review the Record-Keeping Disclaimer screen, check the confirmation box, tap "I Understand & Continue", enter any display name, pick any fee preset and capital amount, and tap "Open My Journal" to access all features offline.
```

### Ads Declaration
- **No, my app does not contain ads.**

### Target Audience
- **18 and over** (adult retail financial journaling; not targeted at children).

### Content Rating (IARC)
- Category: **Utility / Productivity / Personal Finance**
- Violence, Sexual Content, Profanity, Controlled Substances, Gambling, User Interaction, Location Sharing, Digital Purchases: **No**

### Data Safety
- Does your app collect or share any user data types? **No** (no accounts, no developer backend, zero ad/analytics SDKs; trades, capital ledger, and screenshots stay in app-private storage).
- **VERIFY in Play Console policy centre:** Confirm whether bundled `com.google.mlkit:text-recognition` (`datatransport`) library telemetry requires disclosing ephemeral Device/App info diagnostics under Data Safety.

### Financial Features Declaration (**VERIFY** exact wording in Play Console)
- Selected scope: **Personal finance tracking / record-keeping & budgeting utility only (None of the regulated financial products apply — no banking, lending, crypto exchange/wallet, broking/order execution, or investment advisory).**
- Reviewer note (if free-text field appears):
```text
TradeJournal is strictly an offline trading diary, analytics and record-keeping tool. It does not execute trades, hold funds, or provide SEBI-regulated investment advice, tips, or buy/sell signals. Exchange/broker names are plain text with non-affiliation notices; CSV import is read-only and any future broker integration will remain strictly read-only.
```

---

## 4. Release Notes (`1.0.0`)

```text
Initial release of TradeJournal: offline trade logging for Indian equity, F&O, and MCX; editable brokerage & statutory charge estimates; calendar heatmap; historical analytics; Photo Picker chart attachment; read-only CSV import & export; and one-tap local data deletion.
```

---

## 5. Screenshot Captions (`1080 × 1920`, Capture from Live Device/Emulator)

1. `01_Disclaimer.png` — `Personal record-keeping diary with clear disclosures`
2. `02_Dashboard.png` — `Track logged P&L after estimated brokerage & taxes`
3. `03_LogTrade.png` — `Record NSE, BSE & MCX trades offline`
4. `04_FeeBreakdown.png` — `Review brokerage & statutory charge estimates`
5. `05_Calendar.png` — `Inspect monthly sessions on your calendar`
6. `06_Analytics.png` — `View equity curves & historical weekday stats`
7. `07_CsvTools.png` — `Read-only CSV import & instant local CSV export`
8. `08_SettingsPrivacy.png` — `100% offline storage & one-tap local data wipe`

---

## 6. Closed Testing Kit (12+ Testers, 14 Continuous Days)

### Tester Invite Message
```text
Thanks for testing TradeJournal v1.0.0! Please join using the Play Closed Test opt-in link below, keep the app installed for at least 14 consecutive days, log a few sample equity/F&O/MCX trades across the week, test rotation, dark mode, and CSV export, and report any UI glitches or unclear numbers. All entries stay 100% offline on your device.
```

### Feedback Form Questions
1. Device model, Android version, and screen form factor (phone / foldable / tablet)?
2. Was the first-run disclaimer and onboarding setup clear?
3. Any text clipping, layout issues, or crashes during rotation or dark/light switching?
4. Do charge estimates, FIFO/Average matching, and CSV import/export behave as expected?
5. Did Photo Picker attachment and local OCR pre-fill work without extra permission prompts?
