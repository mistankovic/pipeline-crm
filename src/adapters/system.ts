import type { Clock, IdGenerator } from "../application/ports";

export const systemClock: Clock = {
  now: () => new Date(),
};

export const uuidIds: IdGenerator = {
  next: () => crypto.randomUUID(),
};
