/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        spotify: {
          black: '#000000',
          dark: '#1c1c1c',
          surface: '#282828',
          surfaceLight: '#333333',
          surfaceHover: '#404040',
          green: '#1DB954',
          greenHover: '#1ED760',
          text: '#FFFFFF',
          textSecondary: '#b3b3b3',
          textMuted: '#727272',
        },
      },
    },
  },
  plugins: [],
}