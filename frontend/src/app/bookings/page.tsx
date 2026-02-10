"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { bookingApi } from "@/lib/api";
import { Booking } from "@/types";
import { formatCurrency, formatDate, formatTime } from "@/lib/utils";
import { Ticket, Calendar, MapPin, Search } from "lucide-react";
import toast from "react-hot-toast";

const statusColors: Record<string, string> = {
  CONFIRMED: "bg-green-100 text-green-700",
  PENDING: "bg-amber-100 text-amber-700",
  CANCELLED: "bg-red-100 text-red-700",
  EXPIRED: "bg-gray-100 text-gray-500",
};

export default function BookingsPage() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState("");
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!searchQuery.trim()) {
      toast.error("Please enter a booking number or booking ID");
      return;
    }

    setLoading(true);
    setSearched(true);
    try {
      let res;
      // If it looks like a UUID, search by ID; otherwise by booking number
      if (searchQuery.includes("-") && searchQuery.length > 30) {
        res = await bookingApi.getBooking(searchQuery.trim());
      } else {
        res = await bookingApi.getBookingByNumber(searchQuery.trim());
      }
      setBooking(res.data?.data || null);
    } catch {
      setBooking(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-2 text-2xl font-bold">Find Your Booking</h1>
      <p className="mb-6 text-sm text-gray-500">
        Enter your booking number to view your booking details.
      </p>

      {/* Search form */}
      <form onSubmit={handleSearch} className="mb-8">
        <div className="flex gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Enter booking number (e.g. BMS-20250101120000-AB12CD)"
              className="w-full rounded-lg border py-3 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-primary-500 px-6 py-3 font-semibold text-white hover:bg-primary-600 disabled:bg-gray-300"
          >
            {loading ? "Searching..." : "Search"}
          </button>
        </div>
      </form>

      {/* Results */}
      {loading ? (
        <div className="flex items-center justify-center py-16">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : booking ? (
        <button
          onClick={() => router.push(`/bookings/${booking.id}`)}
          className="w-full rounded-lg border bg-white p-5 text-left shadow-sm transition-shadow hover:shadow-md"
        >
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="mb-2 flex items-center gap-3">
                <h3 className="text-lg font-semibold">
                  {booking.show?.movieTitle || "Movie"}
                </h3>
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                    statusColors[booking.status] || "bg-gray-100"
                  }`}
                >
                  {booking.status}
                </span>
              </div>

              <div className="mb-3 space-y-1 text-sm text-gray-500">
                <div className="flex items-center gap-1.5">
                  <Ticket className="h-3.5 w-3.5" />
                  <span>Booked by: {booking.guestName || "Guest"}</span>
                </div>
                {booking.show?.theaterName && (
                  <div className="flex items-center gap-1.5">
                    <MapPin className="h-3.5 w-3.5" />
                    <span>
                      {booking.show.theaterName} - {booking.show.screenName}
                    </span>
                  </div>
                )}
                {booking.show?.showDate && (
                  <div className="flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>
                      {formatDate(booking.show.showDate)}
                      {booking.show.startTime
                        ? ` at ${formatTime(booking.show.startTime)}`
                        : ""}
                    </span>
                  </div>
                )}
              </div>

              <div className="flex items-center gap-4 text-sm">
                <span className="text-gray-500">
                  {booking.seats?.length || 0} ticket
                  {(booking.seats?.length || 0) !== 1 ? "s" : ""}
                </span>
                <span className="font-bold text-gray-900">
                  {formatCurrency(booking.finalAmount)}
                </span>
                <span className="font-mono text-xs text-gray-400">
                  #{booking.bookingNumber}
                </span>
              </div>
            </div>
          </div>
        </button>
      ) : searched ? (
        <div className="flex flex-col items-center justify-center rounded-lg border-2 border-dashed py-16">
          <Ticket className="mb-4 h-12 w-12 text-gray-300" />
          <p className="mb-2 text-lg font-medium text-gray-500">
            No booking found
          </p>
          <p className="mb-6 text-sm text-gray-400">
            Check the booking number and try again.
          </p>
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center rounded-lg border-2 border-dashed py-16">
          <Ticket className="mb-4 h-12 w-12 text-gray-300" />
          <p className="mb-2 text-lg font-medium text-gray-500">
            Look up your booking
          </p>
          <p className="mb-6 text-sm text-gray-400">
            Enter your booking number above to find your booking details.
          </p>
          <button
            onClick={() => router.push("/movies")}
            className="rounded-lg bg-primary-500 px-6 py-2 font-semibold text-white hover:bg-primary-600"
          >
            Browse Movies
          </button>
        </div>
      )}
    </div>
  );
}
