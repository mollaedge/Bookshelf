import { NgModule } from '@angular/core';
import { BrowserModule, provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { RouterModule } from '@angular/router';
import { authInterceptor } from './service/auth/auth.interceptor.fn';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { DashComponent } from './dashboard/dash/dash.component';
import { FeedbackComponent } from './dashboard/feedback/feedback.component';
import { AboutComponent } from './dashboard/about/about.component';
import { NavComponent } from './dashboard/nav/nav.component';
import { LoginComponent } from './dashboard/auth/login/login.component';
import { RegisterComponent } from './dashboard/auth/register/register.component';
import { ProfileComponent } from './dashboard/profile/profile.component';
import { BookComponent } from './dashboard/mybooks/book/book.component';
import { BookpopupComponent } from './dashboard/mybooks/bookpopup/bookpopup.component';
import { MybooksComponent } from './dashboard/mybooks/mybooks.component';
import { HomeComponent } from './dashboard/home/home.component';
import { CreatePostComponent } from './dashboard/home/create-post/create-post.component';
import { StartStreamComponent } from './dashboard/home/start-stream/start-stream.component';
import { ActivateAccountComponent } from './dashboard/auth/activate-account/activate-account.component';
import { LandingPageComponent } from './dashboard/landing-page/landing-page.component';
import { FriendsComponent } from './dashboard/friends/friends.component';
import { MessagesComponent } from './dashboard/messages/messages.component';
import { AdminComponent } from './dashboard/admin/admin.component';
import { AdminUsersComponent } from './dashboard/admin/admin-users/admin-users.component';
import { AdminBooksComponent } from './dashboard/admin/admin-books/admin-books.component';
import { AdminFeedbackComponent } from './dashboard/admin/admin-feedback/admin-feedback.component';
import { AdminReportsComponent } from './dashboard/admin/admin-reports/admin-reports.component';

@NgModule({
  declarations: [
    AppComponent,
    DashComponent,
    FeedbackComponent,
    AboutComponent,
    NavComponent,
    LoginComponent,
    RegisterComponent,
    ProfileComponent,
    BookComponent,
    BookpopupComponent,
    MybooksComponent,
    HomeComponent,
    CreatePostComponent,
    StartStreamComponent,
    ActivateAccountComponent,
    LandingPageComponent,
    FriendsComponent,
    MessagesComponent,
    AdminComponent,
    AdminUsersComponent,
    AdminBooksComponent,
    AdminFeedbackComponent,
    AdminReportsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    RouterModule
  ],
  providers: [
    provideClientHydration(withEventReplay()),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor])
    )
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
