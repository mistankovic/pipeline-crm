import { Contact } from "../../domain/contact";
import type { Ports } from "../ports";
import { requireCompany, requireContact } from "../require";

export async function createContact(
  ports: Ports,
  input: {
    companyId: string;
    name: string;
    email?: string | null;
    title?: string | null;
  },
): Promise<Contact> {
  await requireCompany(ports.companies, input.companyId);
  const contact = Contact.create({
    id: ports.ids.next(),
    companyId: input.companyId,
    name: input.name,
    email: input.email,
    title: input.title,
  });
  await ports.contacts.save(contact);
  return contact;
}

export async function updateContact(
  ports: Ports,
  input: {
    id: string;
    companyId?: string;
    name?: string;
    email?: string | null;
    title?: string | null;
  },
): Promise<Contact> {
  const current = await requireContact(ports.contacts, input.id);
  if (input.companyId) await requireCompany(ports.companies, input.companyId);
  const updated = current.update(input);
  await ports.contacts.save(updated);
  return updated;
}

export async function listContacts(ports: Ports): Promise<Contact[]> {
  return ports.contacts.list();
}

export async function getContact(ports: Ports, id: string): Promise<Contact> {
  return requireContact(ports.contacts, id);
}
