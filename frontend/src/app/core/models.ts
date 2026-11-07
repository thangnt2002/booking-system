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

export interface AuthResponse {
  authenticated?: boolean;
  isAuthenticated?: boolean;
  accessToken: string;
  refreshToken: string;
}

export interface PermissionDto {
  name: string;
  description?: string;
}

export interface RoleDto {
  name: string;
  description?: string;
  permissions?: PermissionDto[];
}

export interface UserCreationRequest {
  username: string;
  password: string;
  firstName?: string;
  lastName?: string;
  dob?: string;
  avatar?: string;
  email?: string;
  city?: string;
}
