import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoryApiService } from '../services/category-api.service';
import { TransactionApiService } from '../services/transaction-api.service';
import { Category, Transaction } from '../models/finance.model';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css',
})
export class CategoriesComponent implements OnInit {
  userId = 1;

  categories: Category[] = [];
  needsReviewItems: Transaction[] = [];
  loading = false;
  errorText: string | null = null;
  statusText: string | null = null;

  // inline rename state
  editingCategoryId: number | null = null;
  editingName = '';

  // merge form state
  mergeFromId: number | null = null;
  mergeToId: number | null = null;

  constructor(
    private categoryApi: CategoryApiService,
    private transactionApi: TransactionApiService
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.errorText = null;
    this.categoryApi.list(this.userId).subscribe({
      next: (cats) => {
        this.categories = cats;
        this.loading = false;
      },
      error: () => {
        this.errorText = 'Could not load categories. Is the backend running?';
        this.loading = false;
      },
    });
    this.transactionApi.needsReview(this.userId).subscribe({
      next: (items) => (this.needsReviewItems = items),
      error: () => {}, // non-fatal — the needs-review section just stays empty
    });
  }

  startRename(cat: Category): void {
    this.editingCategoryId = cat.id;
    this.editingName = cat.name;
  }

  cancelRename(): void {
    this.editingCategoryId = null;
    this.editingName = '';
  }

  saveRename(cat: Category): void {
    const newName = this.editingName.trim();
    if (!newName || newName === cat.name) {
      this.cancelRename();
      return;
    }
    this.categoryApi.rename(this.userId, cat.id, newName).subscribe({
      next: () => {
        this.statusText = `Renamed to "${newName}"`;
        this.cancelRename();
        this.loadAll();
      },
      error: (err) => {
        // A 404 here specifically means the backend refused (e.g. it's a
        // shared system category), not a network failure.
        this.errorText =
          err.status === 404
            ? "Can't rename a shared category — try creating your own instead."
            : 'Rename failed. Try again.';
      },
    });
  }

  performMerge(): void {
    if (!this.mergeFromId || !this.mergeToId || this.mergeFromId === this.mergeToId) {
      this.errorText = 'Pick two different categories to merge.';
      return;
    }
    this.categoryApi.merge(this.userId, this.mergeFromId, this.mergeToId).subscribe({
      next: () => {
        this.statusText = 'Categories merged.';
        this.mergeFromId = null;
        this.mergeToId = null;
        this.loadAll();
      },
      error: () => {
        this.errorText = 'Merge failed. Try again.';
      },
    });
  }

  fixReviewItem(item: Transaction, categoryId: number): void {
    if (!categoryId) return;
    this.transactionApi.fixCategory(this.userId, item.id, categoryId).subscribe({
      next: () => {
        this.needsReviewItems = this.needsReviewItems.filter((t) => t.id !== item.id);
        this.statusText = 'Category set.';
        this.loadAll();
      },
      error: () => {
        this.errorText = 'Could not update that transaction. Try again.';
      },
    });
  }
}
