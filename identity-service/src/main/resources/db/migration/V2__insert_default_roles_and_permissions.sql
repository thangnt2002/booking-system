-- ============================================================================
-- Description: Insert default roles (USER, ADMIN) and permission (USER_CREATE)
-- Flyway Script: V2__insert_default_roles_and_permissions.sql
-- Database Target: SQL Server
-- ============================================================================
IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE name = 'USER_CREATE')
BEGIN
INSERT INTO dbo.permissions (name, description)
VALUES ('USER_CREATE', 'crate new user');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'ADMIN')
BEGIN
INSERT INTO dbo.roles (name, description)
VALUES ('ADMIN', 'manager');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'USER')
BEGIN
INSERT INTO dbo.roles (name, description)
VALUES ('USER', 'normal user');
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.role_permission
    WHERE role_name = 'ADMIN' AND permission_name = 'USER_CREATE'
)
BEGIN
INSERT INTO dbo.role_permission (role_name, permission_name)
VALUES ('ADMIN', 'USER_CREATE');
END;