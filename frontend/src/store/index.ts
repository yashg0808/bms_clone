import { create } from "zustand";
import type { User, City } from "@/types";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  loadFromStorage: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  login: (token, user) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("bms_token", token);
      localStorage.setItem("bms_user", JSON.stringify(user));
    }
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem("bms_token");
      localStorage.removeItem("bms_user");
    }
    set({ token: null, user: null, isAuthenticated: false });
  },

  loadFromStorage: () => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("bms_token");
      const userStr = localStorage.getItem("bms_user");
      if (token && userStr) {
        try {
          const user = JSON.parse(userStr);
          set({ token, user, isAuthenticated: true });
        } catch {
          set({ token: null, user: null, isAuthenticated: false });
        }
      }
    }
  },
}));

interface CityState {
  selectedCity: City | null;
  cities: City[];
  setSelectedCity: (city: City) => void;
  setCities: (cities: City[]) => void;
  loadFromStorage: () => void;
}

export const useCityStore = create<CityState>((set) => ({
  selectedCity: null,
  cities: [],

  setSelectedCity: (city) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("bms_city", JSON.stringify(city));
    }
    set({ selectedCity: city });
  },

  setCities: (cities) => set({ cities }),

  loadFromStorage: () => {
    if (typeof window !== "undefined") {
      const cityStr = localStorage.getItem("bms_city");
      if (cityStr) {
        try {
          const city = JSON.parse(cityStr);
          set({ selectedCity: city });
        } catch {
          // ignore
        }
      }
    }
  },
}));

interface BookingState {
  selectedSeats: string[];
  lockToken: string | null;
  bookingId: string | null;
  expiresAt: string | null;
  toggleSeat: (seatId: string) => void;
  clearSeats: () => void;
  setLockInfo: (lockToken: string, bookingId: string, expiresAt: string) => void;
  clearBooking: () => void;
}

export const useBookingStore = create<BookingState>((set) => ({
  selectedSeats: [],
  lockToken: null,
  bookingId: null,
  expiresAt: null,

  toggleSeat: (seatId) =>
    set((state) => ({
      selectedSeats: state.selectedSeats.includes(seatId)
        ? state.selectedSeats.filter((id) => id !== seatId)
        : state.selectedSeats.length < 10
          ? [...state.selectedSeats, seatId]
          : state.selectedSeats,
    })),

  clearSeats: () => set({ selectedSeats: [] }),

  setLockInfo: (lockToken, bookingId, expiresAt) =>
    set({ lockToken, bookingId, expiresAt }),

  clearBooking: () =>
    set({
      selectedSeats: [],
      lockToken: null,
      bookingId: null,
      expiresAt: null,
    }),
}));
