import Navbar from '../components/Navbar';
import Hero from '../components/Hero';
import Stats from '../components/Stats';
import Features from '../components/Features';
import Pricing from '../components/Pricing';
import ContentSection from '../components/ContentSection';
import CTA from '../components/CTA';
import Footer from '../components/Footer';

export default function Home() {
  return (
    <main>
      <Navbar />
      <Hero />
      <Stats />
      <Features />
      <Pricing />
      <ContentSection />
      <CTA />
      <Footer />
    </main>
  );
}
