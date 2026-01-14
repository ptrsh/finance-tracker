CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id),
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    budget_limit NUMERIC(19, 2),
    user_id BIGINT REFERENCES users(id),
    CONSTRAINT uq_category_name_user UNIQUE (name, user_id, type)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT REFERENCES wallets(id),
    category_id BIGINT REFERENCES categories(id),
    amount NUMERIC(19, 2) NOT NULL,
    description VARCHAR(255),
    date TIMESTAMP NOT NULL
);