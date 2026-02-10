/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        border: "hsl(var(--border))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
          50: "#fef2f3",
          100: "#fde6e7",
          200: "#fbd0d4",
          300: "#f7aab1",
          400: "#f17a87",
          500: "#e23744",
          600: "#d02a3a",
          700: "#ae1f2d",
          800: "#911d29",
          900: "#7c1d28",
          950: "#440a11",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        dark: {
          50: "#f6f6f7",
          100: "#e3e3e5",
          200: "#c5c5ca",
          300: "#a1a1a8",
          400: "#7d7d86",
          500: "#63636c",
          600: "#4e4e56",
          700: "#404047",
          800: "#2b2b30",
          900: "#1a1a1f",
          950: "#0f0f12",
        },
      },
      borderColor: {
        DEFAULT: "hsl(var(--border))",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
