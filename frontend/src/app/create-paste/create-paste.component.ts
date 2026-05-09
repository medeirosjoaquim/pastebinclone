import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PastesService } from '../pastes.service';
import { CreatePaste, Paste } from '../models/Paste.model';
import { Exposure } from '../models/Exposure.enum';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
@Component({
  selector: 'app-create-paste',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './create-paste.component.html',
  styleUrl: './create-paste.component.css',
})
export class CreatePasteComponent {
  constructor(private pastesService: PastesService) {}

  @Output() pasteCreated = new EventEmitter<Paste>();

  pasteForm = new FormGroup({
    title: new FormControl('', [Validators.required]),
    content: new FormControl('', [Validators.required]),
    exposure: new FormControl<Exposure>(Exposure.PUBLIC, [Validators.required]),
    expirationDate: new FormControl(''),
    password: new FormControl('', [Validators.required, Validators.minLength(4)]),
  });

  passwordFieldType: string = 'password';

  togglePasswordVisibility(): void {
    this.passwordFieldType =
      this.passwordFieldType === 'password' ? 'text' : 'password';
  }

  getExposureHint(): string {
    const exposure = this.pasteForm.value.exposure;
    switch (exposure) {
      case Exposure.PUBLIC:
        return 'Visible in public pastes list';
      case Exposure.PRIVATE:
        return 'Hidden from public list, accessible via URL';
      case Exposure.UNLISTED:
        return 'Hidden from public list, accessible via URL';
      default:
        return '';
    }
  }

  onSubmit() {
    if (this.pasteForm.invalid) {
      alert('Please fill in all required fields:\n- Title\n- Content\n- Password (minimum 4 characters)');
      return;
    }
    const expiration = this.pasteForm.value.expirationDate
      ? new Date(this.pasteForm.value.expirationDate).toISOString()
      : null;
    const formValue: CreatePaste = {
      title: this.pasteForm.value.title!,
      content: this.pasteForm.value.content!,
      exposure: this.pasteForm.value.exposure!,
      expirationDate: expiration,
      password: this.pasteForm.value.password!,
    };

    this.pastesService.createPaste(formValue).subscribe({
      next: (newPaste) => {
        this.pasteCreated.emit(newPaste);
        const fullUrl = `${window.location.origin}/${newPaste.url}`;
        alert(`Paste created successfully!\n\nURL: ${fullUrl}\n\nYou'll need your password to delete this paste.`);
        window.location.reload();
      },
      error: (error) => {
        console.error('Error creating paste:', error);
        const message = error.error?.message || error.message || 'Failed to create paste';
        alert(`Error: ${message}`);
      },
    });
  }
}
