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
          bg: '#080809',        // almost black background
          card: '#0f0f11',      // elevated panels
          surface: '#141417',   // buttons and inputs
          border: '#1f1f23',    // subtle border
          borderLight: '#2c2c35', // active focus border
          textMuted: '#888896', // low contrast labels
          accent: '#8b5cf6',    // purple for active triggers
          accentHover: '#7c3aed'
        }
      }
    },
  },
  plugins: [],
}
