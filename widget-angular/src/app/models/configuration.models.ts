export interface CreateConfigurationRequest {
  productId: string;
  kbId: string;
  locale?: string;
}

export interface CreateConfigurationResponse {
  configId: string;
  status: string;
}