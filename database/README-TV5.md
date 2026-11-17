# Reporting and demo data
schema.sql uses the approved nine-table ERD. seed.sql and reconciliation.sql are the isolated reporting-test fixture: 24 successful payments and USD 1,892.00 for 2026-10-29 through 2026-11-04.

The integrated demo profile uses demo-seed.sql and demo-extra.sql, which preserve existing business changes on restart. Do not use the reset-oriented seed.sql against a database in active use.

No Revenue or Kitchen table is added. All reports read Payment/Orders. See the root README and docs/INTEGRATION.md.
