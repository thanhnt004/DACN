export interface ProductBulkImportResponse {
  totalRows: number;
  successCount: number;
  errorCount: number;
  results: ImportResultItem[];
  errors: string[];
}

export interface ImportResultItem {
  rowNumber: number;
  status: 'SUCCESS' | 'ERROR' | 'SKIPPED';
  productName: string;
  sku: string;
  productId: string;
  variantId: string;
  message: string;
  errors: string[];
}
