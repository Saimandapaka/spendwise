CREATE TABLE IF NOT EXISTS users (
  id            SERIAL PRIMARY KEY,
  name          TEXT NOT NULL,
  email         TEXT UNIQUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS categories (
  id            SERIAL PRIMARY KEY,
  user_id       INTEGER REFERENCES users(id) ON DELETE CASCADE,
  name          TEXT NOT NULL,
  type          TEXT NOT NULL CHECK (type IN ('expense', 'income')),
  is_system     BOOLEAN NOT NULL DEFAULT false,
  created_by    TEXT NOT NULL DEFAULT 'system' CHECK (created_by IN ('system', 'llm', 'user')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS transactions (
  id              SERIAL PRIMARY KEY,
  user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id     INTEGER REFERENCES categories(id) ON DELETE SET NULL,
  amount          NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
  direction       TEXT NOT NULL CHECK (direction IN ('debit', 'credit')),
  description     TEXT NOT NULL,
  raw_text        TEXT,
  transaction_date DATE NOT NULL,
  confidence      TEXT CHECK (confidence IN ('high', 'low', 'none')),
  needs_review    BOOLEAN NOT NULL DEFAULT false,
  source          TEXT NOT NULL DEFAULT 'chat' CHECK (source IN ('chat', 'quick_add', 'import')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- NEW: one monthly limit per category per user. Re-used every month
-- (not scoped to a specific month) -- "spent so far this month" is always
-- computed live against transactions, not stored.
CREATE TABLE IF NOT EXISTS budgets (
  id              SERIAL PRIMARY KEY,
  user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id     INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
  monthly_limit   NUMERIC(12, 2) NOT NULL CHECK (monthly_limit >= 0),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_user_category ON transactions (user_id, category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_needs_review ON transactions (user_id, needs_review) WHERE needs_review = true;
CREATE INDEX IF NOT EXISTS idx_budgets_user ON budgets (user_id);
