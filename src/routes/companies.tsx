import { createFileRoute } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { CompanyForm } from "@/components/entity-forms";
import { WorkspaceGate } from "@/components/workspace-gate";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/companies")({ component: CompaniesPage });

function CompaniesPage() {
  return (
    <AppShell>
      <WorkspaceGate>
        {(workspace) => (
          <div className="space-y-5">
            <div className="flex items-end justify-between gap-3">
              <div>
                <p className="text-xs font-medium tracking-[0.18em] text-muted-foreground uppercase">
                  Accounts
                </p>
                <h1 className="font-display text-4xl tracking-tight">Companies</h1>
              </div>
              <CompanyForm trigger={<Button>New company</Button>} />
            </div>
            <ul className="grid gap-3 md:grid-cols-2">
              {workspace.companies.map((company) => (
                <li key={company.id} className="rounded-xl border border-border bg-card p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h2 className="text-lg font-medium">{company.name}</h2>
                      <p className="text-sm text-muted-foreground">{company.domain ?? "No domain"}</p>
                    </div>
                    <CompanyForm
                      company={company}
                      trigger={
                        <Button variant="ghost" size="sm">
                          Edit
                        </Button>
                      }
                    />
                  </div>
                  {company.notes && (
                    <p className="mt-3 text-sm text-muted-foreground">{company.notes}</p>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
      </WorkspaceGate>
    </AppShell>
  );
}
