# CanvasFlow Studio — Performance Engine, Nested Folder Hierarchy & Adaptive Home Navigation

This update eliminates drawing and pinch-to-zoom frame drops through path caching and viewport culling, introduces an unlimited nested folder hierarchy with favorites and search, and launches into a variable-screen-size Home Page Explorer optimized for phones, foldables, and tablets.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices incorporate your confirmed selections from our planning interview:

- **Confirmed Decision 1 — Variable-Screen Home & Folder Layout**: Adaptive split view (`NavigationRail` + Folder Tree Sidebar + Multi-Column Sheet Grid) on tablets, foldables, and landscape displays, transitioning smoothly to a responsive breadcrumb-driven folder & sheet grid on compact mobile portrait screens.
- **Confirmed Decision 2 — Nested Folder Capabilities**: Unlimited parent-child subfolder depth (`parentFolderId`) with an interactive breadcrumb path bar, custom folder accent colors and icons, global instant search filter, and one-tap actions to move, duplicate, or favorite sheets across any folder depth.
- **Confirmed Decision 3 — Startup Flow & Navigation**: The app launches directly into the **Home Page Explorer** featuring pinned favorites, recent sheets, and the root folder workspace, with seamless navigation into the full-bleed Drawing & Writing Studio Canvas and a quick Home return button in the top floating HUD.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms CanvasFlow from a single-drawer sketchpad into a high-frame-rate, multi-screen creative workspace and structured notebook manager. Users can organize unlimited nested folders (e.g., *Architecture -> Semester 2 -> Floor Plans*), search and favorite sheets, and jump into a 60/120fps drawing and handwriting canvas.
- **Target Audience / Persona**: Tablet illustrators, students taking structured handwritten lecture notes across subjects, architects organizing multi-level project sketches, and mobile users needing fast multi-screen note organization.
- **Key Value**:
  1. **Zero-Lag Ink & Pinch-to-Zoom**: Eliminates per-frame `Path` allocations and main-thread recomposition overhead during drawing and zooming.
  2. **Structured Multi-Level Knowledge Base**: Unlimited nested folders with breadcrumbs, color coding, and sheet movement/duplication.
  3. **True Variable-Screen Dependability**: Proportional float-based layouts (`0.0f..1.0f`) on both the Home Page Explorer and the Drawing Canvas across any window size or split-screen ratio.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home Page Explorer (Launch Screen)**:
     - Displays a responsive studio header with quick search, filter chips (*All Explorer*, *Favorites*, *Recent Sheets*), and a live **Screen Posture & Float Allocation Badge**.
     - Shows an interactive **Breadcrumb Bar** (`Root Workspace > Design Studio > Wireframes`) allowing one-tap traversal up any ancestor level.
     - Displays a **Subfolders Section** (with custom folder color badges, icon indicators, and item counts) followed by the **Sheets Grid** (showing paper template preview, category tag, favorite star, stroke count, and overflow actions to Move, Duplicate, Rename, or Delete).
  2. **Creating & Managing Nested Folders**:
     - Tapping **+ New Folder** at any folder depth opens the Folder Creator with name input, custom pigment color selector, and icon picker, creating the folder inside the currently active `parentFolderId`.
     - Tapping **Move Sheet** on any canvas card opens a hierarchical destination picker to relocate the sheet to any folder or back to the Root Workspace.
  3. **Transitioning Between Home & Drawing Canvas**:
     - Tapping any sheet card (or **+ New Sheet in Folder**) opens the **Drawing & Writing Studio** scoped to that sheet.
     - The top floating Studio HUD now includes a prominent **Home Explorer** button (and supports system Back navigation) to save changes asynchronously and return to the exact folder depth in the Home Page Explorer.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Tactile Architectural Drafting Studio—combining obsidian/charcoal studio chrome with warm archival paper previews and crisp technical telemetry.
  - *Color Palette & Mood*:
    - `--background`: Studio Obsidian (`#121316`)
    - `--surface`: Studio Charcoal (`#1C1E24`) and Elevated Slate (`#282B34`)
    - `--primary`: Vermilion Studio (`#E85D04`)
    - `--secondary`: Drafting Teal (`#2A9D8F`)
    - `--accent`: Amber Gold (`#F4A261`)
  - *Typography & Hierarchy*:
    - **Display & Section Headers**: `Space Grotesk` (Bold, tight tracking for studio titles and folder headers).
    - **Body & Navigation Controls**: `Plus Jakarta Sans` (Clean legibility for folder names, breadcrumbs, and menus).
    - **Telemetry & Float Metrics**: `JetBrains Mono` (Monospace precision for zoom percentages, float screen-coverage readouts, and stroke counts).
  - *Variable-Screen Layout & Float Adaptation*:
    - **Compact Mobile Portrait (< 600dp width)**: Single-pane vertical layout with horizontal scrolling breadcrumb bar, 2-column folder grid (`0.48f` card width fraction), and responsive sheet cards with bottom floating creation actions.
    - **Mobile Landscape & Foldable Medium (600dp – 839dp width)**: Dual-zone layout with a left Navigation & Folder Tree column (`0.30f` screen width fraction) and a right Content Explorer pane (`0.70f` screen width fraction).
    - **Tablet Studio Deck (≥ 840dp width)**: Three-part canonical studio layout featuring a left `NavigationRail`, a dedicated Nested Folder Tree & Quick Filters pane (`0.26f` width fraction), and a multi-column Sheet & Subfolder Explorer grid (`0.74f` width fraction).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Fixing Canvas Lag via Cached `Path` Geometry & State Separation**:
  - *Chosen Approach*:
    1. **Pre-Computed `Path` Caching**: Previously, `buildSmoothPath(stroke.points)` rebuilt a new `androidx.compose.ui.graphics.Path` for every committed stroke on *every single frame* during drawing and pinch-to-zoom. We will cache the compiled `Path` and bounding box inside each committed `DrawnStroke` so committed strokes render in $O(1)$ path lookup without allocating objects during `onDraw`.
    2. **In-Progress Stroke Isolation & `drawWithCache`**: Keep the live pointer-tracking state local to the Canvas draw phase using a lightweight point buffer and `graphicsLayer` hardware acceleration, avoiding full-screen ViewModel `StateFlow` emissions until the finger lifts.
    3. **Viewport Frustum Culling & Grid LOD (Level of Detail)**: Skip drawing committed strokes whose bounding boxes fall outside the current `(panOffset, zoomFloat)` visible viewport, and dynamically step paper grid spacing when zoomed far out (`zoomFloat < 0.5f`) so grid line counts remain bounded.
    4. **Background `Dispatchers.Default` Serialization**: Run stroke/note string serialization and Room database writes strictly on background coroutines (`Dispatchers.IO` / `Dispatchers.Default`) so auto-saving never blocks the UI thread.
  - *Why*: Guarantees smooth 60–120fps ink responsiveness and fluid pinch-to-zoom even on complex drawings with hundreds of strokes.

