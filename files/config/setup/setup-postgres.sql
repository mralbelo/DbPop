-- DROP
DROP FUNCTION IF EXISTS advanced.GetInvoices();
DROP TABLE IF EXISTS advanced.invoice_details;
DROP TABLE IF EXISTS advanced.invoices;
DROP TABLE IF EXISTS advanced.order_details;
DROP TABLE IF EXISTS advanced.orders;
DROP TABLE IF EXISTS advanced.deliveries;
DROP TABLE IF EXISTS advanced.products;
DROP TABLE IF EXISTS advanced.customers;
DROP TABLE IF EXISTS advanced.test_binary;
DROP TABLE IF EXISTS advanced.customer_types;
DROP TABLE IF EXISTS advanced.order_types;
DROP TABLE IF EXISTS advanced.product_categories;
DROP TABLE IF EXISTS dbo.invoice_details;
DROP TABLE IF EXISTS dbo.invoices;
DROP TABLE IF EXISTS dbo.products;
DROP TABLE IF EXISTS dbo.customers;
DROP TABLE IF EXISTS dbo.test_binary;
DROP SCHEMA IF EXISTS advanced CASCADE;
DROP SCHEMA IF EXISTS dbo CASCADE;

-- CREATE SCHEMAS
CREATE SCHEMA IF NOT EXISTS advanced;
CREATE SCHEMA IF NOT EXISTS dbo;

-- CREATE TABLES in advanced schema
CREATE TABLE advanced.customer_types (
    customer_type_id SERIAL PRIMARY KEY,
    name VARCHAR(32)
);

CREATE TABLE advanced.order_types (
    order_type_id SERIAL PRIMARY KEY,
    name VARCHAR(32)
);

CREATE TABLE advanced.product_categories (
    product_category_id SERIAL PRIMARY KEY,
    name VARCHAR(32)
);

CREATE TABLE advanced.customers (
    customer_id SERIAL PRIMARY KEY,
    customer_type_id INT NOT NULL,
    name VARCHAR(32),
    CONSTRAINT customers_customer_types_fk FOREIGN KEY (customer_type_id) REFERENCES advanced.customer_types
);

CREATE TABLE advanced.products (
    product_id SERIAL PRIMARY KEY,
    product_category_id INT NOT NULL,
    part_no VARCHAR(32) NOT NULL,
    part_desc VARCHAR(255) NOT NULL,
    CONSTRAINT products_product_categories_fk FOREIGN KEY (product_category_id) REFERENCES advanced.product_categories
);

CREATE TABLE advanced.orders (
    order_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    order_type_id INT NOT NULL,
    order_date TIMESTAMP NOT NULL,
    CONSTRAINT orders_customers_fk FOREIGN KEY (customer_id) REFERENCES advanced.customers,
    CONSTRAINT orders_order_types_fk FOREIGN KEY (order_type_id) REFERENCES advanced.order_types
);

CREATE TABLE advanced.order_details (
    order_detail_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT order_details_orders_fk FOREIGN KEY (order_id) REFERENCES advanced.orders,
    CONSTRAINT order_details_products_fk FOREIGN KEY (product_id) REFERENCES advanced.products
);

CREATE TABLE advanced.deliveries (
    delivery_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    delivery_date TIMESTAMP NOT NULL,
    CONSTRAINT deliveries_orders_fk FOREIGN KEY (order_id) REFERENCES advanced.orders
);

CREATE TABLE advanced.invoices (
    invoice_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    CONSTRAINT invoices_customers_fk FOREIGN KEY (customer_id) REFERENCES advanced.customers
);

CREATE TABLE advanced.invoice_details (
    invoice_detail_id SERIAL PRIMARY KEY,
    invoice_id INT NOT NULL,
    product_id INT NOT NULL,
    CONSTRAINT invoice_details_invoices_fk FOREIGN KEY (invoice_id) REFERENCES advanced.invoices,
    CONSTRAINT invoice_details_products_fk FOREIGN KEY (product_id) REFERENCES advanced.products
);

CREATE TABLE advanced.test_binary (
    test_binary_id SERIAL PRIMARY KEY,
    binary_data BYTEA
);

-- CREATE TABLES in dbo schema
CREATE TABLE dbo.customers (
    customer_id SERIAL PRIMARY KEY,
    name VARCHAR(32)
);

CREATE TABLE dbo.products (
    product_id SERIAL PRIMARY KEY,
    part_no VARCHAR(32) NOT NULL,
    part_desc VARCHAR(255) NOT NULL
);

CREATE TABLE dbo.invoices (
    invoice_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    CONSTRAINT invoices_customers_fk FOREIGN KEY (customer_id) REFERENCES dbo.customers
);

CREATE TABLE dbo.invoice_details (
    invoice_detail_id SERIAL PRIMARY KEY,
    invoice_id INT NOT NULL,
    product_id INT NOT NULL,
    CONSTRAINT invoice_details_invoices_fk FOREIGN KEY (invoice_id) REFERENCES dbo.invoices,
    CONSTRAINT invoice_details_products_fk FOREIGN KEY (product_id) REFERENCES dbo.products
);

CREATE TABLE dbo.test_binary (
    test_binary_id SERIAL PRIMARY KEY,
    binary_data BYTEA
);

-- CREATE FUNCTION (equivalent to stored procedure in PostgreSQL)
CREATE OR REPLACE FUNCTION advanced.GetInvoices()
RETURNS TABLE (
    invoice_id INT,
    customer_id INT,
    invoice_date TIMESTAMP,
    customer_name VARCHAR(32)
) AS $$
BEGIN
    RETURN QUERY
    SELECT i.invoice_id, i.customer_id, i.invoice_date, c.name as customer_name
    FROM advanced.invoices i
    JOIN advanced.customers c ON i.customer_id = c.customer_id;
END;
$$ LANGUAGE plpgsql;
