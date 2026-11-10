import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Budget } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class BudgetApiService {
  private readonly baseUrl = '/api/budgets';

  constructor(private http: HttpClient) {}

  list(userId: number): Observable<Budget[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<Budget[]>(this.baseUrl, { params });
  }

  setLimit(userId: number, categoryId: number, monthlyLimit: number): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<void>(this.baseUrl, { categoryId, monthlyLimit }, { params });
  }

  delete(userId: number, budgetId: number): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.baseUrl}/${budgetId}`, { params });
  }
}
