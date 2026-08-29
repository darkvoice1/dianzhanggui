INSERT INTO permission (code, name, description)
VALUES ('MERCHANT_MEMBER_MANAGE', '商家成员管理', '向商家添加员工');

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
         CROSS JOIN permission
WHERE role.code = 'OWNER'
  AND permission.code = 'MERCHANT_MEMBER_MANAGE';
