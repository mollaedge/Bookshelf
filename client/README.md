# 📚 Bookshelf - Client Application

A modern book management and sharing platform built with Angular 19. Bookshelf allows users to organize their personal book collections, share books with others, and discover new reads through a community-driven lending system.

## 🌟 Features

### 📖 Book Management
- **Personal Library**: Create, view, edit, and delete books in your collection
- **Rich Book Details**: Store comprehensive information including title, author, ISBN, synopsis, genre, cover images, and ratings
- **Book Organization**: Mark books as favorites, archived, or shareable
- **Cover Images**: Upload and display book cover images

### 🤝 Social Features
- **Book Sharing**: Make your books available for others to borrow
- **Borrowing System**: Request books from other users and track borrowed items
- **Request Management**: Approve or decline book requests from other members
- **Return Tracking**: Monitor books you've lent out and their return status

### 🔐 Authentication & Security
- **User Registration**: Create an account with email and password
- **Secure Login**: JWT token-based authentication
- **Google OAuth**: Sign in quickly with your Google account
- **Protected Routes**: Auth guards ensure secure access to features
- **HTTP Interceptors**: Automatic token attachment to API requests

### 👤 User Features
- **User Profiles**: Manage your account information
- **Feedback System**: Share your thoughts and suggestions about the platform
- **Dashboard**: Overview of your book activities
- **Personalized Experience**: Track your reading and sharing statistics

### 📱 User Interface
- **Responsive Design**: Works seamlessly across desktop, tablet, and mobile devices
- **Angular Material**: Modern, accessible UI components
- **Font Awesome Icons**: Clear, intuitive iconography
- **SweetAlert2 Notifications**: Beautiful, user-friendly alerts and confirmations
- **Pagination**: Efficient browsing of large book collections

## 🛠️ Technology Stack

- **Framework**: Angular 19.2
- **UI Components**: Angular Material 19.2
- **Icons**: Font Awesome 6.7
- **Notifications**: SweetAlert2 11.26
- **Server-Side Rendering**: Angular SSR
- **HTTP Client**: Angular HttpClient with RxJS 7.8
- **Routing**: Angular Router with lazy loading
- **Forms**: Reactive Forms with validation
- **Build Tool**: Angular CLI 19.2.13
- **Testing**: Jasmine & Karma

## 📋 Prerequisites

Before running this project, ensure you have the following installed:

- **Node.js**: Version 18.x or higher
- **npm**: Version 9.x or higher
- **Angular CLI**: Version 19.2.13

```bash
npm install -g @angular/cli@19.2.13
```

## 🚀 Getting Started

### 1. Install Dependencies

```bash
npm install
```

### 2. Environment Configuration

The application expects a backend API running at `http://localhost:8088/api/v1`. Update the environment configuration in:

- `src/environments/environment.ts` (development)
- `src/environments/environment.prod.ts` (production)

Configure the following settings:
```typescript
export const environment = {
  production: false,
  googleClientId: 'YOUR_GOOGLE_CLIENT_ID',
  apiUrl: 'http://localhost:8088/api/v1',
  auth: {
    redirectUri: 'http://localhost:4200/dashboard/dash'
  }
};
```

### 3. Start Development Server

```bash
npm start
# or
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload when you modify source files.

### 4. Start with Server-Side Rendering

```bash
npm run build
npm run serve:ssr:client
```

## 📁 Project Structure

```
src/
├── app/
│   ├── dashboard/          # Main application features
│   │   ├── about/         # About page component
│   │   ├── auth/          # Authentication components
│   │   │   ├── login/     # Login form
│   │   │   └── register/  # Registration form
│   │   ├── dash/          # Main dashboard
│   │   ├── feedback/      # User feedback form
│   │   ├── mybooks/       # Book management
│   │   │   ├── book/      # Individual book display
│   │   │   └── bookpopup/ # Book add/edit dialog
│   │   ├── nav/           # Navigation component
│   │   └── profile/       # User profile page
│   ├── service/           # Application services
│   │   ├── auth/          # Authentication services & guards
│   │   ├── book/          # Book management service
│   │   └── profile/       # Profile service
│   └── interfaces/        # TypeScript interfaces
├── environments/          # Environment configurations
└── styles.scss           # Global styles
```

## 🔧 Development

### Code Scaffolding

Generate new components, services, or other Angular schematics:

```bash
ng generate component component-name
ng generate service service-name
ng generate module module-name
```

### Building

Build the project for production:

```bash
ng build
```

Build artifacts will be stored in the `dist/` directory, optimized for performance.

### Running Tests

Execute unit tests via Karma:

```bash
ng test
```

### Code Style

The project follows Angular style guide conventions. Ensure your code adheres to these standards before committing.

## 🔌 API Integration

The application connects to a backend API with the following main endpoints:

- **Authentication**: `/api/v1/auth/*`
  - POST `/authenticate` - Email/password login
  - POST `/register` - User registration
  - POST `/google` - Google OAuth authentication

- **Books**: `/api/v1/books/*`
  - GET `/` - Fetch paginated books
  - POST `/` - Create a new book
  - GET `/{id}` - Get book details
  - PATCH `/{id}` - Update book
  - DELETE `/{id}` - Archive book
  - POST `/{id}/share` - Toggle book shareability
  - POST `/request/{bookId}` - Request to borrow

- **Profile**: `/api/v1/user/*`
  - GET `/profile` - Get user profile
  - PATCH `/profile` - Update profile

## 🔒 Security Features

- JWT token-based authentication
- HTTP interceptors for automatic token attachment
- Route guards for protected pages
- Secure password handling
- Google OAuth 2.0 integration
- CORS configuration for API communication

## 🎨 UI/UX Features

- Clean, modern design with Angular Material
- Loading states and error handling
- Form validation with helpful error messages
- Confirmation dialogs for destructive actions
- Toast notifications for user feedback
- Responsive layout for all screen sizes
- Book cover image preview
- Star rating system for books
- Pagination controls

## 🤔 Common Issues & Solutions

### Backend Connection Issues
Ensure the backend API is running on `http://localhost:8088` before starting the client.

### Google OAuth Not Working
Verify that your Google Client ID is correctly configured in the environment files and matches your Google Cloud Console configuration.

### Build Errors
Clear the node_modules and reinstall:
```bash
rm -rf node_modules package-lock.json
npm install
```

## 📝 Available Scripts

- `npm start` - Start development server
- `npm run build` - Build for production
- `npm run watch` - Build with watch mode
- `npm test` - Run unit tests
- `npm run serve:ssr:client` - Serve SSR build

## 🚧 Future Enhancements

- Book recommendations based on reading history
- Social features (followers, activity feed)
- Book reviews and comments
- Reading goals and progress tracking
- Book clubs and group reading
- Mobile app version
- Email notifications for book requests
- Advanced search and filtering

## 📄 License

This project is private and not licensed for public use.

## 👥 Contributing

This is a private project. For authorized contributors, please follow the standard Git workflow:
1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## 📞 Support

For questions or issues, please contact the development team or submit an issue in the project repository.

---

Built with ❤️ using Angular
