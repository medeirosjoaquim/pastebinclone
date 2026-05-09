import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Paste } from '../models/Paste.model';
import { PastesService } from '../pastes.service';
import { CreatePasteComponent } from '../create-paste/create-paste.component';
import { Exposure } from '../models/Exposure.enum';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    CreatePasteComponent,
    MatIconModule,
    MatCardModule,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  pastesSignal = signal<Paste[]>([]);

  constructor(private pastesService: PastesService, private router: Router) {}

  ngOnInit() {
    console.log('Home component loading pastes...');
    this.pastesService.getPastes().subscribe({
      next: (data) => {
        console.log('Pastes loaded:', data);
        this.pastesSignal.set(data);
      },
      error: (error) => {
        console.error('Error loading pastes:', error);
      }
    });
  }

  navigateToPaste(paste: Paste): void {
    this.router.navigateByUrl(paste.url);
  }

  onPasteCreated(newPaste: Paste) {
    if (newPaste.exposure === Exposure.PUBLIC) {
      this.pastesSignal.update((pastes) => [...pastes, newPaste]);
    }
  }
}
