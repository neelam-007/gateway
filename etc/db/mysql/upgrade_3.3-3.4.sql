---
--- Script to update mysql ssg database from 3.3 to 3.4
---
--- Layer 7 Technologies, inc
---

ALTER TABLE client_cert ADD COLUMN ski VARCHAR(64);
ALTER TABLE client_cert ADD INDEX i_ski (ski);

ALTER TABLE trusted_cert ADD COLUMN ski VARCHAR(64);
ALTER TABLE trusted_cert ADD INDEX i_ski (ski);
