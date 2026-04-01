-- DBUPDATE-032-0.SQL

-- Enable guest login by default so the login page shows the guest access option
update T_CONFIG set CFG_VALUE_C = 'true' where CFG_ID_C = 'GUEST_LOGIN';

-- Update the database version
update T_CONFIG set CFG_VALUE_C = '32' where CFG_ID_C = 'DB_VERSION';
