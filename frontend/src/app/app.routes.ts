import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { PasteComponent } from './paste/paste.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: ':pasteUrl', component: PasteComponent },
];
