import api from '../http';

export interface ActorInfo {
    id: string;
    email: string;
    fullName: string;
    role: string;
}

export interface AuditLogResponse {
    id: string;
    actor: ActorInfo;
    action: string;
    actionDescription: string;
    entityType: string;
    entityTypeDescription: string;
    entityId: string;
    metadata: Record<string, any>;
    traceId: string;
    createdAt: string; // ISO-8601 Instant string from backend
}

export async function getEntityHistory(entityId: string): Promise<AuditLogResponse[]> {
    const response = await api.get<AuditLogResponse[]>(`/api/v1/admin/audit-logs/entity/${entityId}`);
    return response.data;
}