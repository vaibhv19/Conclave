# Conclave UI/UX Design System Specification

This specification documents the governing visual guidelines, typography scales, color hierarchies, and interactive component specs for **Conclave**, as designed under the **UX Lead** guidelines.

---

## 1. Brand Identity & Logo Rationale

Conclave's brand identity represents its core function: **multi-agent LLM consensus orchestration**. 

### The Consensus Star
Rather than an amateur letter inside a rounded square, the logo is a geometric **Consensus Star**:
- **Outer Orbital Ring (Dashed)**: Represents the multiple independent AI engines orbiting the workspace (Google Gemini, OpenAI GPT, Anthropic Claude).
- **Converging Triad Star (Four-Pointed Vesica Piscis)**: Represents vectors of thought from different model brains converging to a single central consensus point.
- **Background Independence**: Made fully solid without background cutout dependencies so it renders crisply on any theme color (Level 0 base, Level 1 headers, and monochrome favicon assets).
- **Cache-Busting Favicon**: Linked with version parameters (`favicon.svg?v=2`) to force prompt updates across browser sessions.
- **Monochrome-first Compatibility**: The geometry scales cleanly down to `16px` (favicon) and up to high-resolution branding headers.

---

## 2. Color System & Surface Elevation

To achieve the premium, low-contrast aesthetic of tools like Claude, Linear, and Cursor, Conclave implements a level-based surface elevation system:

| Surface Level | HSL / Hex Code | Component Usage | Rationale |
| :--- | :--- | :--- | :--- |
| **Level 0 (Base)** | `#08080A` | Main page canvas background | Deep, low-fatigue slate base. |
| **Level 1 (Panels)** | `#121214` | Solid opaque headers, sidebars, cards | Isolates layout sections. No transparency. |
| **Level 2 (Elevated)** | `#18181C` | Input boxes, buttons, code blocks | Surface for actionable inputs. |
| **Level 3 (Active)** | `#222227` | Active selection, button hover | Direct feedback state. |
| **Border (Subtle)** | `#1F1F24` | Default container separators | Thin 1px grid alignments. |
| **Border (Focus)** | `#2E2E36` | Focused inputs, hovered cards | High-fidelity highlight state. |

### Color Accents (Used Sparingly):
- **Purple (`#8B5CF6`)**: Actions and primary trigger button states (e.g. Initialize Workspace, active role badges).
- **Amber (`#EAB308`)**: Pipeline suspended status indicators (`[SYSTEM_HALT]`).
- **Emerald (`#10B981`)**: Active WebSocket connection indicators.
- **Red (`#EF4444`)**: Deletion flags and system errors.

---

## 3. Typography Scale

Conclave enforces a clear hierarchy based on size, weights, and tracking:

- **Primary Sans-Serif (`Inter`)**: Used for all standard interface labels, headings, and descriptions to ensure readability.
- **Data Monospace (`JetBrains Mono` / `SF Mono`)**: Used for execution statuses (`STATUS::ACTIVE`), telemetry numbers, token usage audit metrics, database identifiers, and keyboard shortcuts.

### Typography Hierarchy Scale:
1.  **Workspace Title**: `text-lg font-bold tracking-tight text-white uppercase`
2.  **Section Headers**: `text-xs font-mono font-bold uppercase tracking-wider text-zinc-300`
3.  **Monospace Labels**: `text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted`
4.  **Body Chat Content**: `text-xs leading-relaxed text-zinc-300`
5.  **Telemetry Numbers**: `text-xs font-mono font-bold text-zinc-200`

---

## 4. Spacing, Grid System, & Container Reduction

To match premium layout tools like Trajectory, Linear, and Cursor Settings, Conclave reduces reliance on nested card blocks and border wrappers:
- **Continuous Surface Flow**: Avoids enclosing form components inside heavy bordered panels. Section margins (`mb-12`) and thin horizontal lines (`border-t border-brand-border/60 pt-12`) separate logical scopes.
- **AI Execution Pipeline List**: Renders model and color mappings as a clean list of grid rows (similar to a GitHub file list or Linear issue tracker) instead of individual card blocks, separating them with `divide-y divide-brand-border/60`.
- **Inner Padding**: Padding inside panels is minimized (e.g. `p-5` in sidebars), while open breathing spaces (`pt-16 pb-12` in setup forms) are maximized.
- **Card Radii**: Card corners are limited to form buttons/inputs (`rounded`) and standard outer login blocks (`rounded-xl`).
- **Quiet Elements**: Secondary button styles are kept minimal (e.g. transparent background with thin border boundaries) to ensure the primary initialize buttons command direct focus.

---

## 5. Form UX & Chrome Autofill Protection

To prevent browsers from injecting default white or yellow backgrounds on input elements when credentials autofill is active, Conclave forces background inset shadows:
```css
input:-webkit-autofill {
  -webkit-text-fill-color: #F4F4F6;
  -webkit-box-shadow: 0 0 0px 1000px #18181C inset !important;
}
```
Focus rings are managed via transition borders (`transition-border duration-150`) changing from `#1F1F24` to `#2E2E36` dynamically.

---

## 6. WCAG AA Accessibility Compliance

- **Text Contrast**: Standard body text `#F4F4F6` on Level 0 (`#08080A`) achieves a contrast ratio of **19.4:1**, far exceeding the WCAG AAA threshold.
- **Borders & Controls**: Inputs and buttons utilize clear focus rings and text labels alongside icons to convey state changes to screen readers.
