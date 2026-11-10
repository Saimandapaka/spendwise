export interface Transaction {
  id: number;
  amount: number;
  direction: 'debit' | 'credit';
  description: string;
  categoryId: number | null;
  categoryName: string | null;
  transactionDate: string; // YYYY-MM-DD
  needsReview: boolean;
}

export interface Category {
  id: number;
  name: string;
  type: 'expense' | 'income';
  isSystem: boolean;
  transactionCount: number;
  needsReviewCount: number;
}

export interface Budget {
  id: number;
  categoryId: number;
  categoryName: string;
  monthlyLimit: number;
  spentThisMonth: number;
  percentUsed: number;
  status: 'ok' | 'warning' | 'over';
}
