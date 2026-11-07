export interface ApiResponse<T> {
  success: boolean;
  code: number;
  message?: string;
  timestamp: number;
  data: T;
}

export interface Page<T> {
  data: T[];
  currentPage?: number;
  totalPage?: number;
  cursor?: string;
  hasNext?: boolean;
}

export interface EventDto {
  id: string;
  name: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  poster?: string;
  version?: number;
}

export type EventRequest = Omit<EventDto, 'id' | 'version'>;

export const TICKET_STATUS = ['DRAFT', 'ACTIVE', 'SOLD_OUT', 'EXPIRED', 'CANCELLED'] as const;
export const TICKET_TYPE = ['VVIP', 'VIP', 'CAT', 'STANDARD'] as const;
export const SELLING_TYPE = ['PRE_SALE', 'GENERAL'] as const;

export interface TicketDto {
  id?: string; // BE TicketResponseDTO currently has no id
  eventId: string;
  status: string;
  type: string;
  sellingType: string;
  stockInitial: number;
  stockAvailable: number;
  originalPrice: number;
  flashPrice?: number;
  saleStartTime?: string;
  saleEndTime?: string;
  version?: number;
}

export interface TicketRequest {
  eventId: string;
  status: string;
  type: string;
  sellingType: string;
  stockInitial: number;
  stockAvailable: number;
  reservedStock: number;
  originalPrice: number;
  flashPrice?: number | null;
  saleStartTime?: string | null;
  saleEndTime?: string | null;
}

export interface OrderDto {
  id: string;
  orderNumber: string;
  customerId: string;
  ticketId: string;
  status: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  orderDate: string;
  expireTime: string;
}

export interface KeycloakToken {
  access_token: string;
  refresh_token: string;
  id_token?: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
}

/** keycloak-account-service UserRequest */
export interface KcUserRequest {
  userId?: string;
  username?: string;
  password?: string;
  avatar?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  dob?: string;
  city?: string;
}

export interface KcUserResponse {
  id: string;
  userId?: string;
  username?: string;
  avatar?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  dob?: string;
  city?: string;
}

export interface KcRoleDto {
  id?: string;
  name: string;
  description?: string;
}
