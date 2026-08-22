import { createFileRoute } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { PipelineBoard } from "@/components/pipeline-board";
import { WorkspaceGate } from "@/components/workspace-gate";
import { useCurrentUserState } from "@/lib/auth/use-current-user";
import { Login } from "@/routes/login";

export const Route = createFileRoute("/")({ component: Home });

function Home() {
  const { user, isPending } = useCurrentUserState();
  if (isPending || !user) return <Login />;
  return (
    <AppShell>
      <WorkspaceGate>{(workspace) => <PipelineBoard workspace={workspace} />}</WorkspaceGate>
    </AppShell>
  );
}
