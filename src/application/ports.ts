import type { Activity } from "../domain/activity";
import type { Company } from "../domain/company";
import type { Contact } from "../domain/contact";
import type { Deal } from "../domain/deal";
import type { CrmUser } from "../domain/user";

export interface UserRepository {
  findById(id: string): Promise<CrmUser | null>;
  save(user: CrmUser): Promise<void>;
  list(): Promise<CrmUser[]>;
  count(): Promise<number>;
}

export interface CompanyRepository {
  findById(id: string): Promise<Company | null>;
  save(company: Company): Promise<void>;
  list(): Promise<Company[]>;
}

export interface ContactRepository {
  findById(id: string): Promise<Contact | null>;
  save(contact: Contact): Promise<void>;
  list(): Promise<Contact[]>;
}

export interface DealRepository {
  findById(id: string): Promise<Deal | null>;
  save(deal: Deal): Promise<void>;
  list(): Promise<Deal[]>;
}

export interface ActivityRepository {
  findById(id: string): Promise<Activity | null>;
  save(activity: Activity): Promise<void>;
  list(): Promise<Activity[]>;
  listForDeal(dealId: string): Promise<Activity[]>;
  listForContact(contactId: string): Promise<Activity[]>;
}

export interface Clock {
  now(): Date;
}

export interface IdGenerator {
  next(): string;
}

export type Ports = {
  users: UserRepository;
  companies: CompanyRepository;
  contacts: ContactRepository;
  deals: DealRepository;
  activities: ActivityRepository;
  clock: Clock;
  ids: IdGenerator;
};