- **Decision 2 — Self-Referencing Relational Schema for Unlimited Nested Folders**:
  - *Chosen Approach*: Introduce a `FolderEntity` table in Room with `id`, `parentFolderId: Long?` (`null` for Root Workspace), `name`, `colorArgb`, `iconKey`, and `createdAtMillis`, and add `folderId: Long?`, `isFavorite: Boolean`, and `previewColorArgb: Long` to `SketchPageEntity`.
  - *Why*: A self-referencing adjacency model (`parentFolderId`) supports unlimited nesting depth, instant subfolder queries, fast breadcrumb path resolution, and clean folder deletion/re-parenting without arbitrary depth limits.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                         │
│             Adaptive Root Router (HomeExplorer <-> StudioCanvas)           │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┴─────────────────────────────┐
        ▼                                                           ▼
┌───────────────────────────────────────┐   ┌────────────────────────────────┐
│   AdaptiveHomeExplorerScreen (Variable│   │  StudioWorkspaceScreen (Canvas)│
│             Screen Size)              │   │                                │
│ ┌───────────────────────────────────┐ │   │ ┌────────────────────────────┐ │
│ │ Posture & Float Layout Engine     │ │   │ │ High-FPS Cached Path Canvas│ │
│ │ • Mobile: Breadcrumb + Grid       │ │   │ │ • O(1) Cached Stroke Paths │ │
│ │ • Foldable/Tablet: Split Tree     │ │   │ │ • Viewport Frustum Culling │ │
│ │   Pane (0.26f) + Content (0.74f)  │ │   │ │ • Adaptive LOD Paper Grid  │ │
│ └─────────────────┬─────────────────┘ │   │ └──────────────┬─────────────┘ │
│                   ▼                   │   │                ▼               │
│ ┌───────────────────────────────────┐ │   │ ┌────────────────────────────┐ │
│ │ Folder & Sheet Management Modals  │ │   │ │ Floating Studio HUD & Docks│ │
│ │ • New Subfolder (Color + Icon)    │ │   │ │ • Quick Home Return Button │ │
│ │ • Move / Duplicate / Favorite     │ │   │ │ • Pinch-Zoom & Color Picker│ │
│ └───────────────────────────────────┘ │   │ └────────────────────────────┘ │
└───────────────────┬───────────────────┘   └────────────────┬───────────────┘
                    └─────────────────────┬──────────────────┘
                                          ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   StudioViewModel (MVVM Reactive State)                    │
