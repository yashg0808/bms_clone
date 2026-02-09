import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("bms_token");
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      if (typeof window !== "undefined") {
        localStorage.removeItem("bms_token");
        localStorage.removeItem("bms_user");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

// ---- Auth API ----
export const authApi = {
  register: (data: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
  }) => apiClient.post("/api/v1/auth/register", data),

  login: (data: { email: string; password: string }) =>
    apiClient.post("/api/v1/auth/login", data),

  getProfile: () => apiClient.get("/api/v1/users/me"),

  updateProfile: (data: { fullName?: string; phone?: string }) =>
    apiClient.put("/api/v1/users/me", data),
};

// ---- Movie API ----
export const movieApi = {
  getMovies: (params?: { page?: number; size?: number; city?: string }) =>
    apiClient.get("/api/v1/movies", { params }),

  getMovie: (id: string) => apiClient.get(`/api/v1/movies/${id}`),

  searchMovies: (query: string) =>
    apiClient.get("/api/v1/movies/search", { params: { q: query } }),

  getFeatured: () => apiClient.get("/api/v1/movies/featured"),

  getMovieShows: (movieId: string, cityId: string, date: string) =>
    apiClient.get(`/api/v1/movies/${movieId}/shows`, {
      params: { cityId, date },
    }),
};

// ---- City & Theater API ----
export const locationApi = {
  getCities: () => apiClient.get("/api/v1/cities"),

  getTheaters: (cityId: string) =>
    apiClient.get(`/api/v1/theaters/city/${cityId}`),
};

// ---- Show & Seat API ----
export const showApi = {
  getShow: (id: string) => apiClient.get(`/api/v1/shows/${id}`),

  getShowSeats: (showId: string) =>
    apiClient.get(`/api/v1/seats/show/${showId}`),

  getSeatAvailability: (showId: string) =>
    apiClient.get(`/api/v1/seats/show/${showId}/availability`),
};

// ---- Booking API ----
export const bookingApi = {
  lockSeats: (data: { showId: string; seatIds: string[] }) =>
    apiClient.post("/api/v1/bookings/lock", data),

  confirmBooking: (data: {
    bookingId: string;
    lockToken: string;
    couponId?: string;
  }) => apiClient.post("/api/v1/bookings/confirm", data),

  cancelBooking: (bookingId: string) =>
    apiClient.post(`/api/v1/bookings/${bookingId}/cancel`),

  getBooking: (bookingId: string) =>
    apiClient.get(`/api/v1/bookings/${bookingId}`),

  getMyBookings: (params?: { page?: number; size?: number }) =>
    apiClient.get("/api/v1/bookings/my", { params }),
};

// ---- Payment API ----
export const paymentApi = {
  initiatePayment: (data: {
    bookingId: string;
    amount: number;
    paymentMethod: string;
    idempotencyKey: string;
  }) => apiClient.post("/api/v1/payments/initiate", data),

  verifyPayment: (data: {
    paymentId: string;
    gatewayPaymentId: string;
    gatewaySignature: string;
  }) => apiClient.post("/api/v1/payments/verify", data),

  getPayment: (paymentId: string) =>
    apiClient.get(`/api/v1/payments/${paymentId}`),
};

export default apiClient;
