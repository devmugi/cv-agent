# Eval Run: e7d6564e

**Timestamp:** 2026-01-26T16:40:18.313235Z
**Config:** BASELINE | APP_DEFAULT | ALL_PROJECTS
**Model:** openai/gpt-oss-120b

## Summary

| Metric | Value |
|--------|-------|
| Questions | 8 |
| Conversations | 0 |
| Success Rate | 100% |
| Avg Latency | 2958ms |
| Avg TTFT | 2953ms |
| P50 Latency | 3049ms |
| P95 Latency | 3763ms |
| Total Prompt Tokens | 0 |
| Total Completion Tokens | 0 |
| Total Tokens | 0 |

## Question Results

| ID | Category | Latency | TTFT | Tokens | Suggestions | Status |
|----|----------|---------|------|--------|-------------|--------|
| W1 | personal | 3049ms | 3045ms | 0 | mcdonalds, geosatis | OK |
| W2 | technology | 3786ms | 3779ms | 0 | mcdonalds, food-network-kitchen, geosatis | OK |
| W3 | technology | 3763ms | 3752ms | 0 | mcdonalds, geosatis | OK |
| W4 | featured project | 1841ms | 1835ms | 0 | geosatis, food-network-kitchen | OK |
| W5 | featured project | 3544ms | 3540ms | 0 | adidas-gmr, veev | OK |
| W6 | featured project | 3476ms | 3472ms | 0 | mcdonalds, geosatis | OK |
| W7 | teaching | 2938ms | 2936ms | 0 | android-school, rifl-media, lesara | OK |
| W8 | leadership | 1272ms | 1271ms | 0 | mcdonalds, adidas-gmr, android-school | OK |

## Detailed Responses

<details>
<summary>Click to expand question responses</summary>

### W1: What's Denys's current role?

**Category:** personal

```
Denys Honcharenko is not employed in a permanent position at the moment. He is actively looking for new opportunities, primarily as an **Android Developer** and as an **AI Agent Engineer** – the latter highlighted by his work on the CV Agent app, a Compose Multiplatform project that integrates LLMs via the Groq API.

He is based in **Genk, Belgium** and can be reached at **aidevmugi@gmail.com** or via his LinkedIn profile.

```json
{"suggestions": ["mcdonalds", "geosatis"]}
```
```

### W2: Has he worked with Jetpack Compose?

**Category:** technology

```
Yes. Denys has extensive experience with **Jetpack Compose** and **Compose Multiplatform**.

**Projects that used Compose**

| Project | Role | Compose work |
|--------|------|--------------|
| **McDonald’s Global App** | Lead Android Engineer / KMM Developer | Introduced Jetpack Compose to the team during Android Talks, rewrote several screens from classic Views to Compose, and helped adopt Compose across the delivery and payments feature streams. |
| **Food Network Kitchen** | Android Developer | Developed the Android TV and Fire TV portions of the app and later integrated Compose‑based UI components for the mobile side, improving UI consistency and simplifying the codebase. |
| **GEOSATIS Victim App** | Android Developer / Backend DevOps | Built the victim‑app UI with **Compose Multiplatform**, sharing UI code between Android and iOS while keeping a single, predictable UI layer. |
| **Arcane Design System** (a personal Compose Multiplatform design system) | Creator | Designed a reus
... (truncated)
```

### W3: What's his Kotlin Multiplatform experience?

**Category:** technology

```
Denys has > 8 years of Kotlin Multiplatform (KMP) experience, working on large‑scale production apps and design‑system tooling.

**Key KMP projects**

| Project | Role | KMP contribution |
|---------|------|------------------|
| **McDonald’s Global App** (2021‑2022) | Lead Android Engineer / KMM Developer | Built shared KMP libraries for the Delivery and Payments feature streams, enabling the same business‑logic code to run on Android and iOS. Designed a flexible architecture that supports 20 + market variations and integrated 17 payment providers. |
| **GEOSATIS Victim App** (2022‑2025) | Android Developer / Backend Dev / DevOps | Re‑built the V2 “VAP2” app with **80 % shared code** between Android and iOS using Kotlin Multiplatform and Compose Multiplatform. Implemented real‑time GPS tracking, emergency video capture, and GraphQL subscriptions in the shared module. |
| **Arcane Design System** (internal, not listed in the index) | Lead Engineer | Created a Compose Multiplatform desig
... (truncated)
```

### W4: Tell me about the McDonald's app

**Category:** featured project

