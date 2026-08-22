export class NotFoundError extends Error {
  readonly code = "NOT_FOUND";

  constructor(readonly entity: string, readonly id: string) {
    super(`${entity} ${id} was not found`);
    this.name = "NotFoundError";
  }
}
