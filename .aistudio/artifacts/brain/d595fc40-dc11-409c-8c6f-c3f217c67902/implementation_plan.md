# Chess Helper — Real-time Overlay Chess Assistant

Chess Helper is an educational assistant designed to run unobtrusively alongside Chess.com on Android. It captures the screen, locates the chessboard, classifies pieces, analyzes positions using a multi-variation chess engine, and draws dynamic colored hint arrows and evaluation badges directly over the active game board.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been confirmed for implementation:
>
> - **Chess Engine Architecture**: High-performance Kotlin-based chess engine with UCI compatibility, MiniMax with Alpha-Beta pruning, transposition tables, piece-square tables, mobility evaluation, and MultiPV line evaluation heuristics (Safe, Aggressive, Positional, Tactical).
> - **Computer Vision Pipeline**: Hybrid vision system combining color profiling, square grid sampling, template correlation, piece occupancy differential tracking, and orientation detection (coordinate OCR heuristics & initial piece layouts).
> - **Dual Operation Mode**: Full Android system overlay (`SYSTEM_ALERT_WINDOW` + `MediaProjection` foreground service) complemented by an interactive in-app **Board Simulator & Testing Lab** so users can immediately test move hints, engine evaluations, and explanations without needing another app installed.

---

## 1. Overview & Core Concept

- **What It Does**: Reads the Android device screen via MediaProjection, isolates the Chess.com board ROI, extracts FEN positions, validates legal moves, calculates top engine recommendations, and renders non-intrusive vector arrows directly onto the screen.
- **Target Audience**: Beginners and intermediate players looking to learn tactical patterns, understand why moves are played, and review positions in real-time or post-game.
- **Key Value**: Real-time educational feedback without copy-pasting FENs or manually setting up positions in separate tools, with zero touch interference on the underlying game.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Onboarding & Permissions**:
   - Permission walkthrough with clear disclosures: Overlay permission (`SYSTEM_ALERT_WINDOW`), Screen capture (`MediaProjection`), and Notifications (`POST_NOTIFICATIONS`).
   - Educational safety note: Offline analysis, privacy notice (no data leaves device), and clarification that the app is an educational tool independent from Chess.com.
2. **Main Hub**:
   - Status dashboard with one-tap "Launch Assistant" Floating Bubble toggle.
   - Quick settings: Engine Depth (6–20), MultiPV (1–3 arrows), Style preset (Safe, Aggressive, Positional, Tactical), "Think First" timer.
   - Interactive **Live Simulator & Playground**: Allows playing test positions, analyzing custom FENs, and testing arrow rendering immediately.
3. **Floating Overlay & Floating Bubble**:
   - Draggable bubble snapping smoothly to screen edges.
   - Tap opens a compact glassmorphism control drawer:
     - Pause / Resume tracking
     - Style selector chip
     - Engine evaluation badge (e.g., `+1.4` or `M3`)
     - "Explain Move" button (e.g., *"Knight forks Queen and Rook on c7"*, *"Pins bishop to king"*)
     - Quick hide / minimize
4. **Game Analysis & Move History**:
   - View detected moves list, evaluation graph, and accuracy/blunder classification (Great, Good, Inaccuracy, Mistake, Blunder).

### Visual Identity & Theme

- **Aesthetic Direction**: Sleek, modern chess-tech dark theme with high-contrast tactical accents and translucent frosted glass (glassmorphism) controls.
- **Color Palette**:
  - Dominant Background: Deep Obsidian (`#0F141C`) and Slate Surface (`#1A2230`)
  - Accent Best Move (Arrow 1): Emerald Green (`#00E676`)
  - Accent Alternative 2 (Arrow 2): Amber Gold (`#FFD600`)
  - Accent Alternative 3 (Arrow 3): Cyan Glow (`#00E5FF`)
  - Evaluation Advantage: Electric Cyan / Crimson Red for White / Black advantages
  - Text: High-contrast Off-White (`#F0F4F8`) and Muted Slate (`#8E9BAE`)
- **Typography & Layout**:
  - Display Headers: JetBrains / Inter font style with bold numerical readouts.
  - Generous 48dp touch targets on floating mini-window buttons.
  - Smooth spring animations for floating bubble drawer expansion and arrow fade-in/out.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Pure Kotlin Engine vs. Native NDK Process**
  - *Chosen Approach*: Comprehensive, self-contained Kotlin chess engine featuring bitboard representations, alpha-beta pruning, quiescence search, piece-square tables, and MultiPV output.
  - *Why*: Guaranteed compilation across all Android architectures without NDK toolchain fragility, instant warm-up, and predictable battery footprint with thermal throttling integration.
