CREATE TABLE payment_method (
        payment_method_id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE countries (
       country_id SERIAL PRIMARY KEY,
       name VARCHAR(255) NOT NULL UNIQUE,
       acronym VARCHAR(10) UNIQUE,
       currency VARCHAR(50),
       rating INTEGER CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE country_payment_method (
        country_id INTEGER NOT NULL,
        payment_method_id INTEGER NOT NULL,
        PRIMARY KEY (country_id, payment_method_id),
        FOREIGN KEY (country_id) REFERENCES countries (country_id) ON DELETE CASCADE,
        FOREIGN KEY (payment_method_id) REFERENCES payment_method (payment_method_id) ON DELETE CASCADE
);

CREATE TABLE mobile_money_option (
         mobile_money_option_id SERIAL PRIMARY KEY,
         name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE country_mobile_money_options (
          country_id INTEGER NOT NULL,
          mobile_money_option_id INTEGER NOT NULL,
          PRIMARY KEY (country_id, mobile_money_option_id),
          FOREIGN KEY (country_id) REFERENCES countries (country_id) ON DELETE CASCADE,
          FOREIGN KEY (mobile_money_option_id) REFERENCES mobile_money_option (mobile_money_option_id) ON DELETE CASCADE
);
