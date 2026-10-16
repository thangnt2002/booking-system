
CREATE TABLE roles (
                       id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
                       name NVARCHAR(255) NOT NULL,
                       description NVARCHAR(255),
                       CONSTRAINT pk_roles PRIMARY KEY (id)
);

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

CREATE UNIQUE NONCLUSTERED INDEX idx_roles_name
    ON roles(name)
    WHERE name IS NOT NULL;

CREATE UNIQUE NONCLUSTERED INDEX idx_users_user_id
    ON users(user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE NONCLUSTERED INDEX idx_users_email
    ON users(email)
    WHERE email IS NOT NULL;