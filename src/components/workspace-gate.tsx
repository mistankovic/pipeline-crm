import type { ReactNode } from "react";
import type { WorkspaceDto } from "@/lib/crm-api";
import { useWorkspace } from "@/lib/workspace";

export function WorkspaceGate({
  children,
}: {
  children: (workspace: WorkspaceDto) => ReactNode;
}) {
  const { data, isPending, error } = useWorkspace();
  if (isPending) {
    return <div className="h-72 animate-pulse rounded-xl bg-card" />;
  }
  if (error || !data) {
    return (
      <p className="text-sm text-muted-foreground">
        The pipeline could not be loaded. Sign in again, then refresh.
      </p>
    );
  }
  return <>{children(data)}</>;
}
