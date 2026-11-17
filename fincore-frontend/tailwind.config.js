/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        /* FinCore semantic palette */
        surface: '#090b13',
        card: '#111522',
        elevated: '#171c2c',
        border: '#252c3d',
        'fc-bg': '#090b13',
        'fc-panel': '#111522',
        'fc-raised': '#171c2c',
        'fc-primary': '#8275ff',
        'fc-cyan': '#4bd4c4',
        'fc-positive': '#37c995',
        'fc-negative': '#ff7185',
      },
      boxShadow: {
        'fc-card': '0 18px 48px rgba(0, 0, 0, 0.2)',
        'fc-glow': '0 12px 36px rgba(109, 93, 252, 0.18)',
      },
      animation: {
        'fade-in': 'fadeIn 0.24s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(5px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
};
