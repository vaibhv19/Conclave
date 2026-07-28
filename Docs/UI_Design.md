# Conclave UI/UX Design System Specification

This specification documents the governing visual guidelines, typography scales, color hierarchies, and interactive component specs for **Conclave**, as designed under the **UX Lead** guidelines.

---

## 1. Brand Identity & Logo Rationale

Conclave's brand identity represents its core function: **multi-agent LLM consensus orchestration**. 

### The Consensus Star
Rather than an amateur letter inside a rounded square, the logo is a geometric **Consensus Star**:
- **Outer Orbital Ring (Dashed)**: Represents the multiple independent AI engines orbiting the workspace (Google Gemini, OpenAI GPT, Anthropic Claude).
- **Converging Triad Star (Four-Pointed Vesica Piscis)**: Represents vectors of thought from different model brains converging to a single central consensus point.
- **Negative Space Central Chamber**: The absolute center represents the "Conclave" — the shared workspace state where consensus is finalized.
- **Monochrome-first Compatibility**: The geometry scales cleanly down to `16px` (favicon) and up to high-resolution branding headers.

---

## 2. Color System & Surface Elevation

To achieve the premium, low-contrast aesthetic of tools like Claude, Linear, and Cursor, Conclave implements a level-based surface elevation system:

| Surface Level | HSL / Hex Code | Component Usage | Rationale |
| :--- | :--- | :--- | :--- |
| **Level 0 (Base)** | `#08080A` | Main page canvas background | Deep, low-fatigue slate base. |
| **Level 1 (Panels)** | `#121214` | Sidebars, header decks, cards | Isolates layout sections. |
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

## 4. Spacing & Grid System

All layouts conform to a tight, high-density padding grid to emulate native desktop software rather than a generic SaaS website:
- **Inner Padding**: `p-2` (8px), `p-3.5` (14px), `p-5` (20px).
- **Section Separators**: Thin `1px` borders replacing default margin gaps.
- **Card Radii**: Clean `rounded-lg` (8px) on form elements/buttons, and `rounded-xl` (12px) on main cards.

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
