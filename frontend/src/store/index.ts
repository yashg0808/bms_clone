import { create } from "zustand";
import type { City } from "@/types";

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
  setLockInfo: (
    lockToken: string,
    bookingId: string,
    expiresAt: string,
  ) => void;
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
