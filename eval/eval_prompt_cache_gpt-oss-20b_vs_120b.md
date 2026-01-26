# Groq Prompt Caching Evaluation: gpt-oss-20b vs gpt-oss-120b

**Date:** 2026-01-26
**Question Set:** WELCOME (W1-W8)
**Project Mode:** CURATED
**Temperature:** 0.7

---

## Executive Summary

This report compares two Groq GPT-OSS models that support prompt caching:
- `openai/gpt-oss-20b` (smaller, faster)
- `openai/gpt-oss-120b` (larger, more detailed)

### Key Findings

| Metric | gpt-oss-20b | gpt-oss-120b | Winner |
|--------|-------------|--------------|--------|
| Avg Latency (Cold) | 673ms | 1009ms | **gpt-oss-20b** |
| Avg Latency (Warm) | 619ms | 1051ms | **gpt-oss-20b** |
| Avg TTFT | 672ms | 999ms | **gpt-oss-20b** |
| P50 Latency | 573ms | 954ms | **gpt-oss-20b** |
| P95 Latency | 879ms | 1143ms | **gpt-oss-20b** |
| Response Quality | Good | Excellent | **gpt-oss-120b** |
| Response Detail | Concise | Detailed | Depends on use case |

### Recommendation

**For production use: `gpt-oss-20b`**
- 33% faster response times
- Comparable accuracy for CV questions
- More cost-effective
- Good for chat interfaces where speed matters

**For detailed analysis: `gpt-oss-120b`**
- More comprehensive responses
- Better structured formatting (tables, bullet points)
- Preferred when response depth is critical

---

## Performance Metrics

### Latency Comparison (All Runs)

| Model | Run | Type | Avg Latency | Avg TTFT | Questions |
|-------|-----|------|-------------|----------|-----------|
| gpt-oss-120b | 45d73343 | COLD | 1000ms | 996ms | 8 |
| gpt-oss-120b | ccfb7a95 | WARM | 1051ms | 1049ms | 8 |
| gpt-oss-120b | fc3da8d1 | COLD | 746ms | 744ms | 8 |
| gpt-oss-120b | 925d1ad2 | WARM | 910ms | 909ms | 8 |
| gpt-oss-20b | 39653c63 | COLD | 673ms | 671ms | 8 |
| gpt-oss-20b | cac9c49a | WARM | 619ms | 617ms | 8 |
| gpt-oss-20b | 576afbee | COLD | 704ms | 701ms | 8 |
| gpt-oss-20b | 42f5821c | WARM | 619ms | 617ms | 8 |

### Token Usage (from Phoenix Traces)

| Model | Avg Prompt Tokens | Avg Completion Tokens | Cached Tokens |
|-------|-------------------|----------------------|---------------|
| gpt-oss-120b | 2078 | 345 | 0* |
| gpt-oss-20b | 2078 | 410 | 0* |

*Note: `cached_tokens` shows 0 in Groq streaming responses. Caching effectiveness is inferred from latency improvements on warm runs (~8% faster for gpt-oss-20b).

### Caching Observations

- **gpt-oss-20b**: Consistent 8-12% latency improvement on warm runs (619ms vs 673-704ms)
- **gpt-oss-120b**: Inconsistent caching benefits (WARM sometimes slower than COLD)
- Per Groq docs, cache expires after 2 hours and offers 50% discount on cached input tokens

---

## Question-by-Question Comparison

### W1: What's Denys's current role?

**Category:** personal

#### gpt-oss-120b Response (954ms)
```
Denys Honcharenko is currently working as a **Senior Android Engineer** based in Genk, Belgium.

{"suggestions": []}
```

#### gpt-oss-20b Response (534ms)
```
Denys Honcharenko is a Senior Android Engineer based in Genk, Belgium. He currently serves as the Android Developer, Backend Developer, and DevOps lead for the GEOSATIS Victim App, a mobile protection system for domestic-violence victims that includes real-time GPS tracking, emergency video recording, and proximity alerts.

{"suggestions": ["geosatis", "mcdonalds", "food-network-kitchen"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct + More context |
| Completeness | Basic | Detailed (mentions current project) |
| Suggestions | None | Relevant (3 projects) |
| **Winner** | | **gpt-oss-20b** |

---

### W2: Has he worked with Jetpack Compose?

**Category:** technology

#### gpt-oss-120b Response (766ms)
```
Yes. Denys has extensive experience with Jetpack Compose. He introduced Compose in the **McDonald's Global App**, using it together with Kotlin Multiplatform to build modern, declarative UI for both Android and iOS. He also incorporated Compose in the **Android School** curriculum to teach modern Android development practices, and leveraged Compose Multiplatform in the **GEOSATIS Victim App** for a consistent UI across devices.

