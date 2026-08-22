import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { ContactForm } from "@/components/entity-forms";
import { WorkspaceGate } from "@/components/workspace-gate";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { WorkspaceDto } from "@/lib/crm-api";

export const Route = createFileRoute("/contacts")({ component: ContactsPage });

function ContactsPage() {
  return (
    <AppShell>
      <WorkspaceGate>{(workspace) => <ContactList workspace={workspace} />}</WorkspaceGate>
    </AppShell>
  );
}

function ContactList({ workspace }: { workspace: WorkspaceDto }) {
  const [query, setQuery] = useState("");
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return workspace.contacts;
    return workspace.contacts.filter((contact) =>
      `${contact.name} ${contact.companyName} ${contact.email ?? ""}`
        .toLowerCase()
        .includes(needle),
    );
  }, [workspace.contacts, query]);
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs font-medium tracking-[0.18em] text-muted-foreground uppercase">
            People
          </p>
          <h1 className="font-display text-4xl tracking-tight">Contacts</h1>
        </div>
        <ContactForm trigger={<Button>New contact</Button>} />
      </div>
      <Input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Filter by name, company, or email"
      />
      <ul className="divide-y divide-border rounded-xl border border-border bg-card">
        {filtered.map((contact) => (
          <li key={contact.id} className="flex items-center justify-between gap-3 p-4">
            <div>
              <p className="font-medium">{contact.name}</p>
              <p className="text-sm text-muted-foreground">
                {contact.title ? `${contact.title} · ` : ""}
                {contact.companyName}
              </p>
            </div>
            <ContactForm
              contact={contact}
              trigger={
                <Button variant="ghost" size="sm">
                  Edit
                </Button>
              }
            />
          </li>
        ))}
      </ul>
    </div>
  );
}
