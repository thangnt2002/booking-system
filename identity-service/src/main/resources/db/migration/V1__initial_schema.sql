-- ============================================================================
-- Description: Initial schema setup for User, Role, Permission, and Token
-- Flyway Script: V1__initial_schema.sql
-- Database Target: SQL Server
-- ============================================================================
CREATE TABLE dbo.permissions (
                                 name VARCHAR(50) NOT NULL,
                                 description NVARCHAR(255) NULL,

                                 CONSTRAINT pk_permissions PRIMARY KEY (name)
);

CREATE TABLE dbo.roles (
                           name VARCHAR(50) NOT NULL,
                           description NVARCHAR(255) NULL,

                           CONSTRAINT pk_roles PRIMARY KEY (name)
);

CREATE TABLE dbo.role_permission (
                                     role_name VARCHAR(50) NOT NULL,
                                     permission_name VARCHAR(50) NOT NULL,

                                     CONSTRAINT pk_role_permission PRIMARY KEY (role_name, permission_name),
                                     CONSTRAINT fk_role_permission_role FOREIGN KEY (role_name)
                                         REFERENCES dbo.roles(name) ON DELETE CASCADE,
                                     CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_name)
                                         REFERENCES dbo.permissions(name) ON DELETE CASCADE
);

CREATE TABLE dbo.users (
                           id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID(),
                           username VARCHAR(50) NOT NULL,
                           password VARCHAR(255) NOT NULL,
                           first_name NVARCHAR(50) NULL,
                           last_name NVARCHAR(50) NULL,
                           dob DATE NULL,

                           CONSTRAINT pk_users PRIMARY KEY (id),
                           CONSTRAINT uq_users_username UNIQUE (username),

                           CONSTRAINT chk_users_dob_min_18 CHECK (
                               dob IS NULL OR DATEDIFF(YEAR, dob, GETDATE()) -
                                              CASE
                                                  WHEN (MONTH(dob) > MONTH(GETDATE())) OR (MONTH(dob) = MONTH(GETDATE()) AND DAY(dob) > DAY(GETDATE()))
                                                      THEN 1
                                                  ELSE 0
                                                  END >= 18
                               )
);

CREATE TABLE dbo.user_role (
                               user_id UNIQUEIDENTIFIER NOT NULL,
                               role_name VARCHAR(50) NOT NULL,

                               CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_name),
                               CONSTRAINT fk_user_role_user FOREIGN KEY (user_id)
                                   REFERENCES dbo.users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_user_role_role FOREIGN KEY (role_name)
                                   REFERENCES dbo.roles(name) ON DELETE CASCADE
);

CREATE TABLE dbo.tokens (
                            id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID(),
                            jti UNIQUEIDENTIFIER NOT NULL,
                            is_revoked BIT NOT NULL DEFAULT 0,
                            revoked_at DATETIME2 NULL,
                            expiry_time DATETIME2 NOT NULL,
                            created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
                            user_id UNIQUEIDENTIFIER NOT NULL,

                            CONSTRAINT pk_tokens PRIMARY KEY (id),
                            CONSTRAINT uq_tokens_jti UNIQUE (jti),
                            CONSTRAINT fk_tokens_user FOREIGN KEY (user_id)
                                REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE NONCLUSTERED INDEX ix_users_username ON dbo.users(username);
CREATE NONCLUSTERED INDEX ix_tokens_jti ON dbo.tokens(jti);
CREATE NONCLUSTERED INDEX ix_tokens_user_id ON dbo.tokens(user_id);