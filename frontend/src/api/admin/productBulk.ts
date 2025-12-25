import api from '@/api/http';
import { ProductBulkImportResponse } from '@/types/product';

export const ProductBulkAPI = {
  downloadTemplate: () => {
    return api.get<Blob>('/api/v1/admin/products/bulk-import/template', {
      responseType: 'blob',
    });
  },

  downloadSample: () => {
    return api.get<Blob>('/api/v1/admin/products/bulk-import/sample', {
      responseType: 'blob',
    });
  },

  bulkImport: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<ProductBulkImportResponse>(
      '/api/v1/admin/products/bulk-import',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
  },
};
