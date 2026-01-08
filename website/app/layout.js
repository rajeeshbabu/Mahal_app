import './globals.css';
import Script from 'next/script';

export const metadata = {
  title: 'Mahal Management System',
  description: 'Digital solution for modern Mahal committees',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        {children}
        <Script src="https://code.iconify.design/iconify-icon/3.0.0/iconify-icon.min.js" strategy="afterInteractive" />
      </body>
    </html>
  );
}
