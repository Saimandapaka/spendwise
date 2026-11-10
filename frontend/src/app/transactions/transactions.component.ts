import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionApiService, ImportSummary } from '../services/transaction-api.service';
import { CategoryApiService } from '../services/category-api.service';
import { Transaction, Category } from '../models/finance.model';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css',
})
export class TransactionsComponent implements OnInit {
  userId = 1;

  transactions: Transaction[] = [];
  categories: Category[] = [];
  loading = false;
  errorText: string | null = null;
  statusText: string | null = null;

  // filter state
  selectedCategoryId: number | null = null;
  startDate = '';
  endDate = '';
  search = '';

  // inline edit state
  editingId: number | null = null;
  editAmount: number | null = null;
  editDirection: 'debit' | 'credit' = 'debit';
  editDescription = '';
  editCategoryId: number | null = null;
  editDate = '';

  // import state
  importing = false;
  importSummary: ImportSummary | null = null;
  showImportPanel = false;

  constructor(
    private transactionApi: TransactionApiService,
    private categoryApi: CategoryApiService
  ) {}

  ngOnInit(): void {
    this.categoryApi.list(this.userId).subscribe({
      next: (cats) => (this.categories = cats),
      error: () => {}, // filter dropdown just stays empty; not fatal
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorText = null;
    this.transactionApi
      .list(this.userId, {
        categoryId: this.selectedCategoryId ?? undefined,
        start: this.startDate || undefined,
        end: this.endDate || undefined,
        search: this.search || undefined,
      })
      .subscribe({
        next: (txns) => {
          this.transactions = txns;
          this.loading = false;
        },
        error: () => {
          this.errorText = 'Could not load transactions. Is the backend running?';
          this.loading = false;
        },
      });
  }

  clearFilters(): void {
    this.selectedCategoryId = null;
    this.startDate = '';
    this.endDate = '';
    this.search = '';
    this.load();
  }

  startEdit(t: Transaction): void {
    this.editingId = t.id;
    this.editAmount = t.amount;
    this.editDirection = t.direction;
    this.editDescription = t.description;
    this.editCategoryId = t.categoryId;
    this.editDate = t.transactionDate;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(t: Transaction): void {
    if (!this.editAmount || this.editAmount <= 0 || !this.editDate) {
      this.errorText = 'Amount and date are required.';
      return;
    }
    this.transactionApi
      .update(this.userId, t.id, {
        amount: this.editAmount,
        direction: this.editDirection,
        description: this.editDescription,
        categoryId: this.editCategoryId,
        transactionDate: this.editDate,
      })
      .subscribe({
        next: () => {
          this.statusText = 'Transaction updated.';
          this.editingId = null;
          this.load();
        },
        error: () => {
          this.errorText = 'Could not save changes. Try again.';
        },
      });
  }

  deleteTransaction(t: Transaction): void {
    const confirmed = window.confirm(
      `Delete this ${this.formatMoney(t.amount)} transaction (${t.description || 'no description'})? This can't be undone.`
    );
    if (!confirmed) return;

    this.transactionApi.delete(this.userId, t.id).subscribe({
      next: () => {
        this.statusText = 'Transaction deleted.';
        this.transactions = this.transactions.filter((x) => x.id !== t.id);
      },
      error: () => {
        this.errorText = 'Could not delete that transaction. Try again.';
      },
    });
  }

  formatMoney(amount: number): string {
    return '₹' + Math.round(amount).toLocaleString('en-IN');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.importing = true;
    this.importSummary = null;
    this.errorText = null;

    this.transactionApi.importFile(this.userId, file).subscribe({
      next: (summary) => {
        this.importSummary = summary;
        this.importing = false;
        this.load(); // refresh the list to show newly imported rows
      },
      error: (err) => {
        this.errorText =
          err.error?.message || err.error || 'Import failed. Check the file format and try again.';
        this.importing = false;
      },
    });

    // allow re-selecting the same file later without needing to change it first
    input.value = '';
  }
}
