import axios from "axios";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach X-City-ID header on every request for geo-shard routing
apiClient.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const cityStr = localStorage.getItem("bms_city");
    if (cityStr) {
      try {
        const city = JSON.parse(cityStr);
        if (city?.name) {
          config.headers["X-City-ID"] = city.name;
        }
      } catch {
        // ignore parse errors
      }
    }
  }
  return config;
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

// ---- Admin API ----
export const adminApi = {
  // Dashboard
  getDashboardStats: () => apiClient.get("/api/admin/dashboard/stats"),
  getBookingStats: () => apiClient.get("/api/admin/bookings/stats"),

  // Movies
  getMovies: (params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) => apiClient.get("/api/admin/movies", { params }),
  createMovie: (data: any) => apiClient.post("/api/admin/movies", data),
  updateMovie: (id: string, data: any) =>
    apiClient.put(`/api/admin/movies/${id}`, data),
  deleteMovie: (id: string) => apiClient.delete(`/api/admin/movies/${id}`),
  toggleMovieActive: (id: string) =>
    apiClient.patch(`/api/admin/movies/${id}/toggle-active`),

  // Shows
  getShows: (params?: {
    page?: number;
    size?: number;
    movieId?: string;
    screenId?: string;
    date?: string;
  }) => apiClient.get("/api/admin/shows", { params }),
  createShow: (data: any) => apiClient.post("/api/admin/shows", data),
  updateShow: (id: string, data: any) =>
    apiClient.put(`/api/admin/shows/${id}`, data),
  deleteShow: (id: string) => apiClient.delete(`/api/admin/shows/${id}`),
  bulkCreateShows: (data: any[]) =>
    apiClient.post("/api/admin/shows/bulk", data),

  // Theaters
  getTheaters: () => apiClient.get("/api/admin/theaters"),
  createTheater: (data: any) => apiClient.post("/api/admin/theaters", data),
  updateTheater: (id: string, data: any) =>
    apiClient.put(`/api/admin/theaters/${id}`, data),
  deleteTheater: (id: string) => apiClient.delete(`/api/admin/theaters/${id}`),
  getScreens: (theaterId: string) =>
    apiClient.get(`/api/admin/theaters/${theaterId}/screens`),
  createScreen: (data: any) =>
    apiClient.post("/api/admin/theaters/screens", data),
  getCities: () => apiClient.get("/api/admin/theaters/cities"),

  // Bookings
  getBookings: (params?: {
    page?: number;
    size?: number;
    status?: string;
    search?: string;
  }) => apiClient.get("/api/admin/bookings", { params }),
  getBookingsByDate: (
    startDate: string,
    endDate: string,
    page?: number,
    size?: number,
  ) =>
    apiClient.get("/api/admin/bookings/by-date", {
      params: { startDate, endDate, page, size },
    }),
  cancelBooking: (id: string, reason?: string) =>
    apiClient.post(`/api/admin/bookings/${id}/cancel`, { reason }),

  // Cache Management
  getCacheStatus: () => apiClient.get("/api/admin/cache/status"),
  clearCache: (cacheName: string) =>
    apiClient.delete(`/api/admin/cache/${cacheName}`),
  clearAllCaches: () => apiClient.delete("/api/admin/cache"),
};

export default apiClient;
