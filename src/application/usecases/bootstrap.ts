import { ActivityType } from "../../domain/activity-type";
import { DealStage } from "../../domain/deal-stage";
import type { Ports } from "../ports";
import { createActivity } from "./activities";
import { createCompany } from "./companies";
import { createContact } from "./contacts";
import { changeDealStage, createDeal } from "./deals";

/** Demo pipeline for an empty workspace. Business rules still go through use cases. */
export async function seedIfEmpty(ports: Ports, actorId: string): Promise<void> {
  const existing = await ports.companies.list();
  if (existing.length > 0) return;

  const harbor = await createCompany(ports, {
    name: "Harbor & Co",
    domain: "harbor.co",
    notes: "Coastal logistics, fleet of 40 vessels",
  });
  const lumen = await createCompany(ports, {
    name: "Lumen Health",
    domain: "lumen.health",
    notes: "Clinic network across the Adriatic",
  });
  const atlas = await createCompany(ports, {
    name: "Atlas Freight",
    domain: "atlasfreight.eu",
  });
  const northwind = await createCompany(ports, {
    name: "Northwind Robotics",
    domain: "northwind.robotics",
  });

  await createContact(ports, {
    companyId: harbor.id,
    name: "Mina Cole",
    email: "mina@harbor.co",
    title: "Head of Fleet",
  });
  await createContact(ports, {
    companyId: lumen.id,
    name: "Dr. Ivo Radić",
    email: "ivo@lumen.health",
    title: "COO",
  });
  await createContact(ports, {
    companyId: atlas.id,
    name: "Petra Holm",
    email: "petra@atlasfreight.eu",
    title: "Ops Director",
  });
  await createContact(ports, {
    companyId: northwind.id,
    name: "Kenji Mori",
    email: "kenji@northwind.robotics",
    title: "VP Sales",
  });

  const fleet = await createDeal(ports, {
    actorId,
    title: "Fleet telemetry",
    companyId: harbor.id,
    amount: 50000,
    currency: "USD",
    probability: 20,
  });
  const clinic = await createDeal(ports, {
    actorId,
    title: "Clinic rollout",
    companyId: lumen.id,
    amount: 120000,
    currency: "EUR",
    probability: 40,
    stage: DealStage.parse("PROPOSAL").name,
  });
  const yard = await createDeal(ports, {
    actorId,
    title: "Yard scanners",
    companyId: atlas.id,
    amount: 80000,
    currency: "USD",
    probability: 30,
    stage: "QUALIFIED",
  });
  const arm = await createDeal(ports, {
    actorId,
    title: "Arm retrofit",
    companyId: northwind.id,
    amount: 100000,
    currency: "USD",
    probability: 50,
    stage: "NEGOTIATION",
  });
  await createDeal(ports, {
    actorId,
    title: "Vision pack",
    companyId: northwind.id,
    amount: 40000,
    currency: "USD",
    probability: 25,
  });
  await createDeal(ports, {
    actorId,
    title: "EU service",
    companyId: northwind.id,
    amount: 20000,
    currency: "EUR",
    probability: 10,
    stage: "QUALIFIED",
  });
  const analog = await createDeal(ports, {
    actorId,
    title: "Analog radios",
    companyId: harbor.id,
    amount: 15000,
    currency: "USD",
    probability: 10,
  });
  const pilot = await createDeal(ports, {
    actorId,
    title: "Imaging pilot",
    companyId: lumen.id,
    amount: 25000,
    currency: "EUR",
    probability: 70,
    stage: "NEGOTIATION",
  });

  await createActivity(ports, {
    actorId,
    type: ActivityType.meeting().kind,
    body: "Walked the clinic floor with Radić. Budget is real; timeline is Q3.",
    dealId: clinic.id,
  });
  await createActivity(ports, {
    actorId,
    type: ActivityType.call().kind,
    body: "Petra confirmed scanner specs for the Rijeka yard.",
    dealId: yard.id,
  });
  await createActivity(ports, {
    actorId,
    type: ActivityType.meeting().kind,
    body: "Factory tour — Kenji wants a phased retrofit.",
    dealId: arm.id,
  });
  await createActivity(ports, {
    actorId,
    type: ActivityType.note().kind,
    body: "Mina asked for a one-pager on satellite failover.",
    dealId: fleet.id,
  });
  await createActivity(ports, {
    actorId,
    type: ActivityType.meeting().kind,
    body: "Pilot readout. Imaging accuracy beat the incumbent.",
    dealId: pilot.id,
  });

  await changeDealStage(ports, {
    actorId,
    dealId: analog.id,
    targetStage: "CLOSED_LOST",
  });
  await changeDealStage(ports, {
    actorId,
    dealId: pilot.id,
    targetStage: "CLOSED_WON",
  });
}
