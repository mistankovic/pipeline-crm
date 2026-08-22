import { createFileRoute } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { ForecastView } from "@/components/forecast-view";
import { WorkspaceGate } from "@/components/workspace-gate";

export const Route = createFileRoute("/forecast")({ component: ForecastPage });

function ForecastPage() {
  return (
    <AppShell>
      <WorkspaceGate>{(workspace) => <ForecastView workspace={workspace} />}</WorkspaceGate>
    </AppShell>
  );
}
