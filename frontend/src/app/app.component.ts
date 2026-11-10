import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatComponent } from './chat/chat.component';
import { TransactionsComponent } from './transactions/transactions.component';
import { CategoriesComponent } from './categories/categories.component';
import { BudgetsComponent } from './budgets/budgets.component';

type Tab = 'chat' | 'transactions' | 'categories' | 'budgets';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatComponent, TransactionsComponent, CategoriesComponent, BudgetsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  activeTab: Tab = 'chat';

  setTab(tab: Tab): void {
    this.activeTab = tab;
  }
}
