export interface CreateConfigurationRequest {
  productId: string;
  kbId: string;
  local?: string;
}

export interface CreateConfigurationResponse {
  configId: string;
  status: string;
  configuration: string;                         // raw response-data
  responseTimeMs: number;
}