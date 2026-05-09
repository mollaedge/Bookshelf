import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NavComponent } from './dashboard/nav/nav.component';
import { DashComponent } from './dashboard/dash/dash.component';
import { AboutComponent } from './dashboard/about/about.component';
import { FeedbackComponent } from './dashboard/feedback/feedback.component';
import { LoginComponent } from './dashboard/auth/login/login.component';
import { RegisterComponent } from './dashboard/auth/register/register.component';
import { ActivateAccountComponent } from './dashboard/auth/activate-account/activate-account.component';
import { MybooksComponent } from './dashboard/mybooks/mybooks.component';
import { AuthGuard } from './service/auth/auth-guard';
import { NoAuthGuard } from './service/auth/no-auth-guard';
import { ProfileComponent } from './dashboard/profile/profile.component';
import { HomeComponent } from './dashboard/home/home.component';
import { LandingPageComponent } from './dashboard/landing-page/landing-page.component';
import { FriendsComponent } from './dashboard/friends/friends.component';
import { MessagesComponent } from './dashboard/messages/messages.component';

const routes: Routes = [
  {
    path: '',
    component: NavComponent,
    children: [
      { path: '', component: LandingPageComponent, canActivate: [NoAuthGuard] },
      { path: 'home', component: HomeComponent, canActivate: [AuthGuard] },
      { path: 'profile', component: ProfileComponent , canActivate: [AuthGuard]},
      { path: 'mybooks', component: MybooksComponent, canActivate: [AuthGuard]},
      { path: 'friends', component: FriendsComponent, canActivate: [AuthGuard] },
      { path: 'messages', component: MessagesComponent, canActivate: [AuthGuard] },
      { path: 'dashboard', component: DashComponent },
      { path: 'about', component: AboutComponent },
      { path: 'feedback', component: FeedbackComponent },
      { path: 'pdf-reader', loadChildren: () => import('./dashboard/pdf-reader/pdf-reader.module').then(m => m.PdfReaderModule) },
      { 
        path: 'auth', 
        children: [
          { path: '', redirectTo: 'login', pathMatch: 'full' },
          { path: 'login', component: LoginComponent },
          { path: 'register', component: RegisterComponent },
          { path: 'activate-account', component: ActivateAccountComponent }
        ] 
      },
    ]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    anchorScrolling: 'enabled',
    scrollPositionRestoration: 'enabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
