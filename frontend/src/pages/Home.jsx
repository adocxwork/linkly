import { ArrowRight, ArrowUpRight, BarChart3, Globe, Link2, MessagesSquare, MousePointer2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { readStoredUser } from '../session';

const features = [
  { Icon: Globe, title: 'Share one profile', description: 'Put your work, socials and favourite links on a public page you can update anytime.' },
  { Icon: BarChart3, title: 'See what gets clicked', description: 'Track trends, devices and browsers for each short link from your dashboard.' },
  { Icon: MessagesSquare, title: 'Hear from visitors', description: 'Turn on anonymous messages and read them in a private inbox.' },
];

export default function Home() {
  const signedIn = localStorage.getItem('token') && readStoredUser();

  return (
    <div className="home-page">
      <section className="home-hero" aria-labelledby="home-title">
        <div className="hero-copy">
          <h1 id="home-title">One link for everything you share.</h1>
          <p>Publish a personal link page, create short URLs, and see which links people actually open.</p>
          <div className="hero-actions">
            <Link className="btn btn-primary" to={signedIn ? '/dashboard' : '/register'}>
              {signedIn ? 'Open dashboard' : 'Create your link page'} <ArrowRight size={18} aria-hidden="true" />
            </Link>
            <a className="btn btn-secondary" href="https://linkly-plum.vercel.app/p/adocxwork">
              See a live profile <ArrowUpRight size={17} aria-hidden="true" />
            </a>
          </div>
        </div>

        <figure className="hero-preview" aria-label="Example public link page">
          <div className="preview-address"><Link2 size={16} aria-hidden="true" /> linkly / p / maya</div>
          <div className="preview-profile">
            <div className="preview-avatar" aria-hidden="true">MC</div>
            <strong className="preview-name">Maya Chen</strong>
            <p>Software engineer · Writing about the web</p>
            <ul>
              <li>Portfolio <ArrowRight size={16} aria-hidden="true" /></li>
              <li>Engineering notes <ArrowRight size={16} aria-hidden="true" /></li>
              <li>Open-source projects <ArrowRight size={16} aria-hidden="true" /></li>
            </ul>
          </div>
          <div className="preview-insight"><MousePointer2 size={17} aria-hidden="true" /><strong>386 clicks</strong><span>Engineering notes</span></div>
        </figure>
      </section>

      <section aria-label="What you can do with Linkly" className="home-features">
        {features.map(({ Icon, title, description }) => (
          <article key={title}>
            <span className="feature-icon"><Icon size={21} aria-hidden="true" /></span>
            <h2>{title}</h2>
            <p>{description}</p>
          </article>
        ))}
      </section>
    </div>
  );
}
