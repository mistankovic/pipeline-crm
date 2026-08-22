import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useCrmMutations, useWorkspace } from "@/lib/workspace";
import { STAGE_LABEL, STAGE_ORDER } from "@/lib/utils";
import type { CompanyDto, ContactDto, DealDto } from "@/lib/crm-api";

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="grid gap-1.5">
      <Label>{label}</Label>
      {children}
    </label>
  );
}

export function CompanyForm({
  company,
  trigger,
}: {
  company?: CompanyDto;
  trigger: React.ReactNode;
}) {
  const { saveCompany } = useCrmMutations();
  const [open, setOpen] = useState(false);
  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    saveCompany.mutate(
      {
        id: company?.id,
        name: String(form.get("name") ?? ""),
        domain: String(form.get("domain") ?? "") || null,
        notes: String(form.get("notes") ?? "") || null,
      },
      { onSuccess: () => setOpen(false) },
    );
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent title={company ? "Edit company" : "New company"}>
        <form className="grid gap-3" onSubmit={onSubmit}>
          <Field label="Name">
            <Input name="name" required defaultValue={company?.name} />
          </Field>
          <Field label="Domain">
            <Input name="domain" defaultValue={company?.domain ?? ""} />
          </Field>
          <Field label="Notes">
            <Textarea name="notes" defaultValue={company?.notes ?? ""} />
          </Field>
          <Button type="submit" disabled={saveCompany.isPending}>
            Save company
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function ContactForm({
  contact,
  trigger,
}: {
  contact?: ContactDto;
  trigger: React.ReactNode;
}) {
  const { data } = useWorkspace();
  const { saveContact } = useCrmMutations();
  const [open, setOpen] = useState(false);
  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    saveContact.mutate(
      {
        id: contact?.id,
        companyId: String(form.get("companyId") ?? ""),
        name: String(form.get("name") ?? ""),
        email: String(form.get("email") ?? "") || null,
        title: String(form.get("title") ?? "") || null,
      },
      { onSuccess: () => setOpen(false) },
    );
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent title={contact ? "Edit contact" : "New contact"}>
        <form className="grid gap-3" onSubmit={onSubmit}>
          <Field label="Name">
            <Input name="name" required defaultValue={contact?.name} />
          </Field>
          <Field label="Company">
            <select
              name="companyId"
              required
              defaultValue={contact?.companyId}
              className="h-11 rounded-md border border-input bg-muted px-3 text-sm"
            >
              <option value="">Select</option>
              {data?.companies.map((company) => (
                <option key={company.id} value={company.id}>
                  {company.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Title">
            <Input name="title" defaultValue={contact?.title ?? ""} />
          </Field>
          <Field label="Email">
            <Input name="email" type="email" defaultValue={contact?.email ?? ""} />
          </Field>
          <Button type="submit" disabled={saveContact.isPending}>
            Save contact
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function DealForm({
  deal,
  trigger,
}: {
  deal?: DealDto;
  trigger: React.ReactNode;
}) {
  const { data } = useWorkspace();
  const { saveDeal } = useCrmMutations();
  const [open, setOpen] = useState(false);
  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    saveDeal.mutate(
      {
        id: deal?.id,
        title: String(form.get("title") ?? ""),
        companyId: String(form.get("companyId") ?? ""),
        ownerId: String(form.get("ownerId") ?? "") || undefined,
        amount: Number(form.get("amount") ?? 0),
        currency: String(form.get("currency") ?? "USD"),
        probability: Number(form.get("probability") ?? 10),
        stage: String(form.get("stage") ?? "LEAD"),
      },
      { onSuccess: () => setOpen(false) },
    );
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent title={deal ? "Edit deal" : "New deal"}>
        <form className="grid gap-3" onSubmit={onSubmit}>
          <Field label="Title">
            <Input name="title" required defaultValue={deal?.title} />
          </Field>
          <Field label="Company">
            <select
              name="companyId"
              required
              defaultValue={deal?.companyId}
              className="h-11 rounded-md border border-input bg-muted px-3 text-sm"
            >
              <option value="">Select</option>
              {data?.companies.map((company) => (
                <option key={company.id} value={company.id}>
                  {company.name}
                </option>
              ))}
            </select>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Value">
              <Input
                name="amount"
                type="number"
                min={0}
                step={1}
                required
                defaultValue={deal?.amount ?? 0}
              />
            </Field>
            <Field label="Currency">
              <select
                name="currency"
                defaultValue={deal?.currency ?? "USD"}
                className="h-11 rounded-md border border-input bg-muted px-3 text-sm"
              >
                <option>USD</option>
                <option>EUR</option>
                <option>GBP</option>
              </select>
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Probability %">
              <Input
                name="probability"
                type="number"
                min={0}
                max={100}
                step={1}
                defaultValue={deal?.probability ?? 10}
              />
            </Field>
            {!deal && (
              <Field label="Stage">
                <select
                  name="stage"
                  defaultValue="LEAD"
                  className="h-11 rounded-md border border-input bg-muted px-3 text-sm"
                >
                  {STAGE_ORDER.filter((s) => s !== "CLOSED_WON" && s !== "CLOSED_LOST").map(
                    (stage) => (
                      <option key={stage} value={stage}>
                        {STAGE_LABEL[stage]}
                      </option>
                    ),
                  )}
                </select>
              </Field>
            )}
          </div>
          {data?.me.role === "MANAGER" && (
            <Field label="Owner">
              <select
                name="ownerId"
                defaultValue={deal?.ownerId ?? data.me.id}
                className="h-11 rounded-md border border-input bg-muted px-3 text-sm"
              >
                {data.users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.name}
                  </option>
                ))}
              </select>
            </Field>
          )}
          <Button type="submit" disabled={saveDeal.isPending}>
            Save deal
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function ActivityForm({
  dealId,
  contactId,
}: {
  dealId?: string;
  contactId?: string;
}) {
  const { saveActivity } = useCrmMutations();
  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    saveActivity.mutate(
      {
        type: String(form.get("type") ?? "NOTE"),
        body: String(form.get("body") ?? ""),
        dealId,
        contactId,
      },
      {
        onSuccess: () => {
          event.currentTarget.reset();
        },
      },
    );
  };
  return (
    <form className="grid gap-3" onSubmit={onSubmit}>
      <Field label="Type">
        <select
          name="type"
          className="h-11 rounded-md border border-input bg-muted px-3 text-sm"
        >
          <option value="NOTE">Note</option>
          <option value="CALL">Call</option>
          <option value="MEETING">Meeting</option>
        </select>
      </Field>
      <Field label="What happened">
        <Textarea name="body" required placeholder="Short, specific, dated in your own words" />
      </Field>
      <Button type="submit" disabled={saveActivity.isPending}>
        Log activity
      </Button>
    </form>
  );
}
