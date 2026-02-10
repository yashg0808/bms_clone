export interface Movie {
  id: string;
  title: string;
  description: string;
  genre: string;
  language: string;
  durationMinutes: number;
  rating: string;
  posterUrl: string;
  bannerUrl: string;
  trailerUrl: string;
  releaseDate: string;
  cast: { name: string; role: string }[];
  crew: { name: string; role: string }[];
  averageRating: number;
  totalReviews: number;
  featured: boolean;
}

export interface City {
  id: string;
  name: string;
  state: string;
}

export interface Theater {
  id: string;
  name: string;
  address: string;
  city: City;
  screens: Screen[];
}

export interface Screen {
  id: string;
  name: string;
  totalSeats: number;
  screenType: string;
}

export interface Show {
  id: string;
  movieId: string;
  movieTitle: string;
  screenId: string;
  screenName: string;
  screenType: string;
  theaterName: string;
  theaterId: string;
  showDate: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  premiumPrice: number;
  reclinerPrice: number;
  availableSeats: number;
}

export interface ShowSeat {
  id: string;
  showId: string;
  seatId: string;
  status: "AVAILABLE" | "LOCKED" | "BOOKED";
  price: number;
  seatRow?: string;
  seatNumber?: string;
  seatType?: string;
}

export interface LockSeatsResponse {
  lockToken: string;
  showId: string;
  lockedSeats: {
    seatId: string;
    seatRow: string;
    seatNumber: string;
    seatType: string;
    price: number;
  }[];
  totalAmount: number;
  expiresAt: string;
}

export interface Booking {
  id: string;
  bookingNumber: string;
  userId: string;
  showId: string;
  show?: Show;
  status:
    | "PENDING_PAYMENT"
    | "CONFIRMED"
    | "CANCELLED"
    | "EXPIRED"
    | "REFUNDED";
  totalAmount: number;
  convenienceFee: number;
  discount: number;
  finalAmount: number;
  lockToken: string | null;
  expiresAt: string | null;
  seats: {
    seatId: string;
    seatRow: string;
    seatNumber: string;
    seatType: string;
    price: number;
  }[];
  createdAt: string;
}

export interface Payment {
  id: string;
  bookingId: string;
  amount: number;
  status:
    | "INITIATED"
    | "PROCESSING"
    | "SUCCESS"
    | "FAILED"
    | "REFUND_INITIATED"
    | "REFUNDED";
  paymentMethod: string;
  gatewayOrderId: string;
  gatewayPaymentId: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
