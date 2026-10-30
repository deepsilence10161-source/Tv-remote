# Tv-remote — Mobile Controller App (Parent)

Parent ke phone par chalne wala app. Doosra hissa: [Tv-agent](../Tv-agent) (TV par chalne wala app).

## Is repo me abhi kya hai

| Layer | File | Kaam |
|---|---|---|
| Protocol | `protocol/PairingCode.kt` | 6-digit code se TV ka IP + port nikalna (reverse engineered, tested) |
| Protocol | `protocol/RemoteMessage.kt` | WebSocket protobuf frame encode/decode (hand written, protoc ki zaroorat nahi) |
| Protocol | `protocol/KeyCodes.kt` | Android keycodes — HOME 3, BACK 4, UP 19 ... POWER 26 |
| Transport | `transport/TvDiscovery.kt` | mDNS `_share._tcp` + `_tvagent._tcp`, TXT records se device info |
| Transport | `transport/TvWebSocketClient.kt` | Command channel + exponential backoff reconnect |
| Data | `data/TvDevice.kt` | Device model + capabilities |
| Cloud | `cloud/PushService.kt` | FCM alerts (blocked app, screen time, presence) |
| UI | `ui/MainActivity.kt` | Pairing + remote buttons (base) |

## Build

```bash
# google-services.json Firebase console se daalna padega
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Abhi baaki (roadmap ke hisaab se)

- [ ] Parent login (Firebase Auth — phone OTP)
- [ ] Device management + multi-TV
- [ ] Firestore command relay (ghar se bahar se control)
- [ ] App blocking UI (`/app/list`, `/app/open`)
- [ ] Screen time limits + schedules
- [ ] Usage reports (charts)
- [ ] Emergency lock
- [ ] QR scanner (ZXing) + 6-digit fallback
- [ ] Voice search streaming (16 kHz PCM)
- [ ] Floating quick remote

## Zaroori note — key injection

TV par D-pad/key event bhejne ke liye TV side par koi app `InputManager.injectInputEvent`
nahi kar sakta (INJECT_EVENTS system-only permission hai). Isliye remote command ke 3 raste hain:

1. **TV ka apna remote service** (Android TV Remote v2, port 6466/6467, TLS pairing) — Google TV /
   Android TV par D-pad, keyboard, voice sab chalta hai
2. **ADB over WiFi** (port 5555) — `input keyevent N`, ek baar user authorize kare
3. **Apna Tv-agent** — parental control (block, lock, reports) + app-level actions
   (`/app/open`, screenshot, mirroring). Raw key injection sirf rooted ya ADB-enabled TV par.

Document 04 (Drive: "Tv remote") me poora vistar hai.
