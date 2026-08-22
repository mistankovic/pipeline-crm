# PipelineCRM QA procedures

These procedures exercise the real UI. An agent or human follows them against
the running app. Expected results are in **bold**.

Demo seed appears after the first successful sign-in of an empty workspace.

## QA-1 Sign in

1. Open the app signed out.
2. **The sign-in desk is visible** (wordmark “Pipeline”, email form, Google, X).
3. Create an account with email + password (8+ characters) or continue with Google/X.
4. **You land on the board**, not the login page.
5. The header shows your identity and a sign-out control.

## QA-2 Pipeline board

1. After sign-in, **six columns** are visible: Lead, Qualified, Proposal, Negotiation, Won, Lost.
2. Seed deals are present (Fleet telemetry, Clinic rollout, Yard scanners, Arm retrofit, Vision pack, EU service, plus closed analog radios / imaging pilot).
3. Each open card shows company, value, probability bar, and owner.

## QA-3 Drag-and-drop stage change

1. Drag **Vision pack** from Lead onto Qualified.
2. **The card lands in Qualified** and stays after refresh.
3. Drag **Clinic rollout** onto Won.
4. **It stays won** (it already has a meeting and a positive value).
5. Create a new deal “Zero touch” with value `0`, no activity, stage Lead.
6. Drag it onto Won.
7. **The move is rejected** with a message about positive value. The card returns to Lead.

## QA-4 Close-won conversation guard

1. Open **Fleet telemetry**.
2. Note the timeline has only a Note.
3. Click **Move to Won**.
4. **Rejected** — needs a Call or Meeting on the deal.
5. Log a Meeting “Satellite review”.
6. Move to Won.
7. **Stage is Won, probability is 100%.**

## QA-5 Ownership

1. Sign in as a second sales user (or stay as the first user who is MANAGER).
2. If you are MANAGER, the first account is the deal owner on seed data; a second account is SALES.
3. As SALES, open a deal you do not own and try **Move to Proposal**.
4. **Rejected: only the owner or a manager may change this deal.**
5. As MANAGER, the same move **succeeds**.

## QA-6 Deal detail and activity

1. Open **Arm retrofit**.
2. **Value, probability, owner, and timeline** are visible.
3. Log a Call “Procurement ping”.
4. **The call appears at the top of the timeline.**
5. Edit the title to “Arm retrofit — phase 1”.
6. **The heading updates.**

## QA-7 Companies and contacts

1. Open Companies. **Harbor & Co, Lumen Health, Atlas Freight, Northwind Robotics** are listed.
2. Create “Brine Studio” with domain `brine.studio`.
3. **It appears on the list.**
4. Open Contacts. Filter “mina”.
5. **Only Mina Cole remains.**
6. Create a contact on Brine Studio.
7. **The contact lists Brine Studio as company.**

## QA-8 Forecast

1. Open Forecast.
2. **By stage and by owner** totals are shown, currencies not mixed (USD and EUR separately).
3. Close a large USD deal as lost.
4. Return to Forecast.
5. **That deal’s weighted USD amount is gone from the grand total.**

## QA-9 Mobile

1. Resize to ~390px wide.
2. **No horizontal page overflow.** The board columns scroll inside the board, not the page.
3. Nav is a horizontal row under the header.
4. Primary buttons remain tappable (≥44px).

## QA-10 Sign out

1. Use the identity control to sign out.
2. **You return to the login desk.** Visiting `/` redirects to login.
