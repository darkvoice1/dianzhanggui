INSERT INTO permission (code, name, description)
VALUES ('PROFILE_MANAGE', '档案管理', '管理商家客户和人员档案');

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
         CROSS JOIN permission
WHERE role.code IN ('OWNER', 'EMPLOYEE')
  AND permission.code = 'PROFILE_MANAGE';
