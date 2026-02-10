import axios from "axios";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

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

  /** Lightweight status-only endpoint (CDN-decoupled flow) */
  getSeatStatuses: (showId: string) =>
    apiClient.get(`/api/v1/seats/status/${showId}`),

  getSeatAvailability: (showId: string) =>
    apiClient.get(`/api/v1/seats/show/${showId}/availability`),
};

// ---- Layout API (simulated CDN) ----
export const layoutApi = {
  /** Fetch static screen layout JSON (cached by browser, simulates CDN) */
  getScreenLayout: (screenId: string) =>
    apiClient.get(`/layouts/screen-${screenId}.json`),
};

// ---- Booking API ----
export const bookingApi = {
  lockSeats: (data: { showId: string; seatIds: string[] }) =>
    apiClient.post("/api/v1/bookings/lock", data),

  confirmBooking: (data: {
    bookingId: string;
    lockToken: string;
    guestName: string;
    guestEmail: string;
    guestPhone: string;
  }) => apiClient.post("/api/v1/bookings/confirm", data),

  cancelBooking: (bookingId: string) =>
    apiClient.post(`/api/v1/bookings/${bookingId}/cancel`),

  getBooking: (bookingId: string) =>
    apiClient.get(`/api/v1/bookings/${bookingId}`),

  getBookingByNumber: (bookingNumber: string) =>
    apiClient.get(`/api/v1/bookings/number/${bookingNumber}`),
};

export default apiClient;
