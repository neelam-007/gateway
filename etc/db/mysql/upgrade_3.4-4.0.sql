---
--- Script to update mysql ssg database from 3.4 to 4.0
---
--- Layer 7 Technologies, inc
---

ALTER TABLE published_service ADD COLUMN http_methods mediumtext;
