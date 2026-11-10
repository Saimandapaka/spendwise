INSERT INTO users (id, name, email) VALUES (1, 'Dev User', 'dev@example.com') ON CONFLICT (id) DO NOTHING;

INSERT INTO categories (user_id, name, type, is_system, created_by) VALUES
  (NULL, 'Food',          'expense', true, 'system'),
  (NULL, 'Groceries',     'expense', true, 'system'),
  (NULL, 'Transport',     'expense', true, 'system'),
  (NULL, 'Bills',         'expense', true, 'system'),
  (NULL, 'Entertainment', 'expense', true, 'system'),
  (NULL, 'Shopping',      'expense', true, 'system'),
  (NULL, 'Health',        'expense', true, 'system'),
  (NULL, 'Savings',       'expense', true, 'system'),
  (NULL, 'Salary',        'income',  true, 'system'),
  (NULL, 'Refund',        'income',  true, 'system'),
  (NULL, 'Other Income',  'income',  true, 'system'),
  (NULL, 'Other',         'expense', true, 'system')
ON CONFLICT (user_id, name) DO NOTHING;
