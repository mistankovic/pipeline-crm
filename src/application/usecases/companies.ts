import { Company } from "../../domain/company";
import type { Ports } from "../ports";
import { requireCompany } from "../require";

export async function createCompany(
  ports: Ports,
  input: { name: string; domain?: string | null; notes?: string | null },
): Promise<Company> {
  const company = Company.create({
    id: ports.ids.next(),
    name: input.name,
    domain: input.domain,
    notes: input.notes,
  });
  await ports.companies.save(company);
  return company;
}

export async function updateCompany(
  ports: Ports,
  input: {
    id: string;
    name?: string;
    domain?: string | null;
    notes?: string | null;
  },
): Promise<Company> {
  const current = await requireCompany(ports.companies, input.id);
  const updated = current.update(input);
  await ports.companies.save(updated);
  return updated;
}

export async function listCompanies(ports: Ports): Promise<Company[]> {
  return ports.companies.list();
}

export async function getCompany(ports: Ports, id: string): Promise<Company> {
  return requireCompany(ports.companies, id);
}