- **Decision 2: Non-Intrusive Overlay Layers**
  - *Chosen Approach*: Two distinct windows:
    1. Fullscreen transparent pass-through canvas (`FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`) for arrow rendering that never intercepts Chess.com touch inputs.
    2. Floating draggable bubble (`FLAG_NOT_FOCUSABLE`) with touchable mini-drawer for user control.
  - *Why*: Ensures seamless touch passthrough to the underlying game while keeping controls easily accessible.
- **Decision 3: Move Stability Filter (Finite State Machine)**
  - *Chosen Approach*: Triple-frame confirmation with chess move legality validation between state transitions.
  - *Why*: Eliminates flickering and false piece detections during piece animations or finger drags.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Android OS                                │
│       MediaProjection API        │       WindowManager Overlay         │
└───────────────────┬──────────────┴─────────────────▲───────────────────┘
                    │ Video/Frames                   │ Draw Arrows/UI
                    ▼                                │
┌───────────────────────────────────────┐            │
│   ChessOverlayService (Foreground)    │            │
│  ┌─────────────────────────────────┐  │            │
│  │   ScreenCaptureManager          │  │            │
│  │   (ImageReader RGBA_8888, FPS)  │  │            │
│  └────────────────┬────────────────┘  │            │
│                   ▼                   │            │
│  ┌─────────────────────────────────┐  │            │
│  │   ComputerVisionDetector        │  │            │
│  │   - Board Quad / ROI Finder     │  │            │
│  │   - 8x8 Grid Normalizer (64px)  │  │            │
│  │   - Piece Feature Classifier    │  │            │
│  │   - Stability & FEN Parser      │  │            │
│  └────────────────┬────────────────┘  │            │
│                   ▼ FEN Position      │            │
│  ┌─────────────────────────────────┐  │            │
│  │   ChessEngineService            │  │            │
│  │   - MiniMax / Alpha-Beta        │  │            │
│  │   - MultiPV Ranking & Styles    │  │            │
│  │   - Move Explanation Generator  │  │            │
│  └────────────────┬────────────────┘  │            │
│                   ▼ Best Moves & Eval │            │
│  ┌─────────────────────────────────┐  │            │
│  │   OverlayWindowManager          ├──┴────────────┘
│  │   - Canvas Arrow Renderer       │
│  │   - Interactive Floating Bubble │
│  └─────────────────────────────────┘
└────────────────────────────────────────────────────────────────────────┘
                    ▲
                    │ State & Preferences
┌───────────────────┴────────────────────────────────────────────────────┐
│                    Chess Helper Compose App                            │
│  - Onboarding & Permissions                                            │
│  - Dashboard & Engine Settings (DataStore)                             │
│  - Interactive Board Simulator & Testing Lab                           │
│  - Game Review & Explanation Explorer                                  │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Modules & Components

1. **`core`**:
   - `ChessBoard`: Bitboard/array board representation, 64 squares, FEN parser/generator.
   - `Move`: From/To squares, promotion, flags (capture, castling, en-passant).
   - `MoveValidator`: Full legal move generation (checks, pins, castling rights).
2. **`engine`**:
   - `ChessEngine`: MiniMax search with Alpha-Beta pruning, iterative deepening, quiescence search, piece-square tables, and MultiPV line evaluation.
   - `EnginePreset`: Safe, Aggressive, Positional, Tactical filtering.
   - `MoveExplainer`: Rule-based tactical classifier (forks, pins, hanging pieces, back-rank mates, center control) generating human-friendly Russian explanations.
3. **`vision`**:
   - `BoardDetector`: Finds 8x8 chessboard quad in screen frames, adjusts for orientation.
   - `PieceClassifier`: Square color difference analysis, template feature matching, and neural fallback for 13 classes (6 white, 6 black, empty).
   - `PositionTracker`: Finite State Machine (`Idle`, `SearchingBoard`, `Tracking`, `Paused`) with 3-frame stability filtering.
4. **`overlay`**:
   - `ChessOverlayService`: MediaProjection foreground service managing lifecycle.
   - `OverlayArrowView`: Canvas drawing path arrows with tapered shafts and directional heads.
   - `FloatingBubbleView`: Movable draggable bubble with magnetic screen edge docking and Compose mini-control drawer.
5. **`ui`**:
   - `OnboardingScreen`: Permissions grant flow with Android system intent shortcuts.
   - `DashboardScreen`: Start/Stop overlay, thermal status, quick presets.
   - `SimulatorScreen`: Interactive chessboard testing lab with live arrows & explanations.
   - `SettingsScreen`: Engine depth, MultiPV, arrow colors, think-first delay, power-saving mode, licensing & source links.

---

### Verification & Testing Plan

1. **Unit & Logic Verification**:
   - Test FEN parsing and move generation with complex positions (castling, en-passant, promotions).
   - Validate MoveExplainer tactical pattern detection (forks, pins, skewers).
2. **In-App Simulator Verification**:
   - Test move recommendations, evaluation updates, and arrow rendering across light and dark board themes.
3. **Compilation**:
   - Complete project build verification via `compile_applet`.
