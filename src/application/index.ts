export { NotFoundError } from "./errors";
export type {
  ActivityRepository,
  Clock,
  CompanyRepository,
  ContactRepository,
  DealRepository,
  IdGenerator,
  Ports,
  UserRepository,
} from "./ports";
export {
  assignRole,
  actorFor,
  ensureProfile,
  listUsers,
} from "./usecases/profiles";
export {
  createCompany,
  getCompany,
  listCompanies,
  updateCompany,
} from "./usecases/companies";
export {
  createContact,
  getContact,
  listContacts,
  updateContact,
} from "./usecases/contacts";
export {
  changeDealStage,
  createDeal,
  getDeal,
  listDeals,
  reassignDeal,
  updateDeal,
} from "./usecases/deals";
export {
  createActivity,
  listContactActivities,
  listDealActivities,
} from "./usecases/activities";
export { getForecast } from "./usecases/forecast";
export { seedIfEmpty } from "./usecases/bootstrap";
export { loadWorkspace } from "./usecases/workspace";
