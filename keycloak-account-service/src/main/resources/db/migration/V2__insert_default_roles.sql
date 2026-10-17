IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')
    BEGIN
        INSERT INTO roles (id, name, description)
        VALUES (NEWID(), 'ADMIN', 'admin');
    END;

IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER')
    BEGIN
        INSERT INTO roles (id, name, description)
        VALUES (NEWID(), 'USER', 'user');
    END;