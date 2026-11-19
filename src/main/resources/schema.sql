-- Banking System database schema (SQLite)
--
-- Mapping to the class diagram:
--   customer      <->  Customer
--   account       <->  Account and its subclasses (Savings, Investment, Cheque);
--                      account_type says which subclass a row is
--   transactions  <->  Transaction
-- A customer owns its accounts and an account owns its transactions (composition),
-- so deleting a customer deletes its accounts and their transactions (ON DELETE CASCADE).

CREATE TABLE IF NOT EXISTS customer (
    customer_id      TEXT PRIMARY KEY,
    first_name       TEXT NOT NULL,
    surname          TEXT NOT NULL,
    address          TEXT NOT NULL,
    username         TEXT NOT NULL UNIQUE,
    password_hash    TEXT NOT NULL,
    employed         INTEGER NOT NULL CHECK (employed IN (0, 1)),
    employer_name    TEXT,
    employer_address TEXT,
    CHECK (employed = 0 OR (employer_name IS NOT NULL AND employer_address IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS account (
    account_number TEXT PRIMARY KEY,
    customer_id    TEXT NOT NULL REFERENCES customer (customer_id) ON DELETE CASCADE,
    account_type   TEXT NOT NULL CHECK (account_type IN ('Savings', 'Investment', 'Cheque')),
    branch         TEXT NOT NULL,
    balance        REAL NOT NULL CHECK (balance >= 0),
    open_date      TEXT NOT NULL,
    UNIQUE (customer_id, account_type)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   TEXT PRIMARY KEY,
    account_number   TEXT NOT NULL REFERENCES account (account_number) ON DELETE CASCADE,
    type             TEXT NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'SALARY', 'INTEREST')),
    amount           REAL NOT NULL CHECK (amount > 0),
    transaction_time TEXT NOT NULL,
    balance_after    REAL NOT NULL CHECK (balance_after >= 0),
    note             TEXT
);

CREATE INDEX IF NOT EXISTS idx_account_customer ON account (customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions (account_number);
