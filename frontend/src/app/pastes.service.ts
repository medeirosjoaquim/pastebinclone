import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { CreatePaste, Paste } from './models/Paste.model';

@Injectable({
  providedIn: 'root',
})
export class PastesService {
  private apiEndpoint = 'http://localhost:8080/pastes';

  constructor(private http: HttpClient) {}
  getPastes(): Observable<Paste[]> {
    return this.http.get<Paste[]>(this.apiEndpoint).pipe(
      map((pastes) =>
        pastes.map((paste) => ({
          ...paste,
          expirationDate: paste.expirationDate
            ? new Date(paste.expirationDate).toDateString() +
              ' - ' +
              new Date(paste.expirationDate).toLocaleTimeString()
            : null,
        }))
      )
    );
  }
  getPaste(url: string): Observable<Paste> {
    return this.http.get<Paste>(this.apiEndpoint + `/${url}`).pipe(
      map((paste) => ({
        ...paste,
        expirationDate: paste.expirationDate
          ? new Date(paste.expirationDate).toDateString() +
            ' - ' +
            new Date(paste.expirationDate).toLocaleTimeString()
          : null,
        createdAt:
          new Date(paste.createdAt).toDateString() +
          ' - ' +
          new Date(paste.createdAt).toLocaleTimeString(),
      }))
    );
  }
  createPaste(paste: CreatePaste): Observable<Paste> {
    return this.http.post<Paste>(this.apiEndpoint, paste);
  }

  deletePaste(url: string, password: string): Observable<Paste> {
    const options = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      body: { url, password },
    };
    return this.http.delete<Paste>(this.apiEndpoint, options);
  }
}
