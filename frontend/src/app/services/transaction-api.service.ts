import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction } from '../models/finance.model';

export interface TransactionFilters {
  categoryId?: number;
  start?: string;
  end?: string;
  search?: string;
}

export interface ImportSummary {
  totalRows: number;
  imported: number;
  needsReview: number;
  errors: string[];
}

@Injectable({ providedIn: 'root' })
export class TransactionApiService {
  private readonly baseUrl = '/api/transactions';

  constructor(private http: HttpClient) {}

  list(userId: number, filters: TransactionFilters = {}): Observable<Transaction[]> {
    let params = new HttpParams().set('userId', userId);
    if (filters.categoryId != null) params = params.set('categoryId', filters.categoryId);
    if (filters.start) params = params.set('start', filters.start);
    if (filters.end) params = params.set('end', filters.end);
    if (filters.search) params = params.set('search', filters.search);
    return this.http.get<Transaction[]>(this.baseUrl, { params });
  }

  needsReview(userId: number): Observable<Transaction[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<Transaction[]>(`${this.baseUrl}/needs-review`, { params });
  }

  fixCategory(userId: number, transactionId: number, categoryId: number): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.patch<void>(`${this.baseUrl}/${transactionId}/category`, { categoryId }, { params });
  }

  update(userId: number, transactionId: number, body: {
    amount: number; direction: 'debit' | 'credit'; description: string;
    categoryId: number | null; transactionDate: string;
  }): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.put<void>(`${this.baseUrl}/${transactionId}`, body, { params });
  }

  delete(userId: number, transactionId: number): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.baseUrl}/${transactionId}`, { params });
  }

  importFile(userId: number, file: File): Observable<ImportSummary> {
    const params = new HttpParams().set('userId', userId);
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportSummary>(`${this.baseUrl}/import`, formData, { params });
  }
}
