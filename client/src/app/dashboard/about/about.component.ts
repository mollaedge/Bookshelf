import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-about',
  standalone: false,
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss'
})
export class AboutComponent implements OnInit {
  features = [
    {
      icon: '📚',
      title: 'Organize Your Library',
      description: 'Keep track of all your books in one place with an intuitive and easy-to-use interface.'
    },
    {
      icon: '🔍',
      title: 'Smart Search',
      description: 'Find any book instantly with our powerful search functionality across your entire collection.'
    },
    {
      icon: '⭐',
      title: 'Rate & Review',
      description: 'Share your thoughts and rate books to remember your reading experience.'
    },
    {
      icon: '📊',
      title: 'Reading Progress',
      description: 'Track your reading goals and see your progress with detailed statistics and insights.'
    },
    {
      icon: '🔒',
      title: 'Secure & Private',
      description: 'Your data is safe with us. We prioritize security and respect your privacy.'
    },
    {
      icon: '🌐',
      title: 'Access Anywhere',
      description: 'Sync your library across all devices and access your books from anywhere, anytime.'
    }
  ];

  stats = [
    { value: '10K+', label: 'Active Users' },
    { value: '500K+', label: 'Books Catalogued' },
    { value: '50K+', label: 'Reviews Posted' },
    { value: '99.9%', label: 'Uptime' }
  ];

  ceo = {
    name: 'Artur Molla',
    title: 'CEO & Founder',
    location: 'Albania 🇦🇱',
    bio: 'Passionate about bringing readers together and making book management simple and enjoyable for everyone.'
  };

  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    // Animation trigger on scroll
    this.setupScrollAnimations();
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('');
  }

  setupScrollAnimations(): void {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.1 });

    // Observe elements after a short delay to ensure DOM is ready
    setTimeout(() => {
      const elements = document.querySelectorAll('.animate-on-scroll');
      elements.forEach(el => observer.observe(el));
    }, 100);
  }
}
