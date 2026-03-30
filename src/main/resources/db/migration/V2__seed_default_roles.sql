-- Seed permissions
INSERT INTO permissions (name, description) VALUES
    ('USER_READ',   'Read user information'),
    ('USER_WRITE',  'Create and update users'),
    ('USER_DELETE', 'Delete users'),
    ('ROLE_READ',   'Read role information'),
    ('ROLE_WRITE',  'Create and update roles');

-- Seed roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',  'Default user role'),
    ('ROLE_ADMIN', 'Administrator role with full access');

-- ROLE_USER gets USER_READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name = 'USER_READ';

-- ROLE_ADMIN gets ALL permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN';