│ • Navigation State: Home Explorer vs Active Canvas + Active Folder Stack   │
│ • Derived Breadcrumb Path, Subfolder List, Filtered/Searched Sheets        │
│ • Off-Thread Auto-Save Debouncer (Dispatchers.Default + Dispatchers.IO)    │
└─────────────────────────────────────────┬──────────────────────────────────┘
                                          ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                  SketchRepository & StudioDatabase (Room)                  │
│ ┌───────────────────────────────────┐   ┌────────────────────────────────┐ │
│ │ FolderEntity (sketch_folders)     │   │ SketchPageEntity (sketch_pages)│ │
│ │ • id (PK)                         │◄──┤ • id (PK), folderId (FK/null)  │ │
│ │ • parentFolderId (Self-FK / null) │   │ • title, categoryTag, template │ │
│ │ • name, colorArgb, iconKey        │   │ • isFavorite, strokesPayload   │ │
│ └───────────────────────────────────┘   └────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **`FolderEntity` (`sketch_folders`)**:
  - `id: Long` (Primary Key)
  - `parentFolderId: Long?` (`null` represents the Root Workspace; non-null points to parent folder `id` for unlimited nesting)
  - `name: String`
  - `colorArgb: Long` (Custom folder pigment color)
  - `iconName: String` (Folder category icon identifier, e.g., `"ARCHITECTURE"`, `"NOTES"`, `"ART"`, `"CODE"`)
  - `updatedAtMillis: Long`
- **`SketchPageEntity` (`sketch_pages` — version 2)**:
  - Adds `folderId: Long? = null` (links sheet to any nested folder or Root when `null`)
  - Adds `isFavorite: Boolean = false` (for 1-tap starring and Home Favorites view)
- **`DrawnStroke` (Runtime Performance Model)**:
  - Pre-builds and stores `cachedPath: Path` and `bounds: Rect` upon creation so `DrawScope` never recomputes Bezier curves on each frame.

### Interactive Component & State Mapping

- **Home Navigation & Breadcrumb Traversal**:
  - Selecting a folder updates `currentFolderId: Long?` in `StudioViewModel`, which reactively computes `breadcrumbPath: List<FolderEntity>` by walking up `parentFolderId` to `null`, filters child `subfolders`, and displays sheets where `sheet.folderId == currentFolderId`.
  - Pressing any breadcrumb chip or the system Back button pops `currentFolderId` to its `parentFolderId` (until reaching Root).
- **Sheet Organization Actions (Move, Duplicate, Favorite, Search)**:
  - **Search Bar & Filter Tabs**: Typing in the search bar or toggling *Favorites* / *Recents* dynamically filters folders and sheets across the entire hierarchy.
  - **Favorite Toggle**: Tapping the star icon on any sheet card immediately flips `isFavorite` in Room.
  - **Duplicate Sheet**: Clones the sheet's serialized strokes, text notes, and paper template into a new `SketchPageEntity` in the same folder with `" (Copy)"` appended.
  - **Move Sheet**: Opens the destination folder selector modal listing Root and all nested folders with visual indentation so the user can move a sheet to any level in one tap.
