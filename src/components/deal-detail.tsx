import { Link } from "@tanstack/react-router";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ActivityForm, DealForm } from "@/components/entity-forms";
import type { WorkspaceDto } from "@/lib/crm-api";
import { moneyLabel, STAGE_LABEL, STAGE_ORDER } from "@/lib/utils";
import { useCrmMutations } from "@/lib/workspace";

export function DealDetail({
  workspace,
  dealId,
}: {
  workspace: WorkspaceDto;
  dealId: string;
}) {
  const deal = workspace.deals.find((item) => item.id === dealId);
  const { moveDeal, transferDeal } = useCrmMutations();
  if (!deal) {
    return (
      <div className="space-y-3">
        <p>That deal is not on the board.</p>
        <Button asChild variant="outline">
          <Link to="/">Back to pipeline</Link>
        </Button>
      </div>
    );
  }
  const timeline = workspace.activities
    .filter((item) => item.dealId === deal.id)
    .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt));

  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_20rem]">
      <div className="space-y-6">
        <div>
          <Link to="/" className="text-xs tracking-wide text-muted-foreground uppercase">
            Pipeline
          </Link>
          <div className="mt-2 flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 className="font-display text-4xl tracking-tight">{deal.title}</h1>
              <p className="mt-1 text-muted-foreground">{deal.companyName}</p>
            </div>
            <DealForm deal={deal} trigger={<Button variant="outline">Edit</Button>} />
          </div>
        </div>
        <div className="grid gap-3 rounded-xl border border-border bg-card p-5 sm:grid-cols-3">
          <div>
            <p className="text-xs tracking-wide text-muted-foreground uppercase">Value</p>
            <p className="mt-1 font-mono text-xl tabular-nums">
              {moneyLabel(deal.amount, deal.currency)}
            </p>
          </div>
          <div>
            <p className="text-xs tracking-wide text-muted-foreground uppercase">Probability</p>
            <p className="mt-1 font-mono text-xl tabular-nums">{deal.probability}%</p>
          </div>
          <div>
            <p className="text-xs tracking-wide text-muted-foreground uppercase">Owner</p>
            <p className="mt-1 text-xl">{deal.ownerName}</p>
          </div>
        </div>
        <section className="space-y-3">
          <h2 className="text-sm font-medium tracking-wide uppercase">Timeline</h2>
          {timeline.length === 0 ? (
            <p className="text-sm text-muted-foreground">No activity yet.</p>
          ) : (
            <ol className="space-y-3">
              {timeline.map((item) => (
                <li key={item.id} className="rounded-lg border border-border bg-card p-4">
                  <div className="flex items-center justify-between gap-2">
                    <Badge>{item.type}</Badge>
                    <time className="text-xs text-muted-foreground">
                      {new Date(item.occurredAt).toLocaleString()}
                    </time>
                  </div>
                  <p className="mt-2 text-sm">{item.body}</p>
                  <p className="mt-2 text-xs text-muted-foreground">{item.createdByName}</p>
                </li>
              ))}
            </ol>
          )}
        </section>
        <section className="rounded-xl border border-border bg-card p-5">
          <h2 className="mb-3 text-sm font-medium tracking-wide uppercase">Log activity</h2>
          <ActivityForm dealId={deal.id} />
        </section>
      </div>
      <aside className="space-y-4">
        <section className="rounded-xl border border-border bg-card p-5">
          <h2 className="mb-3 text-sm font-medium tracking-wide uppercase">Stage</h2>
          <p className="mb-3 text-lg">{STAGE_LABEL[deal.stage]}</p>
          <div className="grid gap-2">
            {STAGE_ORDER.filter((stage) => stage !== deal.stage).map((stage) => (
              <Button
                key={stage}
                variant="outline"
                size="sm"
                onClick={() => moveDeal.mutate({ dealId: deal.id, targetStage: stage })}
              >
                Move to {STAGE_LABEL[stage]}
              </Button>
            ))}
          </div>
        </section>
        {workspace.me.role === "MANAGER" && (
          <section className="rounded-xl border border-border bg-card p-5">
            <h2 className="mb-3 text-sm font-medium tracking-wide uppercase">Reassign</h2>
            <select
              className="h-11 w-full rounded-md border border-input bg-muted px-3 text-sm"
              defaultValue={deal.ownerId}
              onChange={(event) =>
                transferDeal.mutate({ dealId: deal.id, newOwnerId: event.target.value })
              }
            >
              {workspace.users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name}
                </option>
              ))}
            </select>
          </section>
        )}
      </aside>
    </div>
  );
}
