import { Component } from '@angular/core';

export type AdminSection = 'overview' | 'users' | 'books' | 'feedback' | 'reports';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
  standalone: false
})
export class AdminComponent {
  activeSection: AdminSection = 'overview';

  readonly sections: { id: AdminSection; label: string; icon: string }[] = [
    { id: 'overview',  label: 'Overview',  icon: 'fa-tachometer-alt' },
    { id: 'users',     label: 'Users',     icon: 'fa-users' },
    { id: 'books',     label: 'Books',     icon: 'fa-book' },
    { id: 'feedback',  label: 'Feedback',  icon: 'fa-comments' },
    { id: 'reports',   label: 'Reports',   icon: 'fa-chart-bar' },
  ];

  setSection(section: AdminSection): void {
    this.activeSection = section;
  }
}