```
**McDonald’s Global App (GMAL)**  

- **Role & Scope** – Denys served as **Lead Android Engineer / Kotlin Multiplatform (KMM) Developer** from August 2021 to October 2022. He led the **Delivery** and **Payments** Android teams, handled initial mobile‑only integrations, and contributed as a Scrum Master.  
- **Key Contributions**  
  - **Payments Architecture** – Designed a flexible system supporting 20 + market‑specific payment providers (Adyen, Mastercard, Cybersource, etc.) and built the first shared KMM library for payment business logic.  
  - **Delivery Architecture** – Created a modular KMM library for delivery features, integrating Loginext tracking and handling country‑specific address formats via Firebase Remote Config.  
  - **Jetpack Compose Adoption** – Presented Compose during the internal “Android Talks” series; the team began migrating UI screens to Compose within a month.  
  - **ViewBinding Migration** – Automated the migration from Kotlin Synthetics to ViewBinding usi
... (truncated)
```

### W5: What did he build at GEOSATIS?

**Category:** featured project

```
Denys Honcharenko rescued a failing domestic‑violence protection system and rebuilt it from the ground up as the **GEOSATIS Victim App (VAP2)**. His work included:

* **Full‑stack ownership** – he was the sole Android developer, responsible for the mobile client, backend services, and DevOps.
* **Kotlin Multiplatform & Compose Multiplatform** – 80 % of the codebase was shared between Android and iOS, delivering real‑time GPS tracking, emergency video recording, one‑tap emergency calls and instant alarm notifications via GraphQL subscriptions.
* **Secure kiosk deployment** – deep integration with Samsung Knox Manage and Android Enterprise to lock the app in kiosk mode on victim devices.
* **Custom communication protocol** – rewrote the unstable binary protocol, adding ACKs and retry logic to guarantee reliable message delivery.
* **Backend architecture** – designed and implemented the VAP‑DSS backend (Spring Boot, Kafka, Kubernetes) with dual GraphQL APIs (victim + admin), real‑time ala
... (truncated)
```

### W6: Tell me about the Adidas GMR project

**Category:** featured project

```
**Adidas GMR** – *Google Jacquard Smart Insole for FIFA Mobile*  

- **Period:** February 2020 – June 2020 (5 months)  
- **Client:** Adidas, Google (Jacquard), EA Sports  
- **Role:** *Team Lead in EPAM’s Android team for Adidas GMR* – responsible for leading the Android developers within EPAM, coordinating with the client and other contractors, and ensuring delivery of the Android app.  
- **Key Contributions**  
  1. **BLE Tag Emulator** – built a custom Bluetooth‑Low‑Energy tag emulator that simulated Jacquard sensor data, removing the need for physical hardware during QA and accelerating testing ×10.  
  2. **Jacquard SDK Integration** – refined the BLE protocol and machine‑learning data handling to achieve reliable communication with the Google Jacquard tag.  
  3. **Rescue Mission** – assembled and deployed an EPAM team in one week to Adidas headquarters in Herzogenaurach, Germany, collaborating with Intive and client stakeholders to fix critical issues and release the product o
... (truncated)
```

### W7: Has he trained other developers?

**Category:** teaching

```
Denys has extensive experience mentoring and training developers. He created and led the **Android School** program at EPAM, where he designed the curriculum, taught two course iterations and helped 20 students launch their Android careers – 14 of them were hired by EPAM (a 70 % hiring rate). Beyond the formal program, Denys regularly mentored junior engineers on his project teams, grew Android teams (e.g., expanding the Android group from 1 to 3 developers on the Take It To project), and conducted knowledge‑sharing sessions such as the “Android Talks” initiative on the McDonald’s Global App.  

**Related projects you might explore:**  
```json
{"suggestions": ["android-school", "rifl-media", "lesara"]}
```
```

### W8: Has Denys led teams before?

**Category:** leadership

```
Yes. Denys has held several leadership positions throughout his career:

* **McDonald’s Global App** – He was the Lead Android Engineer and acted as Team Lead for the *Delivery* and *Payments* Android squads, guiding those teams through architecture, KMM integration and feature delivery.  
* **Adidas GMR** – Within EPAM, he served as the Android Team Lead for the Adidas GMR project, coordinating the EPAM Android group that worked on the smart‑insole integration.  
* **Android School** – As the Creator & Lead Instructor, he designed the curriculum and mentored two cohorts of junior developers, effectively leading the educational team.  

These experiences demonstrate his ability to lead both product‑focused development teams and training initiatives.  

```json
{"suggestions": ["mcdonalds","adidas-gmr","android-school"]}
```
```

</details>
