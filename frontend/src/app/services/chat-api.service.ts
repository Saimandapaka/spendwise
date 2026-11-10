import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatRequestPayload, ChatReply } from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatApiService {
  // Change this if your backend runs somewhere other than localhost:8080.
  private readonly baseUrl = '';

  constructor(private http: HttpClient) {}

  sendMessage(userId: number, message: string): Observable<ChatReply> {
    const payload: ChatRequestPayload = { userId, message };
    return this.http.post<ChatReply>(`${this.baseUrl}/api/chat`, payload);
  }
}
