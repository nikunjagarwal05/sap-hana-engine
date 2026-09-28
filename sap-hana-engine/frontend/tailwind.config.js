/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        sap: {
          blue: '#0a6ed1',
          dark: '#1d232a',
          accent: '#054e96',
          gold: '#f0ab00'
        }
      }
    },
  },
  plugins: [],
}
