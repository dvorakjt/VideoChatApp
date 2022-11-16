import { Component } from '@angular/core';
import { AuthService } from './services/auth/auth.service';
import { AuthInterceptor } from './services/auth/auth-interceptor.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  providers: [AuthInterceptor]
})
export class AppComponent {
  title = 'client';
  isLoading = true;

  constructor(public authService:AuthService, public authInterceptor:AuthInterceptor) {}

}