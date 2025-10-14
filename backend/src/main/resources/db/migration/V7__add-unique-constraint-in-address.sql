ALTER TABLE users
    add column gender CHAR(1) check ( gender in ('M','F','O')),
    add column date_of_birth date ;
ALTER TABLE addresses
    ADD CONSTRAINT user_default_add_unique UNIQUE (user_id,is_default_shipping);