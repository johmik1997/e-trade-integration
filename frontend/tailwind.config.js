/** @type {import('tailwindcss').Config} */
// Theme tokens mirror cbhiv2 (cbhi-frontendv2/frontendcbhiv2/tailwind.config.js) so the two
// front ends read as one product. Keep them in step when that file changes.
export default {
  content: ['./index.html', './src/**/*.{js,ts,vue,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: 'rgba(var(--primary) / <alpha-value>)',
        'primary-600': '#004c99',
        'primary-700': '#003d7a',
        'primary-dark': '#002e5c',
        'primary-light': 'rgba(var(--primary-light) / <alpha-value>)',
        'primary-lighter': 'rgba(var(--primary-lighter) / <alpha-value>)',
        secondary: 'rgba(var(--secondary) / <alpha-value>)',
        accent: 'rgba(var(--accent) / <alpha-value>)',
        'text-clr': 'rgba(var(--text-clr) / <alpha-value>)',
        'text-secondary': 'rgba(var(--text-secondary-clr) / <alpha-value>)',
        'text-border-clr': 'rgba(var(--text-border-clr) / <alpha-value>)',
        'base-clr': 'rgba(var(--base-clr) / <alpha-value>)',
        'base-clr2': 'rgba(var(--base-clr2) / <alpha-value>)',
        'base-clr3': 'rgba(var(--base-clr3) / <alpha-value>)',
        'base-clr4': 'rgba(var(--base-clr4) / <alpha-value>)',
        'gray-1': 'rgba(var(--gray-1) / <alpha-value>)',
        'gray-2': 'rgba(var(--gray-2) / <alpha-value>)',
        'gray-3': 'rgba(var(--gray-3) / <alpha-value>)',
        'gray-4': 'rgba(var(--gray-4) / <alpha-value>)',
        danger: 'rgba(var(--danger) / <alpha-value>)',
        dark: 'rgba(var(--dark) / <alpha-value>)',
        error: 'rgba(var(--error) / <alpha-value>)',
        success: 'rgba(var(--success) / <alpha-value>)',
        warning: 'rgba(var(--warning) / <alpha-value>)',
      },
      fontSize: {
        lg: ['1.25rem', '1.75rem'],
        md: ['1.125rem', '1.575rem'],
        base: ['1rem', '1.4rem'],
        sm: ['0.875rem', '1.225rem'],
        xs: ['0.75rem', '1rem'],
        xxs: ['0.625rem', '0.75rem'],
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        Inter: ['Inter', 'sans-serif'],
        Raleway: ['Raleway', 'sans-serif'],
        Barlow: ['Barlow', 'sans-serif'],
        ethiopic: ['Noto Sans Ethiopic', 'Inter', 'sans-serif'],
      },
      boxShadow: {
        card: 'var(--shadow-card)',
        xs: 'var(--shadow-xs)',
        sm: 'var(--shadow-sm)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
      },
      keyframes: {
        'fade-in': { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
        'slide-up': {
          '0%': { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.5s ease-out forwards',
        'slide-up': 'slide-up 0.5s ease-out forwards',
        'scale-in': 'scale-in 0.3s ease-out forwards',
        shimmer: 'shimmer 2s infinite linear',
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
}
