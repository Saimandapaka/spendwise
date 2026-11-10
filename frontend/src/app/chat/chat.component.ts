import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatApiService } from '../services/chat-api.service';
import { ChatMessage } from '../models/chat.model';
import { ChatMessageComponent } from '../chat-message/chat-message.component';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatMessageComponent],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
})
export class ChatComponent {
  // Hardcoded for now — matches the single dev user seeded in step 1.
  // Swap this out once you add real auth.
  userId = 1;

  draft = '';
  messages: ChatMessage[] = [];
  isSending = false;
  errorText: string | null = null;

  constructor(private chatApi: ChatApiService) {}

  send(): void {
    const text = this.draft.trim();
    if (!text || this.isSending) return;

    this.messages.push({ role: 'user', text });
    this.draft = '';
    this.isSending = true;
    this.errorText = null;

    this.chatApi.sendMessage(this.userId, text).subscribe({
      next: (reply) => {
        this.messages.push({
          role: 'assistant',
          text: reply.replyText,
          chart: reply.render === 'chart' ? reply.chart : null,
        });
        this.isSending = false;
      },
      error: (err) => {
        console.error('Chat request failed', err);
        this.errorText = 'Something went wrong talking to the backend. Is it running on localhost:8080?';
        this.isSending = false;
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }
}
