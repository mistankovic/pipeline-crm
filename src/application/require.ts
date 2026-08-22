import { NotFoundError } from "./errors";
import type {
  ActivityRepository,
  CompanyRepository,
  ContactRepository,
  DealRepository,
  UserRepository,
} from "./ports";

export async function requireUser(users: UserRepository, id: string) {
  const user = await users.findById(id);
  if (!user) throw new NotFoundError("User", id);
  return user;
}

export async function requireCompany(companies: CompanyRepository, id: string) {
  const company = await companies.findById(id);
  if (!company) throw new NotFoundError("Company", id);
  return company;
}

export async function requireContact(contacts: ContactRepository, id: string) {
  const contact = await contacts.findById(id);
  if (!contact) throw new NotFoundError("Contact", id);
  return contact;
}

export async function requireDeal(deals: DealRepository, id: string) {
  const deal = await deals.findById(id);
  if (!deal) throw new NotFoundError("Deal", id);
  return deal;
}

export async function requireActivity(activities: ActivityRepository, id: string) {
  const activity = await activities.findById(id);
  if (!activity) throw new NotFoundError("Activity", id);
  return activity;
}
