import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  bootstrapCrm,
  moveDeal,
  saveActivity,
  saveCompany,
  saveContact,
  saveDeal,
  transferDeal,
  type WorkspaceDto,
} from "@/lib/crm-api";

export function useWorkspace() {
  return useQuery({
    queryKey: ["crm"],
    queryFn: async () => {
      const result = await bootstrapCrm();
      if (!result.ok) throw new Error(result.message);
      return result.data;
    },
  });
}

export function useCrmMutations() {
  const client = useQueryClient();
  const invalidate = () => client.invalidateQueries({ queryKey: ["crm"] });

  const wrap = <T>(
    fn: () => Promise<{ ok: true; data: T } | { ok: false; code: string; message: string }>,
  ) =>
    fn().then((result) => {
      if (!result.ok) {
        toast.error(result.message);
        throw new Error(result.message);
      }
      void invalidate();
      return result.data;
    });

  return {
    saveCompany: useMutation({
      mutationFn: (input: {
        id?: string;
        name: string;
        domain?: string | null;
        notes?: string | null;
      }) => wrap(() => saveCompany({ data: input })),
    }),
    saveContact: useMutation({
      mutationFn: (input: {
        id?: string;
        companyId: string;
        name: string;
        email?: string | null;
        title?: string | null;
      }) => wrap(() => saveContact({ data: input })),
    }),
    saveDeal: useMutation({
      mutationFn: (input: {
        id?: string;
        title: string;
        companyId: string;
        ownerId?: string;
        amount: number;
        currency: string;
        probability: number;
        stage?: string;
      }) => wrap(() => saveDeal({ data: input })),
    }),
    moveDeal: useMutation({
      mutationFn: async (input: { dealId: string; targetStage: string }) => {
        const snapshot = client.getQueryData<WorkspaceDto>(["crm"]);
        if (snapshot) {
          client.setQueryData<WorkspaceDto>(["crm"], {
            ...snapshot,
            deals: snapshot.deals.map((deal) =>
              deal.id === input.dealId ? { ...deal, stage: input.targetStage } : deal,
            ),
          });
        }
        const result = await moveDeal({ data: input });
        if (!result.ok) {
          toast.error(result.message);
          await invalidate();
          throw new Error(result.message);
        }
        await invalidate();
        return result.data;
      },
    }),
    transferDeal: useMutation({
      mutationFn: (input: { dealId: string; newOwnerId: string }) =>
        wrap(() => transferDeal({ data: input })),
    }),
    saveActivity: useMutation({
      mutationFn: (input: {
        type: string;
        body: string;
        dealId?: string | null;
        contactId?: string | null;
      }) => wrap(() => saveActivity({ data: input })),
    }),
  };
}
