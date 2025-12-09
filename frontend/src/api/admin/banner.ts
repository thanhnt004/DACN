import api from '../http';

export interface Banner {
  id: string;
  title: string;
  description?: string;
  imageUrl: string;
  linkUrl?: string;
  isActive: boolean;
  displayOrder: number;
  startDate?: string;
  endDate?: string;
  createdAt: string;
  updatedAt: string;
  isCurrentlyValid: boolean;
}

export interface BannerCreateRequest {
  title: string;
  description?: string;
  imageUrl: string;
  linkUrl?: string;
  isActive?: boolean;
  displayOrder?: number;
  startDate?: string;
  endDate?: string;
}

export interface BannerUpdateRequest {
  title?: string;
  description?: string;
  imageUrl?: string;
  linkUrl?: string;
  isActive?: boolean;
  displayOrder?: number;
  startDate?: string;
  endDate?: string;
}

// Public API
export const getPublicBanners = async (): Promise<Banner[]> => {
  const response = await api.get<Banner[]>('/api/v1/banners/public');
  return response.data;
};

// Admin APIs
export const getAllBanners = async (): Promise<Banner[]> => {
  const response = await api.get<Banner[]>('/api/v1/admin/banners');
  return response.data;
};

export const getBannerById = async (id: string): Promise<Banner> => {
  const response = await api.get<Banner>(`/api/v1/admin/banners/${id}`);
  return response.data;
};

export const createBanner = async (request: BannerCreateRequest): Promise<Banner> => {
  const response = await api.post<Banner>('/api/v1/admin/banners', request);
  return response.data;
};

export const updateBanner = async (id: string, request: BannerUpdateRequest): Promise<Banner> => {
  const response = await api.put<Banner>(`/api/v1/admin/banners/${id}`, request);
  return response.data;
};

export const deleteBanner = async (id: string): Promise<void> => {
  await api.delete(`/api/v1/admin/banners/${id}`);
};

export const toggleBannerStatus = async (id: string): Promise<Banner> => {
  const response = await api.patch<Banner>(`/api/v1/admin/banners/${id}/toggle-status`);
  return response.data;
};

export const reorderBanners = async (bannerIds: string[]): Promise<void> => {
  await api.put('/api/v1/admin/banners/reorder', bannerIds);
};