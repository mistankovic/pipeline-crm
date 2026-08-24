# Stage 4 — Builder hand-off

## Delivered

- Flyway `V1__init.sql` (users, companies, contacts, deals, activities) on PostgreSQL
- JPA entities and Spring Data repositories in `adapter-persistence` only
- Thin output-port adapters (`Jpa*Repository`) mapping to/from domain aggregates
- `Deal.restore` loads owned activities so closed-won guards stay in the domain
- Testcontainers persistence IT exercises ports, not Spring Data interfaces
- `hibernate.ddl-auto=none`; schema is Flyway only

Domain and application modules still have zero JPA/Spring compile dependencies.
