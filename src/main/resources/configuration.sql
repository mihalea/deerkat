CREATE TABLE IF NOT EXISTS categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  parentId INTEGER DEFAULT NULL,
  title VARCHAR(255) UNIQUE NOT NULL,
  hidden BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transactions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  transactionDate DATE NOT NULL,
  postingDate DATE NOT NULL,
  details VARCHAR(255) NOT NULL,
  amount DOUBLE NOT NULL,
  inflow BOOLEAN NOT NULL,
  categoryId INTEGER,
  UNIQUE (transactionDate, postingDate, details, amount) ON CONFLICT ABORT,
  FOREIGN KEY (categoryId) REFERENCES categories(id)
);

INSERT INTO categories (id, parentId, title) VALUES
  (0, NULL, 'Giving'),
  (1, NULL, 'Monthly Bills'),
  (2, NULL, 'Everyday Expenses'),
  (3, NULL, 'Rainy Day Funds'),
  (4, NULL, 'Saving Goals'),
  (5, NULL, 'Debt'),
  (NULL, 0, 'Tithing'),
  (NULL, 0, 'Charitable'),
  (NULL, 1, 'Rent/Mortgage'),
  (NULL, 1, 'Phone'),
  (NULL, 1, 'Internet'),
  (NULL, 1, 'Cable TV'),
  (NULL, 1, 'Electricity'),
  (NULL, 1, 'Water'),
  (NULL, 1, 'Natural Gas/Propane/Oil'),
  (NULL, 2, 'Groceries'),
  (NULL, 2, 'Fuel'),
  (NULL, 2, 'Spending Money'),
  (NULL, 2, 'Restaurants'),
  (NULL, 2, 'Medical'),
  (NULL, 2, 'Clothing'),
  (NULL, 2, 'Household Goods'),
  (NULL, 3, 'Emergency Fund'),
  (NULL, 3, 'Car Repairs'),
  (NULL, 3, 'Home Maintenance'),
  (NULL, 3, 'Car Insurance'),
  (NULL, 3, 'Life Insurance'),
  (NULL, 3, 'Health Insurance'),
  (NULL, 3, 'Birthdays'),
  (NULL, 3, 'Christmas'),
  (NULL, 4, 'Car Replacement'),
  (NULL, 4, 'Vacation'),
  (NULL, 5, 'Car Payment'),
  (NULL, 5, 'Student Loan Payment'),
  (NULL, 5, 'Personal Loan Payment')
  ;


