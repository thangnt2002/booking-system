
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'roles')
BEGIN
CREATE TABLE roles (
                       id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
                       name NVARCHAR(255) NOT NULL,
                       description NVARCHAR(255),
                       CONSTRAINT pk_roles PRIMARY KEY (id)
);
END;

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'users')
BEGIN
CREATE TABLE users (
                       id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
                       user_id NVARCHAR(255),
                       username NVARCHAR(255),
                       avatar NVARCHAR(255),
                       email NVARCHAR(255),
                       first_name NVARCHAR(255),
                       last_name NVARCHAR(255),
                       dob DATE,
                       city NVARCHAR(255),
                       CONSTRAINT pk_users PRIMARY KEY (id)
);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_roles_name' AND object_id = OBJECT_ID('roles'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_roles_name
        ON roles(name)
        WHERE name IS NOT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_users_user_id' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_users_user_id
        ON users(user_id)
        WHERE user_id IS NOT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_users_email' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_users_email
        ON users(email)
        WHERE email IS NOT NULL;
END;