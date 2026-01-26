# Eval Run: 98a73ab4

**Timestamp:** 2026-01-26T16:33:40.149693Z
**Config:** BASELINE | APP_DEFAULT | ALL_PROJECTS
**Model:** openai/gpt-oss-20b

## Summary

| Metric | Value |
|--------|-------|
| Questions | 8 |
| Conversations | 0 |
| Success Rate | 100% |
| Avg Latency | 1797ms |
| Avg TTFT | 1525ms |
| P50 Latency | 922ms |
| P95 Latency | 3214ms |
| Total Prompt Tokens | 0 |
| Total Completion Tokens | 0 |
| Total Tokens | 0 |

## Question Results

| ID | Category | Latency | TTFT | Tokens | Suggestions | Status |
|----|----------|---------|------|--------|-------------|--------|
| W1 | personal | 3189ms | 3187ms | 0 | mcdonalds, geosatis | OK |
| W2 | technology | 763ms | 761ms | 0 | mcdonalds, food-network-kitchen, geosatis | OK |
| W3 | technology | 3684ms | -ms | 0 | - | OK |
| W4 | featured project | 922ms | 919ms | 0 | geosatis, food-network-kitchen, adidas-gmr | OK |
| W5 | featured project | 1097ms | 1093ms | 0 | mcdonalds, food-network-kitchen | OK |
| W6 | featured project | 3214ms | 3211ms | 0 | mcdonalds, geosatis | OK |
| W7 | teaching | 796ms | 794ms | 0 | android-school, lesara | OK |
| W8 | leadership | 718ms | 716ms | 0 | mcdonalds, adidas-gmr, veev | OK |

## Detailed Responses

<details>
<summary>Click to expand question responses</summary>

### W1: What's Denys's current role?

**Category:** personal

```
Denys is currently seeking new opportunities as an **Android Developer** and an **AI Agent Engineer**. He is not employed in a senior Android engineering role at the moment and is open to projects that leverage his extensive experience in Kotlin Multiplatform, Jetpack Compose, and AI‑powered development.

```json
{"suggestions": ["mcdonalds", "geosatis"]}
```
```

### W2: Has he worked with Jetpack Compose?

**Category:** technology

```
Yes. Denys has extensive experience with Jetpack Compose, having used it in multiple high‑profile projects:

- **McDonald’s Global App** – led Android teams and introduced Compose for UI refactoring and new screens.  
- **Food Network Kitchen** – built the Android TV / Fire TV app with a TV‑optimized Compose UI.  
- **GEOSATIS Victim App** – developed a Compose Multiplatform UI for Android, iOS and the web.  

These engagements demonstrate his ability to design, implement, and maintain Compose‑based UIs at scale.

```json
{"suggestions": ["mcdonalds", "food-network-kitchen", "geosatis"]}
```
```

### W3: What's his Kotlin Multiplatform experience?

**Category:** technology

```

```

### W4: Tell me about the McDonald's app

**Category:** featured project

```
Denys Honcharenko’s work on the McDonald’s Global App (GMAL) focused on two key Android feature streams: **Delivery (MDS)** and **Payments**.  
- **Leadership & Scope** – He served as Lead Android Engineer and Kotlin Multiplatform (KMM) developer for the Delivery and Payments teams, coordinating with the iOS side and the backend team in India.  
- **Technical Highlights**  
  - Designed a flexible architecture that supports 20 + market‑specific payment providers (Adyen, Mastercard, Cybersource, etc.) and delivery‑tracking integrations (Loginext).  
  - Implemented a shared KMM library for business logic that runs on both Android and iOS, achieving ~80 % code reuse.  
  - Modernized the UI layer by introducing Jetpack Compose, migrating small‑screen code from Views to Compose within a month after an Android‑Talks presentation.  
  - Led the ViewBinding migration using IntelliJ Structural Search Replace (SSR) tooling, automating refactor of over 100 adapter delegates.  
  - Established t
... (truncated)
```

### W5: What did he build at GEOSATIS?

**Category:** featured project

```
Denys Honcharenko led the full‑stack development of the **GEOSATIS Victim App** – a mobile‑first protection system for domestic‑violence victims. His responsibilities spanned the entire product lifecycle:

| What he built | Key outcomes |
|---------------|--------------|
| **Android app (VAP2)** | 80 % of the code is shared via Kotlin Multiplatform, real‑time GPS tracking, emergency video recording, one‑touch calls, and GraphQL‑based live alarm notifications. |
| **Backend (VAP‑DSS)** | Spring Boot/Kafka micro‑services, dual GraphQL APIs (Victim & Admin), Kafka event streaming, Firebase Auth, and a 1‑second alarm‑latency subscription system. |
| **Infrastructure & DevOps** | Docker/Kubernetes, CI/CD pipelines, Terraform IaC for AWS resources, and extensive observability (ElasticSearch, Kafka, Logstash). |
| **AI/Automation** | MCP Server with Claude Code integration for natural‑language system management and rapid testing. |
| **Security & Compliance** | Samsung Knox Kiosk Mode, Androi
... (truncated)
```

### W6: Tell me about the Adidas GMR project

**Category:** featured project

```
Denys Honcharenko served as the Android Team Lead within EPAM’s Android squad for the Adidas GMR initiative. The project was a collaboration between Adidas, Google’s Jacquard technology, and EA Sports’ FIFA Mobile. Its goal was to turn a smart insole into a game‑ready device that could record real‑world soccer performance and feed that data back to FIFA Mobile for in‑game rewards.

Key points of the engagement:

| Aspect | Details |
|--------|---------|
| **Role** | Android Team Lead (EPAM) – managed the Android side of the product, coordinated with Adidas and Intive teams, and drove the delivery of the Android app. |
| **Scope** | • Integration of the Google Jacquard SDK for Bluetooth Low Energy (BLE) communication.<br>• Development of a BLE tag emulator to accelerate QA and regression testing.<br>• Design of data pipelines that translate sensor readings into in‑game actions. |
| **Achievements** | • Delivered a fully functional Android app that captured kicking events, shot power, di
... (truncated)
```

### W7: Has he trained other developers?

**Category:** teaching

```
Denys has a strong track record of training and mentoring other developers.  
- **Android School (EPAM)** – He created and led the internal “Android School” program, training 20 junior developers and career‑switchers. The program achieved a 70 % hiring rate, with 14 students secured roles at EPAM.  
- **Lesara (Temy)** – While serving as Senior Android Developer, he led architecture discussions, mentored teammates on Clean Architecture, MVP, and Kotlin best practices, and guided the rewrite of the Product List feature.  
- **McDonald’s Global App** – As Lead Android Engineer, he coached Android teams on KMM, Jetpack Compose, and architectural patterns, and organized the Android Talks initiative to share knowledge across feature teams.  

These experiences demonstrate his commitment to elevating the skills of fellow developers.

```json
{"suggestions": ["android-school", "lesara"]}
```
```

### W8: Has Denys led teams before?

**Category:** leadership

```
Yes. Denys has led several development teams throughout his career:

- **McDonald’s Global App** – Led the Android “Delivery” and “Payments” teams, driving feature architecture and sprint execution.  
- **Adidas GMR** – Served as the Android Team Lead within EPAM’s mobile squad, coordinating the delivery of the smart‑sole integration.  
- **Veev Smart Home** – Headed the Android development effort (WallApp and Mobile App) and guided the backend/IoT work across the team.

These roles demonstrate his experience managing cross‑functional teams, setting technical direction, and ensuring on‑time delivery.

```json
{"suggestions": ["mcdonalds", "adidas-gmr", "veev"]}
```
```

</details>
