INSERT INTO permission (code, name, description)
VALUES ('CATALOG_MANAGE', '商品服务目录管理', '管理商家的商品和服务目录');

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
         CROSS JOIN permission
WHERE role.code IN ('OWNER', 'EMPLOYEE')
  AND permission.code = 'CATALOG_MANAGE';
