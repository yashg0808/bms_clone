"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { bookingApi } from "@/lib/api";
import { Booking } from "@/types";
import { formatCurrency, formatDate, formatTime } from "@/lib/utils";
import { Ticket, Calendar, MapPin, ChevronRight } from "lucide-react";

const statusColors: Record<string, string> = {
  CONFIRMED: "bg-green-100 text-green-700",
  PENDING_PAYMENT: "bg-amber-100 text-amber-700",
  CANCELLED: "bg-red-100 text-red-700",
  EXPIRED: "bg-gray-100 text-gray-500",
  REFUNDED: "bg-blue-100 text-blue-700",
};

export default function BookingsPage() {
  const router = useRouter();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchBookings();
  }, [page]);

  async function fetchBookings() {
    try {
      const res = await bookingApi.getMyBookings({ page, size: 10 });
      const data = res.data?.data;
      setBookings(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch {
      setBookings([]);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold">My Bookings</h1>

      {bookings.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border-2 border-dashed py-16">
          <Ticket className="mb-4 h-12 w-12 text-gray-300" />
          <p className="mb-2 text-lg font-medium text-gray-500">
            No bookings yet
          </p>
          <p className="mb-6 text-sm text-gray-400">
            Explore movies and book your first show!
          </p>
          <button
            onClick={() => router.push("/movies")}
            className="rounded-lg bg-primary-500 px-6 py-2 font-semibold text-white hover:bg-primary-600"
          >
            Browse Movies
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((booking) => (
            <button
              key={booking.id}
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
                      {booking.status.replace("_", " ")}
                    </span>
                  </div>

                  <div className="mb-3 space-y-1 text-sm text-gray-500">
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
                      {formatCurrency(booking.totalAmount)}
                    </span>
                    <span className="font-mono text-xs text-gray-400">
                      #{booking.bookingNumber}
                    </span>
                  </div>
                </div>

                <ChevronRight className="mt-1 h-5 w-5 flex-shrink-0 text-gray-400" />
              </div>
            </button>
          ))}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-lg border px-4 py-2 text-sm font-medium disabled:opacity-50"
              >
                Previous
              </button>
              <span className="px-4 text-sm text-gray-500">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-lg border px-4 py-2 text-sm font-medium disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
