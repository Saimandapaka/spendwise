import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BudgetApiService } from '../services/budget-api.service';
import { CategoryApiService } from '../services/category-api.service';
import { Budget, Category } from '../models/finance.model';

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './budgets.component.html',
  styleUrl: './budgets.component.css',
})
export class BudgetsComponent implements OnInit {
  userId = 1;

  budgets: Budget[] = [];
  categories: Category[] = [];
  loading = false;
  errorText: string | null = null;

  // new/edit budget form state
  formCategoryId: number | null = null;
  formLimit: number | null = null;

  constructor(
    private budgetApi: BudgetApiService,
    private categoryApi: CategoryApiService
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.errorText = null;
    this.budgetApi.list(this.userId).subscribe({
      next: (budgets) => {
        this.budgets = budgets;
        this.loading = false;
      },
      error: () => {
        this.errorText = 'Could not load budgets. Is the backend running?';
        this.loading = false;
      },
    });
    this.categoryApi.list(this.userId).subscribe({
      next: (cats) => (this.categories = cats.filter((c) => c.type === 'expense')),
      error: () => {},
    });
  }

  saveBudget(): void {
    if (!this.formCategoryId || !this.formLimit || this.formLimit <= 0) {
      this.errorText = 'Pick a category and enter a limit above 0.';
      return;
    }
    this.budgetApi.setLimit(this.userId, this.formCategoryId, this.formLimit).subscribe({
      next: () => {
        this.formCategoryId = null;
        this.formLimit = null;
        this.loadAll();
      },
      error: () => {
        this.errorText = 'Could not save that budget. Try again.';
      },
    });
  }

  deleteBudget(budget: Budget): void {
    this.budgetApi.delete(this.userId, budget.id).subscribe({
      next: () => this.loadAll(),
      error: () => {
        this.errorText = 'Could not remove that budget. Try again.';
      },
    });
  }

  editExisting(budget: Budget): void {
    this.formCategoryId = budget.categoryId;
    this.formLimit = budget.monthlyLimit;
  }

  formatMoney(amount: number): string {
    return '₹' + Math.round(amount).toLocaleString('en-IN');
  }
}
