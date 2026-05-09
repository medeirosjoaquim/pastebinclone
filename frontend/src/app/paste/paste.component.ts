import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Paste } from '../models/Paste.model';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PastesService } from '../pastes.service';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { catchError, throwError } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-paste',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './paste.component.html',
  styleUrl: './paste.component.css',
})
export class PasteComponent implements OnInit {
  pasteSignal = signal<Paste | null>(null);
  pasteUrlSignal = signal<string | null>('');

  constructor(
    private route: ActivatedRoute,
    private pastesService: PastesService
  ) {}

  deletePasteForm = new FormGroup({
    password: new FormControl(''),
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const pasteUrl = params.get('pasteUrl');
      pasteUrl && this.pasteUrlSignal.set(pasteUrl);
      if (pasteUrl) {
        this.pastesService
          .getPaste(pasteUrl)
          .subscribe((data) => this.pasteSignal.set(data));
      }
    });
  }

  passwordFieldType: string = 'password';

  togglePasswordVisibility(): void {
    this.passwordFieldType =
      this.passwordFieldType === 'password' ? 'text' : 'password';
  }

  onSubmit() {
    if (!this.deletePasteForm.value.password || !this.pasteUrlSignal()) {
      return;
    }

    console.log(this.pasteUrlSignal());
    this.pastesService
      .deletePaste(this.pasteUrlSignal()!, this.deletePasteForm.value.password)
      .pipe(
        catchError((error) => {
          // Handle the error here
          console.error('Error:', error);
          alert(`Error: ${error.error.message || error.message}`);
          return throwError(() => error);
        })
      )
      .subscribe((data) => {
        alert('Paste deleted!');
        this.pasteSignal.set(null);
        window.location.reload();
      });
  }
}
