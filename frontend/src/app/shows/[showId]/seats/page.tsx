"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { showApi, bookingApi } from "@/lib/api";
import { ShowSeat } from "@/types";
import { useBookingStore } from "@/store";
import SeatMap from "@/components/booking/SeatMap";
import { formatCurrency } from "@/lib/utils";
import toast from "react-hot-toast";
import { Clock, CheckCircle, User, Mail, Phone } from "lucide-react";

export default function SeatSelectionPage() {
  const params = useParams();
  const router = useRouter();
  const showId = params.showId as string;
  const {
    selectedSeats,
    clearSeats,
    setLockInfo,
    lockToken,
    bookingId,
    expiresAt,
    clearBooking,
  } = useBookingStore();
  const [seats, setSeats] = useState<ShowSeat[]>([]);
  const [loading, setLoading] = useState(true);
  const [locking, setLocking] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [success, setSuccess] = useState(false);
  const [confirmedBooking, setConfirmedBooking] = useState<any>(null);

  // Guest details form state
  const [showGuestForm, setShowGuestForm] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [guestEmail, setGuestEmail] = useState("");
  const [guestPhone, setGuestPhone] = useState("");
  const [timeLeft, setTimeLeft] = useState(0);
  const [totalAmount, setTotalAmount] = useState(0);

  useEffect(() => {
    fetchSeats();
    return () => {
      if (!showGuestForm) clearSeats();
    };
  }, [showId]);

  // Countdown timer for lock expiry
  useEffect(() => {
    if (!expiresAt || success) return;

    const updateTimer = () => {
      const remaining = Math.max(
        0,
        Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000),
      );
      setTimeLeft(remaining);
      if (remaining === 0) {
        toast.error("Booking expired! Seats have been released.");
        clearBooking();
        setShowGuestForm(false);
        fetchSeats();
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, success]);

  const formatTime = useCallback((seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
  }, []);

  async function fetchSeats() {
    try {
      const res = await showApi.getShowSeats(showId);
      setSeats(res.data?.data || []);
    } catch (error) {
      toast.error("Failed to load seats");
    } finally {
      setLoading(false);
    }
  }

  async function handleProceed() {
    if (selectedSeats.length === 0) {
      toast.error("Please select at least one seat");
      return;
    }

    setLocking(true);
    try {
      const res = await bookingApi.lockSeats({
        showId,
        seatIds: selectedSeats,
      });

      const data = res.data?.data;
      setLockInfo(data.lockToken, data.bookingId || "", data.expiresAt);
      setTotalAmount(data.totalAmount);
      setShowGuestForm(true);

      toast.success("Seats locked! Enter your details to confirm booking.");
    } catch (error: any) {
      const message = error.response?.data?.message || "Failed to lock seats";
      toast.error(message);
      fetchSeats();
    } finally {
      setLocking(false);
    }
  }

  async function handleConfirmBooking(e: React.FormEvent) {
    e.preventDefault();

    if (!bookingId || !lockToken) {
      toast.error("No active booking found");
      return;
    }

    if (!guestName.trim() || !guestEmail.trim() || !guestPhone.trim()) {
      toast.error("Please fill in all fields");
      return;
    }

    setConfirming(true);
    try {
      const res = await bookingApi.confirmBooking({
        bookingId,
        lockToken,
        guestName: guestName.trim(),
        guestEmail: guestEmail.trim(),
        guestPhone: guestPhone.trim(),
      });

      setSuccess(true);
      setConfirmedBooking(res.data?.data);
      toast.success("Booking confirmed!");
      clearBooking();
    } catch (error: any) {
      const message =
        error.response?.data?.message || "Failed to confirm booking";
      toast.error(message);
      if (
        error.response?.status === 410 ||
        error.response?.data?.message?.includes("expired")
      ) {
        clearBooking();
        setShowGuestForm(false);
        fetchSeats();
      }
    } finally {
      setConfirming(false);
    }
  }

  // Success screen
  if (success && confirmedBooking) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-green-100">
          <CheckCircle className="h-10 w-10 text-green-600" />
        </div>
        <h1 className="mb-2 text-2xl font-bold">Booking Confirmed!</h1>
        <p className="mb-6 text-gray-500">
          Your tickets have been booked successfully.
        </p>
        <div className="mb-8 rounded-lg bg-gray-50 p-4 text-left text-sm">
          <div className="mb-2 flex justify-between">
            <span className="text-gray-500">Booking Number</span>
            <span className="font-mono font-medium">
              {confirmedBooking.bookingNumber}
            </span>
          </div>
          <div className="mb-2 flex justify-between">
            <span className="text-gray-500">Name</span>
            <span className="font-medium">{confirmedBooking.guestName}</span>
          </div>
          <div className="mb-2 flex justify-between">
            <span className="text-gray-500">Seats</span>
            <span className="font-medium">
              {confirmedBooking.seats?.length || 0} ticket(s)
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Total</span>
            <span className="font-bold">
              {formatCurrency(confirmedBooking.finalAmount)}
            </span>
          </div>
        </div>
        <div className="space-y-3">
          <button
            onClick={() => router.push(`/bookings/${confirmedBooking.id}`)}
            className="w-full rounded-lg bg-primary-500 py-3 font-semibold text-white hover:bg-primary-600"
          >
            View Booking
          </button>
          <button
            onClick={() => router.push("/")}
            className="w-full rounded-lg border py-3 font-semibold text-gray-700 hover:bg-gray-50"
          >
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  // Guest details form (shown after seats are locked)
  if (showGuestForm) {
    return (
      <div className="mx-auto max-w-lg px-4 py-8">
        {/* Timer Bar */}
        <div
          className={`mb-6 flex items-center justify-between rounded-lg p-4 ${
            timeLeft <= 60
              ? "bg-red-50 text-red-700"
              : "bg-amber-50 text-amber-700"
          }`}
        >
          <div className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            <span className="font-medium">Complete booking in</span>
          </div>
          <span className="font-mono text-xl font-bold">
            {formatTime(timeLeft)}
          </span>
        </div>

        <h1 className="mb-2 text-2xl font-bold">Enter Your Details</h1>
        <p className="mb-6 text-sm text-gray-500">
          Fill in your details to confirm the booking. A confirmation will be
          sent to your email.
        </p>

        {/* Booking summary */}
        <div className="mb-6 rounded-lg border bg-gray-50 p-4">
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">
              {selectedSeats.length} seat(s) selected
            </span>
            <span className="font-bold">{formatCurrency(totalAmount)}</span>
          </div>
        </div>

        <form onSubmit={handleConfirmBooking} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Full Name
            </label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={guestName}
                onChange={(e) => setGuestName(e.target.value)}
                placeholder="Enter your full name"
                required
                className="w-full rounded-lg border py-3 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Email Address
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="email"
                value={guestEmail}
                onChange={(e) => setGuestEmail(e.target.value)}
                placeholder="Enter your email"
                required
                className="w-full rounded-lg border py-3 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Phone Number
            </label>
            <div className="relative">
              <Phone className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="tel"
                value={guestPhone}
                onChange={(e) => setGuestPhone(e.target.value)}
                placeholder="Enter your phone number"
                required
                className="w-full rounded-lg border py-3 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={confirming || timeLeft === 0}
            className="w-full rounded-lg bg-primary-500 py-4 text-lg font-bold text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:bg-gray-300"
          >
            {confirming ? (
              <span className="flex items-center justify-center gap-2">
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                Confirming...
              </span>
            ) : (
              "Confirm Booking"
            )}
          </button>
        </form>

        <button
          onClick={() => {
            clearBooking();
            setShowGuestForm(false);
            fetchSeats();
          }}
          className="mt-4 w-full text-center text-sm text-gray-500 hover:text-gray-700"
        >
          Cancel and go back to seat selection
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-2 text-2xl font-bold">Select Your Seats</h1>
      <p className="mb-8 text-sm text-gray-500">
        You can select up to 10 seats. Locked seats will be held for 8 minutes.
      </p>

      <SeatMap seats={seats} onProceed={handleProceed} />

      {locking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="rounded-lg bg-white p-8 text-center">
            <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
            <p className="font-medium">Locking your seats...</p>
            <p className="text-sm text-gray-500">Please wait</p>
          </div>
        </div>
      )}
    </div>
  );
}
