/* this file should eventually be deprecated with
Hibernate 2.2 custom sql statements
i.e. <sql-insert>....</sql-insert> inside SSG.hbm.xml
*/
/* INSERT INTO hibernate_unique_key VALUES (70); */
INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','Admin console users having administration rights to the gateway');
INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','fname','lname','email','title');
INSERT INTO internal_user_group VALUES (3,2);

