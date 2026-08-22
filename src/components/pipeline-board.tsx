import { useNavigate } from "@tanstack/react-router";
import type { DragEvent } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DealForm } from "@/components/entity-forms";
import type { DealDto, WorkspaceDto } from "@/lib/crm-api";
import { moneyLabel, STAGE_LABEL, STAGE_ORDER } from "@/lib/utils";
import { useCrmMutations } from "@/lib/workspace";

function stageTone(stage: string): string {
  if (stage === "CLOSED_WON") return "text-won border-won/40";
  if (stage === "CLOSED_LOST") return "text-lost border-lost/40";
  return "text-muted-foreground";
}

function DealCard({ deal }: { deal: DealDto }) {
  const navigate = useNavigate();
  return (
    <article
      draggable
      onDragStart={(event: DragEvent) => {
        event.dataTransfer.setData("text/deal-id", deal.id);
        event.dataTransfer.effectAllowed = "move";
      }}
      onClick={() => navigate({ to: "/deals/$dealId", params: { dealId: deal.id } })}
      className="cursor-pointer rounded-lg border border-border bg-card p-3 transition-colors hover:border-ring"
    >
      <p className="text-sm font-medium text-foreground">{deal.title}</p>
      <p className="mt-1 text-xs text-muted-foreground">{deal.companyName}</p>
      <div className="mt-3 flex items-baseline justify-between gap-2">
        <span className="font-mono text-sm tabular-nums">
          {moneyLabel(deal.amount, deal.currency)}
        </span>
        <span className="text-xs tabular-nums text-muted-foreground">{deal.probability}%</span>
      </div>
      <div className="mt-2 h-1 rounded-full bg-muted">
        <div
          className="h-1 rounded-full bg-primary"
          style={{ width: `${deal.probability}%` }}
        />
      </div>
      <p className="mt-2 text-[11px] text-muted-foreground">{deal.ownerName}</p>
    </article>
  );
}

export function PipelineBoard({ workspace }: { workspace: WorkspaceDto }) {
  const { moveDeal } = useCrmMutations();
  const onDrop = (stage: string, event: DragEvent<HTMLElement>) => {
    event.preventDefault();
    const dealId = event.dataTransfer.getData("text/deal-id");
    if (!dealId) return;
    const current = workspace.deals.find((deal) => deal.id === dealId);
    if (!current || current.stage === stage) return;
    moveDeal.mutate({ dealId, targetStage: stage });
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs font-medium tracking-[0.18em] text-muted-foreground uppercase">
            Open pipeline
          </p>
          <h1 className="font-display text-4xl tracking-tight">The board</h1>
        </div>
        <DealForm trigger={<Button>New deal</Button>} />
      </div>
      <div className="flex gap-3 overflow-x-auto pb-4">
        {STAGE_ORDER.map((stage) => {
          const deals = workspace.deals.filter((deal) => deal.stage === stage);
          return (
            <section
              key={stage}
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => onDrop(stage, event)}
              className="flex w-[260px] shrink-0 flex-col rounded-xl border border-border bg-muted/40 p-3"
            >
              <header className="mb-3 flex items-center justify-between gap-2">
                <Badge className={stageTone(stage)}>{STAGE_LABEL[stage]}</Badge>
                <span className="text-xs tabular-nums text-muted-foreground">
                  {deals.length}
                </span>
              </header>
              <div className="flex min-h-40 flex-col gap-2">
                {deals.map((deal) => (
                  <DealCard key={deal.id} deal={deal} />
                ))}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}
