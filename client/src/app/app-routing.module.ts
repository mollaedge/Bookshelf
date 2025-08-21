import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NavComponent } from './dashboard/nav/nav.component';
import { DashComponent } from './dashboard/dash/dash.component';
import { AboutComponent } from './dashboard/about/about.component';
import { FeedbackComponent } from './dashboard/feedback/feedback.component';
import { LoginComponent } from './dashboard/auth/login/login.component';
import { RegisterComponent } from './dashboard/auth/register/register.component';
import { AuthGuard } from './service/auth/auth-guard';

const routes: Routes = [
  {
    path: '',
    component: NavComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashComponent },
      { path: 'about', component: AboutComponent , canActivate: [AuthGuard]},
      { path: 'feedback', component: FeedbackComponent , canActivate: [AuthGuard]},
      { 
        path: 'auth', 
        children: [
          { path: '', redirectTo: 'login', pathMatch: 'full' },
          { path: 'login', component: LoginComponent },
          { path: 'register', component: RegisterComponent }
        ] 
      },
    ]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
