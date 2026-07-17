/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Kathirha coral — the product accent from the deck (#C25A33 / #DD8463)
        brand: {
          50: '#FBF1EA',
          100: '#F6E3D7',
          200: '#EFC9B4',
          300: '#E6A98B',
          400: '#DD8463',
          500: '#D06A3F',
          600: '#C25A33',
          700: '#A34728',
          800: '#833A22',
          900: '#6B301D',
        },
        navy: {
          50: '#EEF2F8',
          100: '#D9E2EF',
          600: '#27406B',
          700: '#1E3A5F',
          800: '#15243F',
          900: '#102A47',
        },
        gold: {
          100: '#FBF1DA',
          400: '#E8C06B',
          500: '#C9A24B',
          700: '#8A5E0D',
        },
      },
      fontFamily: {
        sans: ['Tajawal', 'IBM Plex Sans Arabic', 'Segoe UI', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
