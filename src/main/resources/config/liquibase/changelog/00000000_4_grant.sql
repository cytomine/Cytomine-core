create user root with password 'root';
create user sudo with password 'sudo';
GRANT postgres TO root;
GRANT postgres TO sudo;