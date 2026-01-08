"use client";

export default function ContentSection() {
    const benefits = [
        {
            title: 'Cloud-Based Access',
            desc: 'Access your data securely from any device, anywhere, anytime.'
        },
        // {
        //     title: 'Role-Based Permissions',
        //     desc: 'Assign specific roles (President, Secretary, Clerk) with limited access.'
        // },
        {
            title: 'Data Backup & Security',
            desc: 'Automatic daily backups and bank-grade encryption for your data.'
        }
    ];

    return (
        <section id="solutions" className="section-spacing content-section">
            <div className="container">
                <div className="split-grid">
                    <div className="image-wrapper">
                        <img
                            src="https://images.unsplash.com/photo-1750768145390-f0ad18d3e65b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w2MjYyMDB8MHwxfHNlYXJjaHwxfHxhZG1pbiUyMHBhbmVsJTIwdXNlciUyMG1hbmFnZW1lbnQlMjB1aXxlbnwwfHx8fDE3NjcxNTU4NzN8MA&ixlib=rb-4.1.0&q=80&w=1080"
                            alt="Member Management"
                        />
                    </div>
                    <div>
                        <span className="section-tag">EASY ADMINISTRATION</span>
                        <h2 className="section-title">
                            Simplify complex administrative tasks
                        </h2>
                        <p className="section-subtitle">
                            Replace physical ledgers with a secure cloud-based system accessible from anywhere. Reduce paperwork and increase transparency.
                        </p>

                        <div className="content-list">
                            {benefits.map((benefit, index) => (
                                <div key={index} className="content-item">
                                    <div className="check-icon">
                                        <iconify-icon icon="lucide:check-circle" style={{ fontSize: '20px' }}></iconify-icon>
                                    </div>
                                    <div className="content-text">
                                        <strong>{benefit.title}</strong>
                                        <span>{benefit.desc}</span>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div style={{ marginTop: '32px' }}>
                            <button className="btn btn-outline">
                                Learn more about security
                                <iconify-icon icon="lucide:arrow-right" style={{ fontSize: '16px' }}></iconify-icon>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            <style jsx>{`
        .content-section { background-color: var(--muted); }
        .split-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 80px;
          align-items: center;
        }
        .image-wrapper img {
          width: 100%;
          border-radius: var(--radius-lg);
          border: 1px solid var(--border);
          box-shadow: 0 10px 20px rgba(0,0,0,0.05);
        }
        .section-tag { color: var(--primary); font-weight: 600; font-size: 14px; margin-bottom: 12px; display: block; }
        .section-title { font-size: 32px; font-weight: 700; margin-bottom: 16px; letter-spacing: -0.01em; }
        .section-subtitle { font-size: 16px; color: var(--muted-foreground); line-height: 1.6; }
        .content-list { display: flex; flex-direction: column; gap: 20px; margin-top: 32px; }
        .content-item { display: flex; gap: 12px; align-items: flex-start; }
        .check-icon { color: var(--primary); margin-top: 2px; }
        .content-text strong { display: block; margin-bottom: 4px; color: var(--foreground); }
        .content-text span { color: var(--muted-foreground); font-size: 14px; line-height: 1.5; }
        @media (max-width: 968px) {
          .split-grid { grid-template-columns: 1fr; gap: 48px; }
          .image-wrapper { order: 2; }
        }
      `}</style>
        </section>
    );
}
