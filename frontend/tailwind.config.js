/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'SF Mono', 'Fira Code', 'monospace'],
      },
      colors: {
        brand: {
          bg: '#08080A',          // Level 0: Main background
          panel: '#121214',       // Level 1: Sidebar, headers, cards
          surface: '#18181C',     // Level 2: Inputs, code blocks, controls
          active: '#222227',      // Level 3: Active items, button hover
          border: '#1F1F24',      // Low contrast borders
          borderLight: '#2E2E36', // Focus state/active borders
          accent: '#8B5CF6',      // Action purple (used sparingly)
          accentHover: '#7C3AED',
          textPrimary: '#F4F4F6', // High contrast text
          textSecondary: '#A1A1AA', // Zinc-400 equivalent for details
          textMuted: '#71717A'    // Zinc-500 equivalent for labels
        }
      }
    },
  },
  plugins: [],
}