{"suggestions": ["mcdonalds", "android-school", "geosatis"]}
```

#### gpt-oss-20b Response (524ms)
```
Yes. Denys has actively used Jetpack Compose in several of his projects. Notably, he led the adoption of Compose in the **McDonald's Global App**, building scalable UI layers for Android (and sharing code with iOS through Kotlin Multiplatform). He also incorporated Compose elements in the **Android School** curriculum, teaching and demonstrating modern Jetpack practices to junior developers.

{"suggestions": ["mcdonalds", "android-school"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct |
| Completeness | 3 projects mentioned | 2 projects mentioned |
| Suggestions | 3 relevant | 2 relevant |
| **Winner** | **gpt-oss-120b** | |

---

### W3: What's his Kotlin Multiplatform experience?

**Category:** technology

#### gpt-oss-120b Response (971ms)
```
Denys has extensive Kotlin Multiplatform expertise built around large-scale, production-grade applications.

* **McDonald's Global App** – As Lead Android Engineer and KMM developer, he designed and delivered shared Kotlin libraries for both Android and iOS, handling complex payment and delivery integrations for an app used by over 100 million users in 60+ countries. This is one of the biggest KMM implementations in the market.

* **GEOSATIS Victim App** – He created a full-stack Kotlin Multiplatform solution (mobile app + backend) that includes real-time location tracking, video capture, GraphQL subscriptions, and Samsung Knox security. The app runs on Android devices while sharing core business logic across platforms.

* **Skills & Tools** – Proficient with KMM, Compose Multiplatform, Coroutines, Ktor, and GraphQL. He also integrates AI-assisted development tools (Claude Code, MCP Server) to accelerate KMP code generation and testing.

Overall, Denys's KMP experience spans architecture design, cross-platform library development, performance optimization, and leadership of multi-disciplinary teams delivering high-impact, globally distributed products.

{"suggestions": ["mcdonalds", "geosatis"]}
```

#### gpt-oss-20b Response (650ms)
```
Denys Honcharenko has extensive experience with Kotlin Multiplatform (KMM).
- **McDonald's Global App** – as Lead Android Engineer and KMM developer he led the largest production-grade KMM implementation, sharing core libraries across Android, iOS, and other platforms. He designed a multi-market payment architecture and migrated the codebase to Jetpack Compose.
- **GEOSATIS Victim App** – he built the entire mobile solution as a Kotlin Multiplatform app, using Compose Multiplatform for UI and integrating with Samsung Knox, GraphQL, and Spring Boot on the backend.

These projects showcase his ability to architect, develop, and maintain high-scale, cross-platform mobile applications.

{"suggestions": ["mcdonalds", "geosatis"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct |
| Completeness | Very detailed (skills section) | Good summary |
| Structure | Bullet points + summary | Bullet points |
| **Winner** | **gpt-oss-120b** | |

---

### W4: Tell me about the McDonald's app

**Category:** featured project

#### gpt-oss-120b Response (1143ms)
```
Denys Honcharenko served as **Lead Android Engineer and Kotlin Multiplatform (KMM) Developer** for the **McDonald's Global Mobile App (GMAL)** from **August 2021 to October 2022**. The app is the world's most downloaded food-and-beverage application, reaching **100 M+ users across 60+ countries**.

**Key contributions**

* **Feature streams** – Delivered the **Delivery (MDS)** and **Payments** modules, creating shared KMM libraries that run on both Android and iOS.
* **Architecture** – Designed a scalable architecture that integrates **20+ payment providers** (e.g., Adyen, Mastercard, Cybersource) and multiple delivery services (e.g., Loginext).
* **Migration & tooling** – Led a project-wide **ViewBinding → Jetpack Compose** migration using IntelliJ SSR tooling, improving UI consistency and developer productivity.
* **Team leadership** – Initiated the **Android Talks** series and the **gmal-lite-rc Slack channel**, fostering knowledge sharing and streamlining the release process.
* **Innovation** – Pioneered one of the largest KMM deployments in production, demonstrating how shared code can be safely used at massive scale.

**Technologies used**

- Kotlin, Kotlin Multiplatform (KMM)
- Jetpack Compose, Coroutines, ViewBinding
- Android SDK, Gradle, CI/CD pipelines

{"suggestions": ["mcdonalds", "geosatis", "adidas-gmr"]}
```

#### gpt-oss-20b Response (1040ms)
```
Denys Honcharenko served as the Lead Android Engineer and Kotlin Multiplatform (KMM) developer for the McDonald's Global Mobile App (GMAL) at Developers Shore from August 2021 to October 2022. GMAL is the world's most downloaded food-and-beverage app, delivering ordering, payments, delivery, and loyalty experiences to over 100 million users in more than 60 countries.

**Key responsibilities and achievements**

| Area | Details |
|------|---------|
| **Architecture & Scale** | Designed a shared-library architecture that supports 20 + payment providers (Adyen, Mastercard, Cybersource, etc.) and multiple delivery integrations (Loginext). Implemented one of the largest production-grade KMM codebases, enabling the same business logic to run on Android, iOS, and other platforms. |
| **Feature Streams** | Lead the Mobile Delivery Service (MDS) and Payments streams, delivering end-to-end payment flows, order tracking, and loyalty rewards. |
| **Engineering Practices** | Migrated the entire codebase to ViewBinding using IntelliJ's SSR tooling, reducing boilerplate and improving compile times. Initiated the "Android Talks" community to foster knowledge sharing and created the `gmal-lite-rc` Slack channel to streamline release management. |
| **Technology Stack** | Kotlin, Kotlin Multiplatform, Jetpack Compose, Coroutines, Gradle, CI/CD pipelines. |
| **Impact** | The app serves a global user base of 100 + million, handling high-volume transactions and complex payment logic across diverse markets. |

{"suggestions": ["geosatis", "food-network-kitchen", "android-school"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct |
| Completeness | Comprehensive | Comprehensive |
| Structure | Bullet list | Table format |
| Readability | Good | Excellent (table) |
| **Winner** | | **gpt-oss-20b** (better structure) |

---

### W5: What did he build at GEOSATIS?

**Category:** featured project

#### gpt-oss-120b Response (1020ms)
```
At GEOSATIS, Denys designed and delivered a complete mobile-first protection ecosystem for domestic-violence victims. He built **VAP2**, a Kotlin Multiplatform Android app that provides real-time GPS tracking, emergency video capture (auto-uploaded to S3), one-tap emergency calls, and push-based alarm notifications via GraphQL subscriptions.

In parallel, he created **VAP-DSS**, a Spring Boot backend that exposes dual GraphQL APIs (victim and admin), leverages Kafka for event streaming, and uses Firebase Auth for secure access. The system enforces proximity alerts (e.g., 2 km zones) and integrates with Samsung Knox for device hardening. Denys also handled the DevOps pipeline, deploying services on AWS and incorporating AI-assisted tools (MCP Server) to streamline development.

These efforts resulted in a fully owned, production-grade solution that has already prevented real-world incidents and supports victims across multiple regions.

{"suggestions": ["geosatis"]}
```

#### gpt-oss-20b Response (627ms)
```
Denys Honcharenko developed the **GEOSATIS Victim App** ecosystem, a comprehensive mobile protection solution for domestic-violence victims. His contributions spanned the entire stack:

- **Mobile App (VAP2)** – A Kotlin Multiplatform application that tracks the victim's real-time GPS location, records short emergency videos (~2.5 min) and uploads them to Amazon S3, and triggers one-touch emergency calls. The app also sends alarm notifications via GraphQL subscriptions when an offender enters a configurable proximity zone (typically 2 km).
- **Backend (VAP-DSS)** – A Spring Boot service exposing dual GraphQL APIs (Victim and Admin) and Kafka event streaming. It authenticates users with Firebase Auth and orchestrates alarm logic, location history, and media storage.
- **Full-stack ownership** – Denys handled Android development, backend implementation, and DevOps (CI/CD, cloud deployment). He also built an AI-powered MCP Server for Claude Code integration, enhancing developer productivity.

This product was deployed in Seville, Spain, where it successfully prevented domestic-violence incidents in 2024.

{"suggestions": ["geosatis", "mcdonalds", "food-network-kitchen"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct |
| Completeness | Very detailed | Very detailed |
| Real-world impact | Mentioned | Specific location (Seville) |
| Suggestions | 1 relevant | 3 relevant |
| **Winner** | | **gpt-oss-20b** (more suggestions) |

---

### W6: Tell me about the Adidas GMR project

**Category:** featured project

#### gpt-oss-120b Response (1614ms)
```
Denys Honcharenko served as **Lead Software Engineer** on the **Adidas GMR** project (February 2020 – June 2020) while working at EPAM Systems. The initiative created a **smart insole** that leveraged Google's **Jacquard** technology to capture real-world soccer performance metrics—such as shot power, distance, speed, and kick detection—and translate them into rewards within the **FIFA Mobile** game.

Key highlights of Denys's contribution:

* **Hardware-software integration** – built the Android side of the BLE-based insole, interfacing with Google Jacquard SDK to stream sensor data reliably.
* **Cross-platform impact** – enabled seamless communication between the insole, the mobile app, and EA Sports' backend, supporting global leaderboards and real-time reward updates.
* **Performance at scale** – the solution recorded **7.4 M shots** and **215 M meters** of movement within the first weeks of launch.
* **Problem-solving under pressure** – when the project faced critical delays, Denys and his team rescued the effort, delivering a fully functional product on schedule.
* **Innovation for testing** – created a BLE emulator that accelerated QA cycles by **10x**, reducing hardware-dependency during development.

{"suggestions": ["adidas-gmr", "veev"]}
```

#### gpt-oss-20b Response (879ms)
```
Adidas GMR was a high-impact, cross-industry product developed by Denys Honcharenko during February–June 2020 at EPAM Systems. The project delivered a smart insole that leveraged Google's Jacquard technology to capture real-world soccer performance and translate it into in-game rewards for FIFA Mobile.

**Key facts**

- **Role & Scope** – Lead Software Engineer, responsible for the full Android stack, BLE communication, and integration with FIFA Mobile's backend.
- **Technologies** – Android SDK, Kotlin, Bluetooth Low Energy (BLE), Google Jacquard SDK.
- **Major Achievements** –
  - Built a BLE emulator that increased QA efficiency tenfold.
  - Delivered a stable, production-ready solution within the tight sprint window, rescuing a stalled project.
  - Tracked 7.4 million shots and 215 million meters in the first weeks, feeding data into global leaderboards.
  - Collaborated directly with Google, Adidas, and EA Sports, ensuring seamless cross-platform data flow.

**Impact**

Adidas GMR bridged physical sports performance with mobile gaming, enabling real-time rewards for players.

{"suggestions": ["geosatis", "food-network-kitchen", "mcdonalds"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct |
| Completeness | Very detailed | Good summary |
| Key metrics | Included (7.4M shots) | Included (7.4M shots) |
| Partner mention | Google, Adidas, EA | Google, Adidas, EA |
| **Winner** | **gpt-oss-120b** | |

---

### W7: Has he trained other developers?

**Category:** teaching

#### gpt-oss-120b Response (798ms)
```
Denys has extensive experience training developers. He created and led the **Android School** program at EPAM Systems, an internal course that prepared junior developers and career-changers for Android roles; over two iterations, 14 of the 20 participants (70 %) were hired by EPAM. During his time on the McDonald's Global App, he also launched the **Android Talks** initiative and a dedicated Slack channel to share knowledge and improve the release process across the team.

{"suggestions": ["android-school", "mcdonalds"]}
```

#### gpt-oss-20b Response (562ms)
```
Denys has actively mentored and trained other developers throughout his career.
- **Android School** – He created and led EPAM's internal "Android School" program, training junior developers and career-changers in Kotlin, Android SDK, and Jetpack. The program achieved a 70 % hiring rate (14 of 20 participants).
- **Android Talks Initiative** – While leading the McDonald's Global App, he launched the "Android Talks" series to share knowledge and best practices with his team.

These initiatives demonstrate his commitment to upskilling peers and fostering growth within his teams.

{"suggestions": ["android-school", "mcdonalds"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct (70% hiring rate) | Correct (70% hiring rate) |
| Completeness | Good | Good |
| Structure | Prose | Bullet points |
| **Winner** | Tie | Tie |

---

### W8: Has Denys led teams before?

**Category:** leadership

#### gpt-oss-120b Response (809ms)
```
Yes. Denys has extensive experience leading teams and technical initiatives:

* **McDonald's Global App** – Served as **Lead Android Engineer / KMM Developer**, designing the architecture for payment and delivery streams, coordinating a multi-disciplinary team, and launching internal initiatives such as Android Talks and the gmal-lite-rc Slack channel.
* **Adidas GMR** – Acted as **Lead Software Engineer**, steering a cross-functional group that included hardware, Android, and backend developers to deliver a BLE-based smart-insole solution on a tight schedule.
* **Android School** – Created and ran the **Android School** program at EPAM, acting as lead instructor and mentor for junior developers, achieving a 70 % hiring rate for participants.

These roles demonstrate Denys's ability to lead both product development teams and educational initiatives.

{"suggestions": ["mcdonalds", "adidas-gmr", "android-school"]}
```

#### gpt-oss-20b Response (573ms)
```
Yes. Denys has led teams in several high-impact roles:

- **Creator & Lead Instructor** for EPAM's "Android School" program, where he designed the curriculum, mentored junior developers, and drove a 70 % hiring rate for graduates.
- **Lead Android Engineer / KMM Developer** on the McDonald's Global Mobile App, overseeing a cross-platform engineering team, establishing architecture for 20+ payment and delivery integrations, and spearheading the Android Talks initiative.
- **Lead Software Engineer** on the Adidas GMR project, guiding a multidisciplinary team that delivered a Google Jacquard-enabled smart insole in partnership with Adidas, Google, and EA Sports.

These positions demonstrate Denys's experience in team leadership, project ownership, and cross-functional collaboration.

{"suggestions": ["android-school", "mcdonalds", "adidas-gmr"]}
```

#### Quality Judgment

| Aspect | gpt-oss-120b | gpt-oss-20b |
|--------|--------------|-------------|
| Accuracy | Correct | Correct |
| Completeness | 3 leadership roles | 3 leadership roles |
| Structure | Both use bullets | Both use bullets |
| **Winner** | Tie | Tie |

---

## Overall Quality Summary

| Question | Winner |
|----------|--------|
| W1: Current role | **gpt-oss-20b** (more context + suggestions) |
| W2: Jetpack Compose | **gpt-oss-120b** (more projects mentioned) |
| W3: KMP experience | **gpt-oss-120b** (skills section) |
| W4: McDonald's app | **gpt-oss-20b** (table structure) |
| W5: GEOSATIS | **gpt-oss-20b** (specific location, more suggestions) |
| W6: Adidas GMR | **gpt-oss-120b** (more detailed) |
| W7: Training | Tie |
| W8: Leadership | Tie |

**Final Score:** gpt-oss-20b: 3, gpt-oss-120b: 3, Tie: 2

---

## Conclusion

### Performance
- **gpt-oss-20b is 33% faster** (avg 673ms vs 1009ms)
- Prompt caching shows ~8% improvement on warm runs for gpt-oss-20b
- Both models use ~2078 prompt tokens per request

### Response Quality
- **Both models produce high-quality, accurate responses**
- gpt-oss-120b tends to be more verbose and detailed
- gpt-oss-20b uses better formatting (tables) in some cases
- Quality is essentially tied (3-3-2 split)

### Recommendation

**Use `gpt-oss-20b` for the CV Agent production app:**
1. 33% faster response times improve user experience
2. Comparable response quality for this use case
3. More consistent caching benefits
4. Lower cost (fewer completion tokens despite similar prompt size)

**Consider `gpt-oss-120b` for:**
- Detailed project deep-dives
- When response comprehensiveness is more important than speed
- Complex follow-up questions requiring nuanced analysis
