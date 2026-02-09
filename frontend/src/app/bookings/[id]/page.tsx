"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { bookingApi } from "@/lib/api";
import { Booking } from "@/types";
import { useAuthStore } from "@/store";
import { formatCurrency, formatDate, formatTime } from "@/lib/utils";
import {
  Ticket,
  Calendar,
  MapPin,
  Clock,
  ArrowLeft,
  Download,
  XCircle,
} from "lucide-react";
import toast from "react-hot-toast";

const statusColors: Record<string, string> = {
  CONFIRMED: "bg-green-100 text-green-700 border-green-200",
  PENDING_PAYMENT: "bg-amber-100 text-amber-700 border-amber-200",
  CANCELLED: "bg-red-100 text-red-700 border-red-200",
  EXPIRED: "bg-gray-100 text-gray-500 border-gray-200",
  REFUNDED: "bg-blue-100 text-blue-700 border-blue-200",
};

export default function BookingDetailPage() {
  const params = useParams();
  const router = useRouter();
  const bookingId = params.id as string;
  const { isAuthenticated } = useAuthStore();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    fetchBooking();
  }, [bookingId, isAuthenticated]);

  async function fetchBooking() {
    try {
      const res = await bookingApi.getBooking(bookingId);
      setBooking(res.data?.data);
    } catch {
      toast.error("Booking not found");
      router.push("/bookings");
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel() {
    if (!confirm("Are you sure you want to cancel this booking?")) return;
    setCancelling(true);
    try {
      await bookingApi.cancelBooking(bookingId);
      toast.success("Booking cancelled. Refund will be processed.");
      fetchBooking();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Failed to cancel booking"
      );
    } finally {
      setCancelling(false);
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (!booking) return null;

  const canCancel =
    booking.status === "CONFIRMED" &&
    booking.showTime &&
    new Date(booking.showTime) > new Date();

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <button
        onClick={() => router.push("/bookings")}
        className="mb-6 flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4" /> Back to Bookings
      </button>

      {/* Ticket Card */}
      <div className="overflow-hidden rounded-xl border shadow-lg">
        {/* Header */}
        <div className="bg-primary-500 p-6 text-white">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-bold">
                {booking.movieTitle || "Movie Ticket"}
              </h1>
              <p className="mt-1 text-primary-100">
                {booking.language} • {booking.format || "2D"}
              </p>
            </div>
            <span
              className={`rounded-full border px-3 py-1 text-xs font-bold ${
                statusColors[booking.status] || ""
              }`}
            >
              {booking.status.replace("_", " ")}
            </span>
          </div>
        </div>

        {/* Dashed separator */}
        <div className="relative">
          <div className="absolute -left-3 -top-3 h-6 w-6 rounded-full bg-gray-100" />
          <div className="absolute -right-3 -top-3 h-6 w-6 rounded-full bg-gray-100" />
          <div className="border-t-2 border-dashed border-gray-200" />
        </div>

        {/* Details */}
        <div className="space-y-5 p-6">
          {/* Venue */}
          <div className="flex items-start gap-3">
            <MapPin className="mt-0.5 h-5 w-5 flex-shrink-0 text-gray-400" />
            <div>
              <p className="font-semibold">{booking.theaterName}</p>
              <p className="text-sm text-gray-500">{booking.screenName}</p>
            </div>
          </div>

          {/* Date & Time */}
          {booking.showTime && (
            <div className="flex items-start gap-3">
              <Calendar className="mt-0.5 h-5 w-5 flex-shrink-0 text-gray-400" />
              <div>
                <p className="font-semibold">{formatDate(booking.showTime)}</p>
                <p className="text-sm text-gray-500">
                  {formatTime(booking.showTime)}
                </p>
              </div>
            </div>
          )}

          {/* Seats */}
          <div className="flex items-start gap-3">
            <Ticket className="mt-0.5 h-5 w-5 flex-shrink-0 text-gray-400" />
            <div>
              <p className="font-semibold">
                {booking.seatCount || booking.seats?.length || 0} Ticket
                {(booking.seatCount || booking.seats?.length || 0) !== 1
                  ? "s"
                  : ""}
              </p>
              {booking.seats && booking.seats.length > 0 && (
                <div className="mt-1 flex flex-wrap gap-1.5">
                  {booking.seats.map((seat, i) => (
                    <span
                      key={i}
                      className="rounded bg-gray-100 px-2 py-0.5 text-xs font-mono font-medium"
                    >
                      {seat.seatLabel || seat.seatNumber}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Booking Number */}
          <div className="flex items-start gap-3">
            <Clock className="mt-0.5 h-5 w-5 flex-shrink-0 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">Booking ID</p>
              <p className="font-mono font-semibold">{booking.bookingNumber}</p>
            </div>
          </div>
        </div>

        {/* Dashed separator */}
        <div className="relative">
          <div className="absolute -left-3 -top-3 h-6 w-6 rounded-full bg-gray-100" />
          <div className="absolute -right-3 -top-3 h-6 w-6 rounded-full bg-gray-100" />
          <div className="border-t-2 border-dashed border-gray-200" />
        </div>

        {/* Price Breakdown */}
        <div className="p-6">
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">Base Amount</span>
              <span>{formatCurrency(booking.baseAmount || 0)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Convenience Fee</span>
              <span>{formatCurrency(booking.convenienceFee || 0)}</span>
            </div>
            {booking.discountAmount > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Discount</span>
                <span>-{formatCurrency(booking.discountAmount)}</span>
              </div>
            )}
            <div className="border-t pt-2">
              <div className="flex justify-between text-base font-bold">
                <span>Total</span>
                <span>{formatCurrency(booking.totalAmount)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="mt-6 flex gap-3">
        {canCancel && (
          <button
            onClick={handleCancel}
            disabled={cancelling}
            className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-red-200 py-3 font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50"
          >
            <XCircle className="h-4 w-4" />
            {cancelling ? "Cancelling..." : "Cancel Booking"}
          </button>
        )}
        <button
          onClick={() => window.print()}
          className="flex flex-1 items-center justify-center gap-2 rounded-lg border py-3 font-semibold text-gray-700 hover:bg-gray-50"
        >
          <Download className="h-4 w-4" />
          Download Ticket
        </button>
      </div>
    </div>
  );
}
