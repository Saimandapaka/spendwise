import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class CategoryApiService {
  private readonly baseUrl = '/api/categories';

  constructor(private http: HttpClient) {}

  list(userId: number): Observable<Category[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<Category[]>(this.baseUrl, { params });
  }

  rename(userId: number, categoryId: number, name: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.put<void>(`${this.baseUrl}/${categoryId}`, { name }, { params });
  }

  merge(userId: number, fromCategoryId: number, toCategoryId: number): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<void>(`${this.baseUrl}/merge`, { fromCategoryId, toCategoryId }, { params });
  }
}
