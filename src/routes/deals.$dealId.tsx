import { createFileRoute } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { DealDetail } from "@/components/deal-detail";
import { WorkspaceGate } from "@/components/workspace-gate";

export const Route = createFileRoute("/deals/$dealId")({ component: DealPage });

function DealPage() {
  const { dealId } = Route.useParams();
  return (
    <AppShell>
      <WorkspaceGate>
        {(workspace) => <DealDetail workspace={workspace} dealId={dealId} />}
      </WorkspaceGate>
    </AppShell>
  );
}
