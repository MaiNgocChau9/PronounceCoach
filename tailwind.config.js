/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ['./templates/**/*.html', './static/**/*.js'],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
            },
            colors: {
                accent: {
                    50: '#fff4ec', 100: '#ffe4d1', 200: '#fdc7a4', 400: '#f97316',
                    500: '#ea580c', 600: '#d64f0a', 700: '#b54209',
                },
            },
        },
    },
    plugins: [],
};
